package ru.aqstream.event.api.event;

import java.time.Instant;
import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие завершения мероприятия.
 * Публикуется когда мероприятие переходит из PUBLISHED в COMPLETED.
 */
public class EventCompletedEvent extends DomainEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final String title;
    private final Instant completedAt;

    /**
     * Создаёт событие завершения мероприятия.
     *
     * @param eventId     идентификатор мероприятия
     * @param tenantId    идентификатор организации
     * @param title       название мероприятия
     * @param completedAt дата завершения
     */
    public EventCompletedEvent(UUID eventId, UUID tenantId, String title, Instant completedAt) {
        super();
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.title = title;
        this.completedAt = completedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public String getEventType() {
        return "event.completed";
    }

    @Override
    public UUID getAggregateId() {
        return eventId;
    }
}
