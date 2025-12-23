package ru.aqstream.event.db.repository;

import java.time.Instant;
import java.util.Optional;
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
     * Находит публичное опубликованное событие по slug.
     * Используется для публичной страницы события.
     *
     * @param slug URL-slug события
     * @return событие или empty
     */
    @Query("SELECT e FROM Event e WHERE e.slug = :slug "
        + "AND e.isPublic = true "
        + "AND e.status IN ('PUBLISHED', 'COMPLETED')")
    Optional<Event> findPublicBySlug(@Param("slug") String slug);

    /**
     * Возвращает публичные опубликованные события.
     *
     * @param pageable параметры пагинации
     * @return страница событий
     */
    @Query("SELECT e FROM Event e WHERE e.isPublic = true "
        + "AND e.status = 'PUBLISHED' "
        + "ORDER BY e.startsAt ASC")
    Page<Event> findPublicPublished(Pageable pageable);

    /**
     * Возвращает предстоящие публичные события.
     *
     * @param now      текущее время
     * @param pageable параметры пагинации
     * @return страница событий
     */
    @Query("SELECT e FROM Event e WHERE e.isPublic = true "
        + "AND e.status = 'PUBLISHED' "
        + "AND e.startsAt > :now "
        + "ORDER BY e.startsAt ASC")
    Page<Event> findUpcomingPublicEvents(@Param("now") Instant now, Pageable pageable);

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
     * Находит все активные события организации (не отменённые и не завершённые).
     * Используется при удалении организации для архивирования событий.
     *
     * @param tenantId идентификатор организации
     * @return список событий
     */
    @Query("SELECT e FROM Event e WHERE e.tenantId = :tenantId "
        + "AND e.status NOT IN ('CANCELLED', 'COMPLETED')")
    java.util.List<Event> findActiveByTenantId(@Param("tenantId") UUID tenantId);

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
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' "
        + "AND e.startsAt >= :from AND e.startsAt < :to "
        + "ORDER BY e.startsAt ASC")
    java.util.List<Event> findPublishedByStartsAtBetween(
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
