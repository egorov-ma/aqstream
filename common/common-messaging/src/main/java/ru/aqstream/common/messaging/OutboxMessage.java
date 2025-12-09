package ru.aqstream.common.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Сущность для Outbox pattern.
 * Сообщения сохраняются в БД в той же транзакции, что и бизнес-данные,
 * а затем асинхронно отправляются в RabbitMQ.
 *
 * <p>Это обеспечивает reliable event publishing без потери сообщений
 * при сбоях между сохранением данных и отправкой в очередь.</p>
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected OutboxMessage() {
        // Для JPA
    }

    /**
     * Создаёт новое сообщение для outbox.
     *
     * @param aggregateId   идентификатор агрегата
     * @param aggregateType тип агрегата (например, "Event", "User")
     * @param eventType     тип события (например, "event.created")
     * @param payload       JSON payload события
     */
    public OutboxMessage(UUID aggregateId, String aggregateType, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    /**
     * Проверяет, обработано ли сообщение.
     *
     * @return true если сообщение уже отправлено
     */
    public boolean isProcessed() {
        return processedAt != null;
    }

    /**
     * Помечает сообщение как обработанное.
     */
    public void markProcessed() {
        this.processedAt = Instant.now();
    }

    /**
     * Регистрирует неудачную попытку отправки.
     *
     * @param error описание ошибки
     */
    public void recordFailure(String error) {
        this.retryCount++;
        this.lastError = error != null && error.length() > 1000
            ? error.substring(0, 1000)
            : error;
    }
}
