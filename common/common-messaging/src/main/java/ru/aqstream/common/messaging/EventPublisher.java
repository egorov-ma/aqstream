package ru.aqstream.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Публикатор доменных событий через Outbox pattern.
 *
 * <p>События сохраняются в таблицу outbox в текущей транзакции,
 * а затем асинхронно отправляются в RabbitMQ через {@link OutboxProcessor}.</p>
 *
 * <p>ВАЖНО: Метод {@link #publish(DomainEvent)} должен вызываться внутри транзакции.</p>
 *
 * <pre>
 * {@code
 * @Transactional
 * public Event createEvent(CreateEventRequest request) {
 *     Event event = eventRepository.save(mapToEntity(request));
 *     eventPublisher.publish(new EventCreatedEvent(event));
 *     return event;
 * }
 * }
 * </pre>
 */
@Component
@ConditionalOnBean(OutboxRepository.class)
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public EventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Публикует доменное событие через outbox.
     *
     * <p>Событие сериализуется в JSON и сохраняется в таблицу outbox_messages.
     * Отправка в RabbitMQ происходит асинхронно через OutboxProcessor.</p>
     *
     * @param event доменное событие
     * @throws IllegalStateException если вызвано вне транзакции
     * @throws EventPublishingException если не удалось сериализовать событие
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации события: eventType={}, eventId={}",
                event.getEventType(), event.getEventId(), e);
            throw new EventPublishingException(
                "Не удалось сериализовать событие: " + event.getEventType(), e
            );
        }

        // Определяем тип агрегата из типа события (event.created -> Event)
        String aggregateType = extractAggregateType(event.getEventType());

        OutboxMessage message = new OutboxMessage(
            event.getAggregateId(),
            aggregateType,
            event.getEventType(),
            payload
        );

        outboxRepository.save(message);

        log.debug("Событие добавлено в outbox: eventType={}, eventId={}, aggregateId={}",
            event.getEventType(), event.getEventId(), event.getAggregateId());
    }

    /**
     * Извлекает тип агрегата из типа события.
     * Например: "event.created" -> "Event", "user.registered" -> "User"
     *
     * @param eventType тип события
     * @return тип агрегата с заглавной буквы
     */
    private String extractAggregateType(String eventType) {
        if (eventType == null || eventType.isEmpty()) {
            return "Unknown";
        }
        int dotIndex = eventType.indexOf('.');
        String aggregate = dotIndex > 0 ? eventType.substring(0, dotIndex) : eventType;
        return aggregate.substring(0, 1).toUpperCase() + aggregate.substring(1);
    }
}
