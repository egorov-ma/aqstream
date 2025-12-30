package ru.aqstream.event.db.repository;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.db.entity.Event;

/**
 * Репозиторий для работы с событиями.
 * RLS применяется автоматически через tenant_id в TenantAwareEntity.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    // === Поиск по ID и slug ===

    /**
     * Находит событие по slug в рамках tenant.
     *
     * @param slug     URL-slug события
     * @param tenantId идентификатор организации
     * @return событие или empty
     */
    Optional<Event> findBySlugAndTenantId(String slug, UUID tenantId);

    /**
     * Проверяет существование slug в рамках tenant.
     *
     * @param slug     URL-slug события
     * @param tenantId идентификатор организации
     * @return true если slug занят
     */
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);

    /**
     * Находит событие по ID в рамках tenant.
     * Defense in depth: дополнительная проверка tenant_id на уровне приложения.
     *
     * @param id       идентификатор события
     * @param tenantId идентификатор организации
     * @return событие или empty
     */
    Optional<Event> findByIdAndTenantId(UUID id, UUID tenantId);

    // === Списки событий ===

    /**
     * Возвращает страницу событий организации.
     *
     * @param tenantId идентификатор организации
     * @param pageable параметры пагинации
     * @return страница событий
     */
    Page<Event> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Возвращает страницу событий организации с фильтром по статусу.
     *
     * @param tenantId идентификатор организации
     * @param status   статус события
     * @param pageable параметры пагинации
     * @return страница событий
     */
    Page<Event> findByTenantIdAndStatus(UUID tenantId, EventStatus status, Pageable pageable);

    /**
     * Возвращает события организации в диапазоне дат.
     *
     * @param tenantId идентификатор организации
     * @param from     начало периода
     * @param to       конец периода
     * @param pageable параметры пагинации
     * @return страница событий
     */
    @Query("SELECT e FROM Event e WHERE e.tenantId = :tenantId "
        + "AND e.startsAt >= :from AND e.startsAt <= :to "
        + "ORDER BY e.startsAt ASC")
    Page<Event> findByTenantIdAndDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );

    /**
     * Возвращает события группы.
     *
     * @param tenantId идентификатор организации
     * @param groupId  идентификатор группы
     * @param pageable параметры пагинации
     * @return страница событий
     */
    Page<Event> findByTenantIdAndGroupId(UUID tenantId, UUID groupId, Pageable pageable);

    // === Публичные события ===

    /**
     * Статусы для публичных событий.
     */
    Set<EventStatus> PUBLIC_VISIBLE_STATUSES = EnumSet.of(EventStatus.PUBLISHED, EventStatus.COMPLETED);

    /**
     * Находит публичное опубликованное событие по slug.
     * Используется для публичной страницы события.
     *
     * @param slug URL-slug события
     * @return событие или empty
     */
    default Optional<Event> findPublicBySlug(String slug) {
        return findPublicBySlugAndStatuses(slug, PUBLIC_VISIBLE_STATUSES);
    }

    /**
     * Находит публичное событие по slug с указанными статусами.
     */
    @Query("SELECT e FROM Event e WHERE e.slug = :slug "
        + "AND e.isPublic = true "
        + "AND e.status IN :statuses")
    Optional<Event> findPublicBySlugAndStatuses(
        @Param("slug") String slug,
        @Param("statuses") Set<EventStatus> statuses
    );

    /**
     * Возвращает публичные опубликованные события.
     *
     * @param pageable параметры пагинации
     * @return страница событий
     */
    default Page<Event> findPublicPublished(Pageable pageable) {
        return findPublicByStatus(EventStatus.PUBLISHED, pageable);
    }

    /**
     * Возвращает публичные события с указанным статусом.
     */
    @Query("SELECT e FROM Event e WHERE e.isPublic = true "
        + "AND e.status = :status "
        + "ORDER BY e.startsAt ASC")
    Page<Event> findPublicByStatus(@Param("status") EventStatus status, Pageable pageable);

    /**
     * Возвращает предстоящие публичные события.
     *
     * @param now      текущее время
     * @param pageable параметры пагинации
     * @return страница событий
     */
    default Page<Event> findUpcomingPublicEvents(Instant now, Pageable pageable) {
        return findUpcomingPublicEventsByStatus(now, EventStatus.PUBLISHED, pageable);
    }

    /**
     * Возвращает предстоящие публичные события с указанным статусом.
     */
    @Query("SELECT e FROM Event e WHERE e.isPublic = true "
        + "AND e.status = :status "
        + "AND e.startsAt > :now "
        + "ORDER BY e.startsAt ASC")
    Page<Event> findUpcomingPublicEventsByStatus(
        @Param("now") Instant now,
        @Param("status") EventStatus status,
        Pageable pageable
    );

    // === Счётчики ===

    /**
     * Подсчитывает события организации по статусу.
     *
     * @param tenantId идентификатор организации
     * @param status   статус события
     * @return количество событий
     */
    long countByTenantIdAndStatus(UUID tenantId, EventStatus status);

    // === Массовые операции ===

    /**
     * Статусы неактивных (завершённых) событий.
     */
    Set<EventStatus> INACTIVE_STATUSES = EnumSet.of(EventStatus.CANCELLED, EventStatus.COMPLETED);

    /**
     * Находит все активные события организации (не отменённые и не завершённые).
     * Используется при удалении организации для архивирования событий.
     *
     * @param tenantId идентификатор организации
     * @return список событий
     */
    default java.util.List<Event> findActiveByTenantId(UUID tenantId) {
        return findByTenantIdAndStatusNotIn(tenantId, INACTIVE_STATUSES);
    }

    /**
     * Находит события организации, исключая указанные статусы.
     */
    @Query("SELECT e FROM Event e WHERE e.tenantId = :tenantId "
        + "AND e.status NOT IN :excludedStatuses")
    java.util.List<Event> findByTenantIdAndStatusNotIn(
        @Param("tenantId") UUID tenantId,
        @Param("excludedStatuses") Set<EventStatus> excludedStatuses
    );

    // === Dashboard статистика ===

    /**
     * Возвращает ближайшие события организации (для dashboard).
     *
     * @param tenantId идентификатор организации
     * @param now      текущее время
     * @param pageable параметры пагинации (limit)
     * @return список ближайших событий
     */
    default java.util.List<Event> findUpcomingByTenantId(UUID tenantId, Instant now, Pageable pageable) {
        return findUpcomingByTenantIdAndStatus(tenantId, now, EventStatus.PUBLISHED, pageable);
    }

    /**
     * Возвращает ближайшие события организации с указанным статусом.
     */
    @Query("SELECT e FROM Event e WHERE e.tenantId = :tenantId "
        + "AND e.status = :status "
        + "AND e.startsAt > :now "
        + "ORDER BY e.startsAt ASC")
    java.util.List<Event> findUpcomingByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("now") Instant now,
        @Param("status") EventStatus status,
        Pageable pageable
    );

    // === Internal API (без RLS) ===

    /**
     * Находит все опубликованные события в указанном диапазоне времени.
     * Используется для планировщика напоминаний (без ограничений по tenant).
     *
     * <p>ВАЖНО: Этот метод НЕ применяет RLS, так как используется
     * внутренним API для отправки напоминаний всем пользователям.</p>
     *
     * @param from начало диапазона (включительно)
     * @param to   конец диапазона (не включительно)
     * @return список опубликованных событий
     */
    default java.util.List<Event> findPublishedByStartsAtBetween(Instant from, Instant to) {
        return findByStatusAndStartsAtBetween(EventStatus.PUBLISHED, from, to);
    }

    /**
     * Находит события с указанным статусом в диапазоне времени.
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status "
        + "AND e.startsAt >= :from AND e.startsAt < :to "
        + "ORDER BY e.startsAt ASC")
    java.util.List<Event> findByStatusAndStartsAtBetween(
        @Param("status") EventStatus status,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
