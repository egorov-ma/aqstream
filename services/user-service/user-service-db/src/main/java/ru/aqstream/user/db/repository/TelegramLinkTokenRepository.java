package ru.aqstream.user.db.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.db.entity.TelegramLinkToken;

/**
 * Репозиторий для работы с токенами привязки Telegram.
 */
@Repository
public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, UUID> {

    /**
     * Ищет токен по значению.
     *
     * @param token значение токена
     * @return токен если найден
     */
    Optional<TelegramLinkToken> findByToken(String token);

    /**
     * Ищет неиспользованный валидный токен для пользователя.
     *
     * @param userId идентификатор пользователя
     * @param now    текущее время
     * @return токен если найден
     */
    @Query("""
        SELECT t FROM TelegramLinkToken t
        WHERE t.user.id = :userId
          AND t.usedAt IS NULL
          AND t.expiresAt > :now
        ORDER BY t.createdAt DESC
        LIMIT 1
        """)
    Optional<TelegramLinkToken> findValidTokenByUserId(UUID userId, Instant now);

    /**
     * Инвалидирует все неиспользованные токены для пользователя.
     * Вызывается при создании нового токена.
     *
     * @param userId идентификатор пользователя
     * @param now    текущее время
     * @return количество инвалидированных токенов
     */
    @Modifying
    @Query("""
        UPDATE TelegramLinkToken t
        SET t.usedAt = :now
        WHERE t.user.id = :userId
          AND t.usedAt IS NULL
        """)
    int invalidateAllByUserId(UUID userId, Instant now);

    /**
     * Удаляет истёкшие токены.
     *
     * @param before удалить токены, истёкшие до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("DELETE FROM TelegramLinkToken t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(Instant before);

    /**
     * Удаляет использованные токены старше указанного времени.
     *
     * @param before удалить токены, использованные до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("DELETE FROM TelegramLinkToken t WHERE t.usedAt IS NOT NULL AND t.usedAt < :before")
    int deleteUsedBefore(Instant before);
}
