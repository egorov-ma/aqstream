package ru.aqstream.media.db.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.media.api.dto.MediaPurpose;
import ru.aqstream.media.db.entity.MediaFile;

/**
 * Репозиторий для работы с медиа-файлами.
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {

    /**
     * Находит файл по ID и tenant.
     *
     * @param id       идентификатор файла
     * @param tenantId идентификатор организации
     * @return файл или empty
     */
    @Query("SELECT f FROM MediaFile f WHERE f.id = :id AND f.tenantId = :tenantId")
    Optional<MediaFile> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Находит файлы по назначению и связанной сущности.
     *
     * @param purpose  назначение
     * @param entityId идентификатор сущности
     * @param tenantId идентификатор организации
     * @return список файлов
     */
    @Query("SELECT f FROM MediaFile f WHERE f.purpose = :purpose AND f.entityId = :entityId AND f.tenantId = :tenantId")
    List<MediaFile> findByPurposeAndEntityId(
        @Param("purpose") MediaPurpose purpose,
        @Param("entityId") UUID entityId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Находит аватар пользователя.
     *
     * @param userId   идентификатор пользователя
     * @param tenantId идентификатор организации
     * @return аватар или empty
     */
    @Query("SELECT f FROM MediaFile f WHERE f.purpose = 'USER_AVATAR' AND f.entityId = :userId AND f.tenantId = :tenantId")
    Optional<MediaFile> findUserAvatar(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    /**
     * Проверяет существование файла по ключу.
     *
     * @param fileKey ключ файла
     * @return true если файл существует
     */
    boolean existsByFileKey(String fileKey);
}
