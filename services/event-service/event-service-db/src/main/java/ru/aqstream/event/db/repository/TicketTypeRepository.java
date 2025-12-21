package ru.aqstream.event.db.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aqstream.event.db.entity.TicketType;

/**
 * Репозиторий для работы с типами билетов.
 *
 * <p>Типы билетов не имеют собственного tenant_id, так как принадлежат Event,
 * который уже является tenant-aware.</p>
 */
@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    // === Поиск по ID ===

    /**
     * Находит тип билета по ID и ID события.
     *
     * @param id      идентификатор типа билета
     * @param eventId идентификатор события
     * @return тип билета или empty
     */
    Optional<TicketType> findByIdAndEventId(UUID id, UUID eventId);

    /**
     * Находит тип билета с блокировкой для обновления (предотвращение overselling).
     * Используется при регистрации для атомарного обновления счётчиков.
     *
     * @param id      идентификатор типа билета
     * @param eventId идентификатор события
     * @return тип билета или empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.id = :id AND t.event.id = :eventId")
    Optional<TicketType> findByIdAndEventIdForUpdate(
        @Param("id") UUID id,
        @Param("eventId") UUID eventId
    );

    // === Списки типов билетов ===

    /**
     * Возвращает все типы билетов события, отсортированные по sortOrder.
     *
     * @param eventId идентификатор события
     * @return список типов билетов
     */
    List<TicketType> findByEventIdOrderBySortOrderAsc(UUID eventId);

    /**
     * Возвращает активные типы билетов события, отсортированные по sortOrder.
     * Используется для публичного отображения.
     *
     * @param eventId идентификатор события
     * @return список активных типов билетов
     */
    List<TicketType> findByEventIdAndActiveIsTrueOrderBySortOrderAsc(UUID eventId);

    // === Проверки ===

    /**
     * Проверяет существование типа билета в событии.
     *
     * @param id      идентификатор типа билета
     * @param eventId идентификатор события
     * @return true если существует
     */
    boolean existsByIdAndEventId(UUID id, UUID eventId);

    /**
     * Проверяет существование активных типов билетов у события.
     *
     * @param eventId идентификатор события
     * @return true если есть активные типы билетов
     */
    boolean existsByEventIdAndActiveIsTrue(UUID eventId);

    // === Счётчики ===

    /**
     * Подсчитывает количество типов билетов события.
     *
     * @param eventId идентификатор события
     * @return количество типов билетов
     */
    int countByEventId(UUID eventId);

    /**
     * Подсчитывает общее количество проданных билетов события.
     *
     * @param eventId идентификатор события
     * @return количество проданных билетов
     */
    @Query("SELECT COALESCE(SUM(t.soldCount), 0) FROM TicketType t WHERE t.event.id = :eventId")
    int sumSoldCountByEventId(@Param("eventId") UUID eventId);

    /**
     * Подсчитывает общее количество доступных билетов события.
     * Учитывает только типы с ограниченным количеством.
     *
     * @param eventId идентификатор события
     * @return количество доступных билетов (null если есть unlimited типы)
     */
    @Query("SELECT CASE WHEN COUNT(t) = SUM(CASE WHEN t.quantity IS NOT NULL THEN 1 ELSE 0 END) "
        + "THEN SUM(t.quantity - t.soldCount - t.reservedCount) ELSE NULL END "
        + "FROM TicketType t WHERE t.event.id = :eventId AND t.active = true")
    Integer sumAvailableByEventId(@Param("eventId") UUID eventId);

    // === Максимальный sortOrder ===

    /**
     * Возвращает максимальный sortOrder для типов билетов события.
     *
     * @param eventId идентификатор события
     * @return максимальный sortOrder или null если типов нет
     */
    @Query("SELECT MAX(t.sortOrder) FROM TicketType t WHERE t.event.id = :eventId")
    Integer findMaxSortOrderByEventId(@Param("eventId") UUID eventId);
}
