package ru.aqstream.event.api.event;

import java.time.Instant;
import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие отмены мероприятия.
 * Публикуется для уведомления всех зарегистрированных участников.
 */
public class EventCancelledEvent extends DomainEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final String title;
    private final Instant startsAt;
    private final Instant cancelledAt;
    private final String cancelReason;

    /**
     * Создаёт событие отмены мероприятия.
     *
     * @param eventId      идентификатор мероприятия
     * @param tenantId     идентификатор организации
     * @param title        название мероприятия
     * @param startsAt     дата начала (для информирования участников)
     * @param cancelledAt  дата отмены
     * @param cancelReason причина отмены (опционально)
     */
    public EventCancelledEvent(UUID eventId, UUID tenantId, String title, Instant startsAt,
                               Instant cancelledAt, String cancelReason) {
        super();
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.title = title;
        this.startsAt = startsAt;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
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

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    @Override
    public String getEventType() {
        return "event.cancelled";
    }

    @Override
    public UUID getAggregateId() {
        return eventId;
    }
}
