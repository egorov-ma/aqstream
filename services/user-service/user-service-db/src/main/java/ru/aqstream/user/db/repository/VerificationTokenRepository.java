package ru.aqstream.user.db.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.db.entity.VerificationToken;
import ru.aqstream.user.db.entity.VerificationToken.TokenType;

/**
 * Репозиторий для работы с токенами верификации.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    /**
     * Ищет токен по значению.
     *
     * @param token значение токена
     * @return токен если найден
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Ищет неиспользованный токен определённого типа для пользователя.
     *
     * @param userId идентификатор пользователя
     * @param type   тип токена
     * @return токен если найден
     */
    @Query("""
        SELECT t FROM VerificationToken t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.usedAt IS NULL
          AND t.expiresAt > :now
        ORDER BY t.createdAt DESC
        LIMIT 1
        """)
    Optional<VerificationToken> findValidTokenByUserIdAndType(UUID userId, TokenType type, Instant now);

    /**
     * Инвалидирует все неиспользованные токены определённого типа для пользователя.
     * Вызывается при создании нового токена.
     *
     * @param userId идентификатор пользователя
     * @param type   тип токена
     * @param now    текущее время
     * @return количество инвалидированных токенов
     */
    @Modifying
    @Query("""
        UPDATE VerificationToken t
        SET t.usedAt = :now
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.usedAt IS NULL
        """)
    int invalidateAllByUserIdAndType(UUID userId, TokenType type, Instant now);

    /**
     * Удаляет истёкшие токены.
     *
     * @param before удалить токены, истёкшие до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(Instant before);

    /**
     * Удаляет использованные токены старше указанного времени.
     *
     * @param before удалить токены, использованные до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.usedAt IS NOT NULL AND t.usedAt < :before")
    int deleteUsedBefore(Instant before);

    /**
     * Подсчитывает количество запросов на верификацию за период.
     * Используется для rate limiting.
     *
     * @param userId идентификатор пользователя
     * @param type   тип токена
     * @param since  начало периода
     * @return количество запросов
     */
    @Query("""
        SELECT COUNT(t) FROM VerificationToken t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.createdAt >= :since
        """)
    long countByUserIdAndTypeSince(UUID userId, TokenType type, Instant since);
}
