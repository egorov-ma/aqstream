package ru.aqstream.event.db.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.db.entity.Registration;

/**
 * Репозиторий для работы с регистрациями.
 * RLS применяется автоматически через tenant_id в TenantAwareEntity.
 */
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    // === Поиск по ID ===

    /**
     * Находит регистрацию по ID в рамках tenant.
     * Defense in depth: дополнительная проверка tenant_id на уровне приложения.
     *
     * @param id       идентификатор регистрации
     * @param tenantId идентификатор организации
     * @return регистрация или empty
     */
    @EntityGraph(attributePaths = {"event", "ticketType"})
    Optional<Registration> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Находит регистрацию по confirmation code.
     *
     * @param confirmationCode код подтверждения
     * @return регистрация или empty
     */
    @EntityGraph(attributePaths = {"event", "ticketType"})
    Optional<Registration> findByConfirmationCode(String confirmationCode);

    /**
     * Проверяет существование confirmation code.
     *
     * @param confirmationCode код подтверждения
     * @return true если код уже используется
     */
    boolean existsByConfirmationCode(String confirmationCode);

    // === Проверки уникальности ===

    /**
     * Проверяет, есть ли у пользователя активная регистрация на событие.
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @return true если активная регистрация существует
     */
    @Query("SELECT COUNT(r) > 0 FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.userId = :userId "
        + "AND r.status != 'CANCELLED'")
    boolean existsActiveByEventIdAndUserId(
        @Param("eventId") UUID eventId,
        @Param("userId") UUID userId
    );

    /**
     * Находит активную регистрацию пользователя на событие.
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @return регистрация или empty
     */
    @Query("SELECT r FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.userId = :userId "
        + "AND r.status != 'CANCELLED'")
    Optional<Registration> findActiveByEventIdAndUserId(
        @Param("eventId") UUID eventId,
        @Param("userId") UUID userId
    );

    // === Списки регистраций пользователя ("мои регистрации") ===

    /**
     * Возвращает страницу регистраций пользователя.
     *
     * @param userId   идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница регистраций
     */
    @EntityGraph(attributePaths = {"event", "ticketType"})
    @Query("SELECT r FROM Registration r WHERE r.userId = :userId")
    Page<Registration> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Возвращает активные регистрации пользователя.
     *
     * @param userId   идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница регистраций
     */
    @EntityGraph(attributePaths = {"event", "ticketType"})
    @Query("SELECT r FROM Registration r "
        + "WHERE r.userId = :userId "
        + "AND r.status != 'CANCELLED' "
        + "ORDER BY r.event.startsAt ASC")
    Page<Registration> findActiveByUserId(@Param("userId") UUID userId, Pageable pageable);

    // === Списки регистраций события (для организатора) ===

    /**
     * Возвращает страницу регистраций события.
     *
     * @param eventId  идентификатор события
     * @param tenantId идентификатор организации
     * @param pageable параметры пагинации
     * @return страница регистраций
     */
    @EntityGraph(attributePaths = {"ticketType"})
    @Query("SELECT r FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.tenantId = :tenantId")
    Page<Registration> findByEventIdAndTenantId(
        @Param("eventId") UUID eventId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * Возвращает регистрации события с фильтром по статусу.
     *
     * @param eventId  идентификатор события
     * @param tenantId идентификатор организации
     * @param status   статус регистрации
     * @param pageable параметры пагинации
     * @return страница регистраций
     */
    @EntityGraph(attributePaths = {"ticketType"})
    @Query("SELECT r FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.tenantId = :tenantId "
        + "AND r.status = :status")
    Page<Registration> findByEventIdAndTenantIdAndStatus(
        @Param("eventId") UUID eventId,
        @Param("tenantId") UUID tenantId,
        @Param("status") RegistrationStatus status,
        Pageable pageable
    );

    /**
     * Возвращает регистрации события с фильтром по типу билета.
     *
     * @param eventId      идентификатор события
     * @param tenantId     идентификатор организации
     * @param ticketTypeId идентификатор типа билета
     * @param pageable     параметры пагинации
     * @return страница регистраций
     */
    @EntityGraph(attributePaths = {"ticketType"})
    @Query("SELECT r FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.tenantId = :tenantId "
        + "AND r.ticketType.id = :ticketTypeId")
    Page<Registration> findByEventIdAndTenantIdAndTicketTypeId(
        @Param("eventId") UUID eventId,
        @Param("tenantId") UUID tenantId,
        @Param("ticketTypeId") UUID ticketTypeId,
        Pageable pageable
    );

    /**
     * Поиск регистраций по имени или email.
     *
     * @param eventId  идентификатор события
     * @param tenantId идентификатор организации
     * @param query    строка поиска (имя, фамилия или email)
     * @param pageable параметры пагинации
     * @return страница регистраций
     */
    @EntityGraph(attributePaths = {"ticketType"})
    @Query("SELECT r FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.tenantId = :tenantId "
        + "AND (LOWER(r.firstName) LIKE LOWER(CONCAT('%', :query, '%')) "
        + "OR LOWER(r.lastName) LIKE LOWER(CONCAT('%', :query, '%')) "
        + "OR LOWER(r.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Registration> searchByEventIdAndTenantId(
        @Param("eventId") UUID eventId,
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        Pageable pageable
    );

    // === Подсчёты ===

    /**
     * Подсчитывает количество регистраций на событие.
     *
     * @param eventId идентификатор события
     * @return количество регистраций
     */
    long countByEventId(UUID eventId);

    /**
     * Подсчитывает количество подтверждённых регистраций на событие.
     *
     * @param eventId идентификатор события
     * @return количество подтверждённых регистраций
     */
    @Query("SELECT COUNT(r) FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.status = 'CONFIRMED'")
    long countConfirmedByEventId(@Param("eventId") UUID eventId);

    /**
     * Подсчитывает количество регистраций по типу билета.
     *
     * @param ticketTypeId идентификатор типа билета
     * @return количество регистраций
     */
    long countByTicketTypeId(UUID ticketTypeId);

    /**
     * Подсчитывает количество подтверждённых регистраций по типу билета.
     *
     * @param ticketTypeId идентификатор типа билета
     * @return количество подтверждённых регистраций
     */
    @Query("SELECT COUNT(r) FROM Registration r "
        + "WHERE r.ticketType.id = :ticketTypeId "
        + "AND r.status = 'CONFIRMED'")
    long countConfirmedByTicketTypeId(@Param("ticketTypeId") UUID ticketTypeId);

    // === Dashboard статистика ===

    /**
     * Подсчитывает количество регистраций организации за период.
     *
     * @param tenantId идентификатор организации
     * @param from     начало периода
     * @return количество регистраций
     */
    @Query("SELECT COUNT(r) FROM Registration r "
        + "WHERE r.tenantId = :tenantId "
        + "AND r.createdAt >= :from")
    long countByTenantIdAndCreatedAtAfter(
        @Param("tenantId") UUID tenantId,
        @Param("from") java.time.Instant from
    );

    /**
     * Подсчитывает количество check-in организации за период.
     *
     * @param tenantId идентификатор организации
     * @param from     начало периода
     * @return количество check-in
     */
    @Query("SELECT COUNT(r) FROM Registration r "
        + "WHERE r.tenantId = :tenantId "
        + "AND r.checkedInAt IS NOT NULL "
        + "AND r.checkedInAt >= :from")
    long countCheckedInByTenantIdAfter(
        @Param("tenantId") UUID tenantId,
        @Param("from") java.time.Instant from
    );

    // === Для массовых операций ===

    /**
     * Находит все активные регистрации события (для отмены при отмене события).
     *
     * @param eventId идентификатор события
     * @return список регистраций
     */
    @Query("SELECT r FROM Registration r "
        + "WHERE r.event.id = :eventId "
        + "AND r.status != 'CANCELLED'")
    List<Registration> findActiveByEventId(@Param("eventId") UUID eventId);
}
