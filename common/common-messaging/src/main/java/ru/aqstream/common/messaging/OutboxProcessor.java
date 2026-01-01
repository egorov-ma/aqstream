package ru.aqstream.common.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Процессор для обработки outbox сообщений.
 * Периодически читает необработанные сообщения из таблицы outbox и отправляет их в RabbitMQ.
 *
 * <p>Конфигурируется через application.yml:</p>
 * <pre>
 * outbox:
 *   processor:
 *     enabled: true
 *     batch-size: 100
 *     max-retries: 5
 *     retention-days: 7
 * </pre>
 *
 * <p>Для отключения в тестах: {@code outbox.processor.enabled=false}</p>
 */
@Component
@ConditionalOnBean(OutboxRepository.class)
@ConditionalOnProperty(name = "outbox.processor.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;
    private final int maxRetries;
    private final int retentionDays;
    private final String exchangeName;

    public OutboxProcessor(
        OutboxRepository outboxRepository,
        RabbitTemplate rabbitTemplate,
        @Value("${outbox.processor.batch-size:100}") int batchSize,
        @Value("${outbox.processor.max-retries:5}") int maxRetries,
        @Value("${outbox.processor.retention-days:7}") int retentionDays,
        @Value("${outbox.exchange:aqstream.events}") String exchangeName
    ) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.retentionDays = retentionDays;
        this.exchangeName = exchangeName;
    }

    /**
     * Обрабатывает необработанные сообщения из outbox.
     * Запускается каждую секунду.
     */
    @Scheduled(fixedDelayString = "${outbox.processor.interval:1000}")
    @Transactional
    public void processOutbox() {
        List<OutboxMessage> messages = outboxRepository.findUnprocessedMessages(maxRetries, batchSize);

        if (messages.isEmpty()) {
            return;
        }

        log.debug("Обработка {} сообщений из outbox", messages.size());

        int successCount = 0;
        int failureCount = 0;

        for (OutboxMessage message : messages) {
            try {
                // Отправляем сообщение в RabbitMQ
                // routing key = event type (например, "event.created")
                rabbitTemplate.convertAndSend(
                    exchangeName,
                    message.getEventType(),
                    message.getPayload()
                );

                message.markProcessed();
                outboxRepository.save(message);
                successCount++;

                log.debug("Сообщение отправлено: id={}, eventType={}",
                    message.getId(), message.getEventType());

            } catch (Exception e) {
                message.recordFailure(e.getMessage());
                outboxRepository.save(message);
                failureCount++;

                log.warn("Ошибка отправки сообщения: id={}, eventType={}, попытка={}, ошибка={}",
                    message.getId(), message.getEventType(), message.getRetryCount(), e.getMessage());
            }
        }

        if (successCount > 0 || failureCount > 0) {
            log.info("Обработка outbox завершена: успешно={}, ошибок={}", successCount, failureCount);
        }
    }

    /**
     * Удаляет старые обработанные сообщения.
     * Запускается раз в час.
     */
    @Scheduled(fixedDelayString = "${outbox.cleanup.interval:3600000}")
    @Transactional
    public void cleanupProcessedMessages() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = outboxRepository.deleteProcessedBefore(cutoff);

        if (deleted > 0) {
            log.info("Очистка outbox: удалено {} обработанных сообщений старше {} дней",
                deleted, retentionDays);
        }
    }
}
