package ru.aqstream.common.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с outbox сообщениями.
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    /**
     * Находит необработанные сообщения для отправки с блокировкой.
     * Использует FOR UPDATE SKIP LOCKED для предотвращения параллельной обработки
     * одних и тех же сообщений несколькими инстансами.
     *
     * @param maxRetries максимальное количество повторных попыток
     * @param limit      максимальное количество сообщений
     * @return список сообщений для обработки
     */
    @Query(value = """
        SELECT * FROM outbox_messages
        WHERE processed_at IS NULL
          AND retry_count < :maxRetries
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxMessage> findUnprocessedMessages(
        @Param("maxRetries") int maxRetries,
        @Param("limit") int limit
    );

    /**
     * Находит необработанные сообщения для конкретного агрегата.
     * Используется для гарантии порядка событий в рамках агрегата.
     *
     * @param aggregateId идентификатор агрегата
     * @return список сообщений
     */
    @Query("""
        SELECT m FROM OutboxMessage m
        WHERE m.aggregateId = :aggregateId
          AND m.processedAt IS NULL
        ORDER BY m.createdAt ASC
        """)
    List<OutboxMessage> findUnprocessedByAggregateId(@Param("aggregateId") UUID aggregateId);

    /**
     * Удаляет обработанные сообщения старше указанной даты.
     * Используется для очистки таблицы outbox.
     *
     * @param before граница времени
     * @return количество удалённых записей
     */
    @Modifying
    @Query("""
        DELETE FROM OutboxMessage m
        WHERE m.processedAt IS NOT NULL
          AND m.processedAt < :before
        """)
    int deleteProcessedBefore(@Param("before") Instant before);

    /**
     * Подсчитывает количество необработанных сообщений.
     * Используется для мониторинга.
     *
     * @return количество необработанных сообщений
     */
    @Query("SELECT COUNT(m) FROM OutboxMessage m WHERE m.processedAt IS NULL")
    long countUnprocessed();

    /**
     * Подсчитывает количество сообщений с ошибками.
     * Используется для мониторинга.
     *
     * @param maxRetries порог количества попыток
     * @return количество проблемных сообщений
     */
    @Query("SELECT COUNT(m) FROM OutboxMessage m WHERE m.processedAt IS NULL AND m.retryCount >= :maxRetries")
    long countFailed(@Param("maxRetries") int maxRetries);
}
