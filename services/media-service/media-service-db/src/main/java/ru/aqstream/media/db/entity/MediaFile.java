package ru.aqstream.media.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import ru.aqstream.common.data.SoftDeletableEntity;
import ru.aqstream.media.api.dto.MediaPurpose;

/**
 * Сущность для хранения метаданных загруженных файлов.
 */
@Entity
@Table(name = "media_files", schema = "media_service")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaFile extends SoftDeletableEntity {

    /**
     * Идентификатор пользователя, загрузившего файл.
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /**
     * Ключ файла в хранилище (path в MinIO).
     */
    @Column(name = "file_key", nullable = false, unique = true, length = 500)
    private String fileKey;

    /**
     * Оригинальное имя файла.
     */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /**
     * MIME-тип файла.
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * Размер файла в байтах.
     */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /**
     * Имя бакета в MinIO.
     */
    @Column(name = "bucket", nullable = false, length = 100)
    private String bucket;

    /**
     * Назначение файла (аватар, обложка события и т.д.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private MediaPurpose purpose;

    /**
     * Идентификатор связанной сущности (userId для аватара, eventId для обложки).
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Создаёт новый медиа-файл.
     *
     * @param userId      идентификатор пользователя
     * @param fileKey     ключ в хранилище
     * @param originalName оригинальное имя файла
     * @param contentType MIME-тип
     * @param fileSize    размер в байтах
     * @param bucket      имя бакета
     * @param purpose     назначение
     * @return новый объект MediaFile
     */
    public static MediaFile create(
        UUID userId,
        String fileKey,
        String originalName,
        String contentType,
        long fileSize,
        String bucket,
        MediaPurpose purpose
    ) {
        MediaFile file = new MediaFile();
        file.userId = userId;
        file.fileKey = fileKey;
        file.originalName = originalName;
        file.contentType = contentType;
        file.fileSize = fileSize;
        file.bucket = bucket;
        file.purpose = purpose;
        return file;
    }

    /**
     * Связывает файл с сущностью.
     *
     * @param entityId идентификатор сущности
     */
    public void attachToEntity(UUID entityId) {
        this.entityId = entityId;
    }
}
