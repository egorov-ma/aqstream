package ru.aqstream.user.db.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.db.entity.AuthTokenStatus;
import ru.aqstream.user.db.entity.TelegramAuthToken;

/**
 * Репозиторий для работы с токенами авторизации через Telegram бота.
 */
@Repository
public interface TelegramAuthTokenRepository extends JpaRepository<TelegramAuthToken, UUID> {

    /**
     * Ищет токен по значению.
     *
     * @param token значение токена
     * @return токен если найден
     */
    Optional<TelegramAuthToken> findByToken(String token);

    /**
     * Ищет токен по значению и статусу.
     *
     * @param token  значение токена
     * @param status статус токена
     * @return токен если найден
     */
    Optional<TelegramAuthToken> findByTokenAndStatus(String token, AuthTokenStatus status);

    /**
     * Ищет валидный токен для подтверждения (PENDING и не истёк).
     *
     * @param token значение токена
     * @param now   текущее время
     * @return токен если найден
     */
    @Query("""
        SELECT t FROM TelegramAuthToken t
        WHERE t.token = :token
          AND t.status = 'PENDING'
          AND t.expiresAt > :now
        """)
    Optional<TelegramAuthToken> findPendingByToken(String token, Instant now);

    /**
     * Ищет подтверждённый токен для использования (CONFIRMED и не истёк).
     *
     * @param token значение токена
     * @param now   текущее время
     * @return токен если найден
     */
    @Query("""
        SELECT t FROM TelegramAuthToken t
        WHERE t.token = :token
          AND t.status = 'CONFIRMED'
          AND t.expiresAt > :now
        """)
    Optional<TelegramAuthToken> findConfirmedByToken(String token, Instant now);

    /**
     * Помечает истёкшие токены как EXPIRED.
     *
     * @param now текущее время
     * @return количество обновлённых токенов
     */
    @Modifying
    @Query("""
        UPDATE TelegramAuthToken t
        SET t.status = 'EXPIRED'
        WHERE t.status = 'PENDING'
          AND t.expiresAt < :now
        """)
    int markExpiredTokens(Instant now);

    /**
     * Удаляет старые токены (использованные или истёкшие).
     *
     * @param before удалить токены, созданные до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("""
        DELETE FROM TelegramAuthToken t
        WHERE t.status IN ('USED', 'EXPIRED')
          AND t.createdAt < :before
        """)
    int deleteOldTokens(Instant before);

    /**
     * Удаляет все истёкшие токены.
     *
     * @param before удалить токены, истёкшие до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("DELETE FROM TelegramAuthToken t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(Instant before);
}
