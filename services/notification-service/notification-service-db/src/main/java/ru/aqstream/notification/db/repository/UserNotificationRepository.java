package ru.aqstream.notification.db.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.notification.db.entity.UserNotification;

/**
 * Репозиторий для работы с UI-уведомлениями пользователя.
 *
 * <p>Все методы используют tenant_id для RLS изоляции (Defense in Depth).</p>
 */
@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    /**
     * Находит уведомления пользователя с учётом tenant isolation.
     *
     * @param tenantId ID организации
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return страница уведомлений
     */
    @Query("SELECT n FROM UserNotification n " +
           "WHERE n.tenantId = :tenantId AND n.userId = :userId " +
           "ORDER BY n.createdAt DESC")
    Page<UserNotification> findByTenantIdAndUserIdOrderByCreatedAtDesc(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId,
        Pageable pageable
    );

    /**
     * Находит уведомление по ID с проверкой tenant и user.
     * Defense in Depth — двойная проверка помимо RLS.
     *
     * @param id       ID уведомления
     * @param tenantId ID организации
     * @param userId   ID пользователя
     * @return уведомление если найдено и принадлежит пользователю
     */
    @Query("SELECT n FROM UserNotification n " +
           "WHERE n.id = :id AND n.tenantId = :tenantId AND n.userId = :userId")
    Optional<UserNotification> findByIdAndTenantIdAndUserId(
        @Param("id") UUID id,
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId
    );

    /**
     * Подсчитывает количество непрочитанных уведомлений.
     *
     * @param tenantId ID организации
     * @param userId   ID пользователя
     * @return количество непрочитанных
     */
    @Query("SELECT COUNT(n) FROM UserNotification n " +
           "WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.isRead = false")
    long countUnreadByTenantIdAndUserId(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId
    );

    /**
     * Отмечает все уведомления пользователя как прочитанные.
     *
     * @param tenantId ID организации
     * @param userId   ID пользователя
     * @return количество обновлённых записей
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserNotification n SET n.isRead = true " +
           "WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.isRead = false")
    int markAllAsReadByTenantIdAndUserId(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId
    );
}
