package ru.aqstream.event.db.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aqstream.event.db.entity.EventAuditLog;

/**
 * Репозиторий для истории изменений событий.
 */
@Repository
public interface EventAuditLogRepository extends JpaRepository<EventAuditLog, UUID> {

    /**
     * Находит историю изменений события по ID события.
     *
     * @param eventId  идентификатор события
     * @param tenantId идентификатор организации
     * @param pageable параметры пагинации
     * @return страница записей аудита
     */
    Page<EventAuditLog> findByEventIdAndTenantIdOrderByCreatedAtDesc(
        UUID eventId, UUID tenantId, Pageable pageable);
}
