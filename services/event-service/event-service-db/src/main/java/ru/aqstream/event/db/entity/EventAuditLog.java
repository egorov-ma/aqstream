package ru.aqstream.event.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.aqstream.common.data.TenantAwareEntity;
import ru.aqstream.event.api.dto.EventAuditAction;

/**
 * История изменений события (Audit Log).
 * Записывает все изменения события для отслеживания истории.
 */
@Entity
@Table(name = "event_audit_log", schema = "event_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventAuditLog extends TenantAwareEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private EventAuditAction action;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private Map<String, FieldChange> changedFields;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Изменение поля.
     *
     * @param from старое значение
     * @param to   новое значение
     */
    public record FieldChange(
        String from,
        String to
    ) {}

    // === Фабричные методы ===

    /**
     * Создаёт запись аудита для действия над событием.
     *
     * @param eventId     идентификатор события
     * @param action      тип действия
     * @param actorId     идентификатор пользователя
     * @param actorEmail  email пользователя
     * @param description описание действия
     * @return запись аудита
     */
    public static EventAuditLog create(UUID eventId, EventAuditAction action,
                                       UUID actorId, String actorEmail, String description) {
        EventAuditLog log = new EventAuditLog();
        log.setEventId(eventId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setActorEmail(actorEmail);
        log.setDescription(description);
        return log;
    }

    /**
     * Создаёт запись аудита для обновления события с изменёнными полями.
     *
     * @param eventId       идентификатор события
     * @param actorId       идентификатор пользователя
     * @param actorEmail    email пользователя
     * @param changedFields изменённые поля
     * @return запись аудита
     */
    public static EventAuditLog createUpdate(UUID eventId, UUID actorId, String actorEmail,
                                             Map<String, FieldChange> changedFields) {
        EventAuditLog log = new EventAuditLog();
        log.setEventId(eventId);
        log.setAction(EventAuditAction.UPDATED);
        log.setActorId(actorId);
        log.setActorEmail(actorEmail);
        log.setChangedFields(changedFields);
        log.setDescription("Событие обновлено");
        return log;
    }
}
