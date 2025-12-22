package ru.aqstream.notification.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.notification.api.dto.NotificationStatus;
import ru.aqstream.notification.db.entity.NotificationLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с логами уведомлений.
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    /**
     * Находит уведомления пользователя.
     *
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return страница логов
     */
    Page<NotificationLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * Находит уведомления по статусу.
     *
     * @param status   статус
     * @param pageable параметры пагинации
     * @return страница логов
     */
    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    /**
     * Находит уведомления, готовые к повторной отправке.
     * Статус FAILED и количество попыток меньше максимального.
     *
     * @param maxRetries максимальное количество попыток
     * @return список уведомлений для retry
     */
    @Query("SELECT l FROM NotificationLog l WHERE l.status = 'FAILED' AND l.retryCount < :maxRetries")
    List<NotificationLog> findForRetry(@Param("maxRetries") int maxRetries);

    /**
     * Подсчитывает количество уведомлений по статусу.
     *
     * @param status статус
     * @return количество
     */
    long countByStatus(NotificationStatus status);

    /**
     * Подсчитывает количество уведомлений пользователя за период.
     *
     * @param userId       ID пользователя
     * @param templateCode код шаблона
     * @param after        начало периода
     * @return количество отправленных уведомлений
     */
    @Query("SELECT COUNT(l) FROM NotificationLog l WHERE l.userId = :userId "
        + "AND l.templateCode = :templateCode AND l.createdAt > :after")
    long countByUserIdAndTemplateCodeSince(
        @Param("userId") UUID userId,
        @Param("templateCode") String templateCode,
        @Param("after") Instant after
    );

    /**
     * Удаляет старые записи логов.
     * Используется для очистки данных старше указанного времени.
     *
     * @param before время, до которого удалять записи
     * @return количество удалённых записей
     */
    @Modifying
    @Query("DELETE FROM NotificationLog l WHERE l.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") Instant before);

    /**
     * Находит последнее уведомление пользователю по шаблону.
     *
     * @param userId       ID пользователя
     * @param templateCode код шаблона
     * @return последнее уведомление или null
     */
    @Query("SELECT l FROM NotificationLog l WHERE l.userId = :userId "
        + "AND l.templateCode = :templateCode ORDER BY l.createdAt DESC LIMIT 1")
    NotificationLog findLastByUserIdAndTemplateCode(
        @Param("userId") UUID userId,
        @Param("templateCode") String templateCode
    );
}
