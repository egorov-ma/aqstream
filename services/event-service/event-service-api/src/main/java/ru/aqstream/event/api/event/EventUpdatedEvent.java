package ru.aqstream.event.api.event;

import java.time.Instant;
import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие обновления мероприятия.
 * Публикуется при изменении данных события (title, dates, location и т.д.).
 */
public class EventUpdatedEvent extends DomainEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final String title;
    private final Instant startsAt;
    private final Instant updatedAt;

    /**
     * Создаёт событие обновления мероприятия.
     *
     * @param eventId   идентификатор мероприятия
     * @param tenantId  идентификатор организации
     * @param title     название мероприятия
     * @param startsAt  дата начала
     * @param updatedAt время обновления
     */
    public EventUpdatedEvent(UUID eventId, UUID tenantId, String title, Instant startsAt, Instant updatedAt) {
        super();
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.title = title;
        this.startsAt = startsAt;
        this.updatedAt = updatedAt;
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

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String getEventType() {
        return "event.updated";
    }

    @Override
    public UUID getAggregateId() {
        return eventId;
    }
}
