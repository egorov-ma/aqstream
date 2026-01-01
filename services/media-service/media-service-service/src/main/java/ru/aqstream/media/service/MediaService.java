package ru.aqstream.media.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.media.api.dto.MediaPurpose;
import ru.aqstream.media.api.dto.UploadResponse;
import ru.aqstream.media.db.entity.MediaFile;
import ru.aqstream.media.db.repository.MediaFileRepository;
import ru.aqstream.media.storage.MinioStorageService;

/**
 * Сервис для работы с медиа-файлами.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif"
    );

    private final MediaFileRepository mediaFileRepository;
    private final MinioStorageService storageService;

    /**
     * Загружает файл.
     *
     * @param file    загружаемый файл
     * @param purpose назначение файла
     * @param userId  идентификатор пользователя
     * @return информация о загруженном файле
     */
    @Transactional
    public UploadResponse upload(MultipartFile file, MediaPurpose purpose, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();

        log.info("Загрузка файла: userId={}, purpose={}, fileName={}, size={}",
            userId, purpose, file.getOriginalFilename(), file.getSize());

        // Валидация файла
        validateFile(file, purpose);

        // Загрузка в MinIO
        String fileKey;
        try (InputStream inputStream = file.getInputStream()) {
            fileKey = storageService.upload(
                inputStream,
                file.getSize(),
                file.getContentType(),
                purpose,
                tenantId
            );
        } catch (IOException e) {
            log.error("Ошибка чтения файла: {}", e.getMessage(), e);
            throw new FileReadException("Ошибка чтения загружаемого файла");
        }

        // Сохранение метаданных в БД
        MediaFile mediaFile = MediaFile.create(
            userId,
            fileKey,
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            storageService.getBucketName(),
            purpose
        );

        // Для аватара привязываем к userId
        if (purpose == MediaPurpose.USER_AVATAR) {
            mediaFile.attachToEntity(userId);
        }

        mediaFile = mediaFileRepository.save(mediaFile);

        String url = storageService.getPresignedUrl(fileKey);

        log.info("Файл загружен: fileId={}, fileKey={}", mediaFile.getId(), fileKey);

        return new UploadResponse(
            mediaFile.getId(),
            url,
            mediaFile.getOriginalName(),
            mediaFile.getContentType(),
            mediaFile.getFileSize()
        );
    }

    /**
     * Получает URL файла по ID.
     *
     * @param fileId идентификатор файла
     * @return URL файла
     */
    @Transactional(readOnly = true)
    public String getFileUrl(UUID fileId) {
        UUID tenantId = TenantContext.getTenantId();

        MediaFile file = mediaFileRepository.findByIdAndTenantId(fileId, tenantId)
            .orElseThrow(() -> new MediaFileNotFoundException(fileId));

        return storageService.getPresignedUrl(file.getFileKey());
    }

    /**
     * Удаляет файл.
     *
     * @param fileId идентификатор файла
     * @param userId идентификатор пользователя (для проверки прав)
     */
    @Transactional
    public void delete(UUID fileId, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();

        MediaFile file = mediaFileRepository.findByIdAndTenantId(fileId, tenantId)
            .orElseThrow(() -> new MediaFileNotFoundException(fileId));

        // Проверяем, что файл принадлежит пользователю
        if (!file.getUserId().equals(userId)) {
            throw new MediaFileAccessDeniedException(fileId, userId);
        }

        log.info("Удаление файла: fileId={}, userId={}", fileId, userId);

        // Удаляем из MinIO
        storageService.delete(file.getFileKey());

        // Soft delete в БД
        file.softDelete();
        mediaFileRepository.save(file);

        log.info("Файл удалён: fileId={}", fileId);
    }

    private void validateFile(MultipartFile file, MediaPurpose purpose) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Файл пуст");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("Размер файла превышает максимально допустимый (10MB)");
        }

        String contentType = file.getContentType();

        // Для изображений проверяем MIME-тип
        if (purpose == MediaPurpose.USER_AVATAR
            || purpose == MediaPurpose.EVENT_COVER
            || purpose == MediaPurpose.ORGANIZATION_LOGO) {

            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new InvalidFileException("Недопустимый тип файла. Разрешены: JPEG, PNG, WebP, GIF");
            }
        }
    }

    /**
     * Исключение: файл не найден.
     */
    public static class MediaFileNotFoundException extends RuntimeException {
        public MediaFileNotFoundException(UUID fileId) {
            super("Файл не найден: " + fileId);
        }
    }

    /**
     * Исключение: нет доступа к файлу.
     */
    public static class MediaFileAccessDeniedException extends RuntimeException {
        public MediaFileAccessDeniedException(UUID fileId, UUID userId) {
            super("Нет доступа к файлу: fileId=" + fileId + ", userId=" + userId);
        }
    }

    /**
     * Исключение: невалидный файл.
     */
    public static class InvalidFileException extends RuntimeException {
        public InvalidFileException(String message) {
            super(message);
        }
    }

    /**
     * Исключение: ошибка чтения файла.
     */
    public static class FileReadException extends RuntimeException {
        public FileReadException(String message) {
            super(message);
        }
    }
}
