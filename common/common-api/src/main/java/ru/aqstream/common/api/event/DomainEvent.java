package ru.aqstream.common.api.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.UUID;

/**
 * Базовый класс для всех доменных событий.
 * Используется для event-driven коммуникации между сервисами через RabbitMQ.
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    /**
     * Уникальный идентификатор события.
     *
     * @return UUID события
     */
    public UUID getEventId() {
        return eventId;
    }

    /**
     * Время возникновения события (UTC).
     *
     * @return момент времени
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Тип события для роутинга в RabbitMQ.
     * Формат: "{domain}.{action}", например "event.created", "user.registered".
     *
     * @return строка с типом события
     */
    @JsonIgnore
    public abstract String getEventType();

    /**
     * Идентификатор агрегата, к которому относится событие.
     * Используется для партиционирования и ordering guarantee.
     *
     * @return UUID агрегата
     */
    @JsonIgnore
    public abstract UUID getAggregateId();
}
