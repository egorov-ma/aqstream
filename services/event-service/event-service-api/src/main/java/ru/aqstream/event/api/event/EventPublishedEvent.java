package ru.aqstream.event.api.event;

import java.time.Instant;
import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие публикации мероприятия.
 * Публикуется когда мероприятие переходит из DRAFT в PUBLISHED.
 */
public class EventPublishedEvent extends DomainEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final String title;
    private final String slug;
    private final Instant startsAt;
    private final Instant publishedAt;

    /**
     * Создаёт событие публикации мероприятия.
     *
     * @param eventId     идентификатор мероприятия
     * @param tenantId    идентификатор организации
     * @param title       название мероприятия
     * @param slug        URL-адрес мероприятия
     * @param startsAt    дата начала
     * @param publishedAt дата публикации
     */
    public EventPublishedEvent(
        UUID eventId,
        UUID tenantId,
        String title,
        String slug,
        Instant startsAt,
        Instant publishedAt
    ) {
        super();
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.title = title;
        this.slug = slug;
        this.startsAt = startsAt;
        this.publishedAt = publishedAt;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    @Override
    public String getEventType() {
        return "event.published";
    }

    @Override
    public UUID getAggregateId() {
        return eventId;
    }
}
