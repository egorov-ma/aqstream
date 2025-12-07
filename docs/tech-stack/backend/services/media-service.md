# Media Service

Media Service отвечает за загрузку и обработку файлов.

## Обзор

| Параметр | Значение |
|----------|----------|
| Порт | 8085 |
| База данных | postgres-shared |
| Схема | media_service |
| Хранилище | MinIO (S3-compatible) |

## Ответственности

- Загрузка файлов
- Валидация типов и размеров
- Обработка изображений (resize)
- Генерация signed URLs
- Cleanup неиспользуемых файлов

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/media/upload` | Загрузка файла |
| GET | `/api/v1/media/{id}` | Метаданные файла |
| GET | `/api/v1/media/{id}/url` | Signed URL |
| DELETE | `/api/v1/media/{id}` | Удаление файла |

## Ограничения

| Категория | MIME Types | Max Size |
|-----------|------------|----------|
| Изображения | image/jpeg, image/png, image/webp, image/gif | 5 MB |
| Документы | application/pdf | 10 MB |

## Загрузка файлов

```java
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<MediaDto> upload(
        @RequestParam("file") MultipartFile file
    ) {
        MediaDto result = mediaService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    private final MinioClient minioClient;
    private final MediaRepository mediaRepository;
    private final ImageProcessor imageProcessor;

    @Transactional
    public MediaDto upload(MultipartFile file) {
        // Валидация
        validateFile(file);
        
        // Генерация пути
        String storagePath = generateStoragePath(file.getOriginalFilename());
        
        // Загрузка в MinIO
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(bucketName)
            .object(storagePath)
            .stream(file.getInputStream(), file.getSize(), -1)
            .contentType(file.getContentType())
            .build());
        
        // Сохранение метаданных
        Media media = new Media();
        media.setTenantId(TenantContext.getTenantId());
        media.setUploadedBy(SecurityContext.getUserId());
        media.setFilename(file.getOriginalFilename());
        media.setContentType(file.getContentType());
        media.setSizeBytes((int) file.getSize());
        media.setStoragePath(storagePath);
        media.setStatus(MediaStatus.READY);
        
        Media saved = mediaRepository.save(media);
        
        // Создание вариантов для изображений
        if (isImage(file.getContentType())) {
            createImageVariants(saved, file);
        }
        
        return mediaMapper.toDto(saved);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("Файл пустой");
        }
        
        String contentType = file.getContentType();
        
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new ValidationException("Изображение слишком большое. Максимум 5MB");
            }
        } else if ("application/pdf".equals(contentType)) {
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new ValidationException("PDF слишком большой. Максимум 10MB");
            }
        } else {
            throw new ValidationException("Неподдерживаемый тип файла");
        }
    }
}
```

## Варианты изображений

```java
public enum ImageVariant {
    THUMBNAIL(150, 150),
    SMALL(300, 300),
    MEDIUM(600, 600),
    LARGE(1200, 1200);
    
    private final int width;
    private final int height;
}
```

```java
private void createImageVariants(Media media, MultipartFile file) {
    for (ImageVariant variant : ImageVariant.values()) {
        byte[] resized = imageProcessor.resize(
            file.getBytes(),
            variant.getWidth(),
            variant.getHeight()
        );
        
        String variantPath = generateVariantPath(media.getStoragePath(), variant);
        
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(bucketName)
            .object(variantPath)
            .stream(new ByteArrayInputStream(resized), resized.length, -1)
            .contentType(media.getContentType())
            .build());
        
        MediaVariant mediaVariant = new MediaVariant();
        mediaVariant.setMedia(media);
        mediaVariant.setVariantType(variant);
        mediaVariant.setWidth(variant.getWidth());
        mediaVariant.setHeight(variant.getHeight());
        mediaVariant.setStoragePath(variantPath);
        
        mediaVariantRepository.save(mediaVariant);
    }
}
```

## Signed URLs

```java
public String getSignedUrl(UUID mediaId, ImageVariant variant) {
    Media media = findByIdOrThrow(mediaId);
    
    String path = variant != null 
        ? getVariantPath(media, variant)
        : media.getStoragePath();
    
    return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
        .bucket(bucketName)
        .object(path)
        .method(Method.GET)
        .expiry(1, TimeUnit.HOURS)
        .build());
}
```

## Cleanup Job

```java
@Component
@RequiredArgsConstructor
public class MediaCleanupJob {

    private final MediaRepository mediaRepository;
    private final MinioClient minioClient;

    @Scheduled(cron = "0 0 3 * * *") // Каждый день в 3:00
    @Transactional
    public void cleanupUnusedMedia() {
        // Находим файлы старше 24 часов, которые ни к чему не привязаны
        List<Media> unused = mediaRepository.findUnusedOlderThan(
            Instant.now().minus(24, ChronoUnit.HOURS)
        );
        
        for (Media media : unused) {
            // Удаляем из MinIO
            minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(media.getStoragePath())
                .build());
            
            // Удаляем варианты
            for (MediaVariant variant : media.getVariants()) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(variant.getStoragePath())
                    .build());
            }
            
            media.setStatus(MediaStatus.DELETED);
            mediaRepository.save(media);
        }
        
        log.info("Очищено {} неиспользуемых файлов", unused.size());
    }
}
```

## Конфигурация

```yaml
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket-name: ${MINIO_BUCKET:aqstream-media}
```

## Дальнейшее чтение

- [Service Topology](../../../architecture/service-topology.md)
