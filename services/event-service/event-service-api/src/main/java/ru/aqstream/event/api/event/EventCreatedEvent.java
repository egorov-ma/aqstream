package ru.aqstream.event.api.event;

import java.time.Instant;
import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие создания мероприятия.
 */
public class EventCreatedEvent extends DomainEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final String title;
    private final String slug;
    private final Instant startsAt;

    /**
     * Создаёт событие создания мероприятия.
     *
     * @param eventId  идентификатор мероприятия
     * @param tenantId идентификатор организации
     * @param title    название мероприятия
     * @param slug     URL-адрес мероприятия
     * @param startsAt дата начала
     */
    public EventCreatedEvent(UUID eventId, UUID tenantId, String title, String slug, Instant startsAt) {
        super();
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.title = title;
        this.slug = slug;
        this.startsAt = startsAt;
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

    public String getSlug() {
        return slug;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    @Override
    public String getEventType() {
        return "event.created";
    }

    @Override
    public UUID getAggregateId() {
        return eventId;
    }
}
