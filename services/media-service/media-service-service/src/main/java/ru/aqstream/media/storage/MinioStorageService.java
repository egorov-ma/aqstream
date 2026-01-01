package ru.aqstream.media.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.aqstream.media.api.dto.MediaPurpose;

/**
 * Сервис для работы с MinIO хранилищем.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private static final long URL_EXPIRY_HOURS = 24;

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * Инициализирует bucket при запуске.
     */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Создан bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Ошибка инициализации MinIO bucket: {}", e.getMessage(), e);
            throw new StorageInitializationException("Ошибка инициализации хранилища", e);
        }
    }

    /**
     * Загружает файл в хранилище.
     *
     * @param inputStream поток данных
     * @param fileSize    размер файла
     * @param contentType MIME-тип
     * @param purpose     назначение файла
     * @param tenantId    идентификатор организации
     * @return ключ файла в хранилище
     */
    public String upload(
        InputStream inputStream,
        long fileSize,
        String contentType,
        MediaPurpose purpose,
        UUID tenantId
    ) {
        String fileKey = generateFileKey(purpose, tenantId);

        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .stream(inputStream, fileSize, -1)
                    .contentType(contentType)
                    .build()
            );
            log.info("Файл загружен: bucket={}, key={}", bucketName, fileKey);
            return fileKey;
        } catch (Exception e) {
            log.error("Ошибка загрузки файла: {}", e.getMessage(), e);
            throw new FileUploadException("Ошибка загрузки файла в хранилище", e);
        }
    }

    /**
     * Удаляет файл из хранилища.
     *
     * @param fileKey ключ файла
     */
    public void delete(String fileKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build()
            );
            log.info("Файл удалён: bucket={}, key={}", bucketName, fileKey);
        } catch (Exception e) {
            log.error("Ошибка удаления файла: key={}, ошибка={}", fileKey, e.getMessage(), e);
            throw new FileDeleteException("Ошибка удаления файла из хранилища", e);
        }
    }

    /**
     * Генерирует временный URL для доступа к файлу.
     *
     * @param fileKey ключ файла
     * @return presigned URL
     */
    public String getPresignedUrl(String fileKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(fileKey)
                    .expiry((int) URL_EXPIRY_HOURS, TimeUnit.HOURS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Ошибка генерации URL: key={}, ошибка={}", fileKey, e.getMessage(), e);
            throw new UrlGenerationException("Ошибка генерации URL для файла", e);
        }
    }

    /**
     * Генерирует публичный URL (если bucket публичный).
     *
     * @param fileKey ключ файла
     * @return публичный URL
     */
    public String getPublicUrl(String fileKey) {
        return String.format("%s/%s/%s", endpoint, bucketName, fileKey);
    }

    /**
     * Возвращает имя bucket.
     *
     * @return имя bucket
     */
    public String getBucketName() {
        return bucketName;
    }

    private String generateFileKey(MediaPurpose purpose, UUID tenantId) {
        String prefix = switch (purpose) {
            case USER_AVATAR -> "avatars";
            case EVENT_COVER -> "events";
            case ORGANIZATION_LOGO -> "organizations";
            case GENERAL -> "general";
        };
        return String.format("%s/%s/%s", tenantId, prefix, UUID.randomUUID());
    }

    /**
     * Исключение при ошибке инициализации хранилища.
     */
    public static class StorageInitializationException extends RuntimeException {
        public StorageInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Исключение при ошибке загрузки файла.
     */
    public static class FileUploadException extends RuntimeException {
        public FileUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Исключение при ошибке удаления файла.
     */
    public static class FileDeleteException extends RuntimeException {
        public FileDeleteException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Исключение при ошибке генерации URL.
     */
    public static class UrlGenerationException extends RuntimeException {
        public UrlGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
