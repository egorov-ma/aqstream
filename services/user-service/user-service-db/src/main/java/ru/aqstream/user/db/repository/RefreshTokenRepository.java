package ru.aqstream.user.db.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.db.entity.RefreshToken;

/**
 * Репозиторий для работы с refresh токенами.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Ищет токен по хешу.
     *
     * @param tokenHash хеш токена
     * @return токен если найден
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Отзывает все токены пользователя.
     *
     * @param userId идентификатор пользователя
     * @return количество отозванных токенов
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :now "
         + "WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllByUserId(UUID userId, Instant now);

    /**
     * Удаляет истёкшие токены.
     *
     * @param before удалить токены, истёкшие до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(Instant before);

    /**
     * Удаляет отозванные токены старше указанного времени.
     * Используется для очистки после периода хранения для аудита.
     *
     * @param before удалить токены, отозванные до этого времени
     * @return количество удалённых токенов
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.revoked = true AND t.revokedAt < :before")
    int deleteRevokedBefore(Instant before);

    /**
     * Подсчитывает активные токены пользователя.
     *
     * @param userId идентификатор пользователя
     * @return количество активных токенов
     */
    @Query("SELECT COUNT(t) FROM RefreshToken t WHERE t.user.id = :userId AND t.revoked = false AND t.expiresAt > :now")
    long countActiveByUserId(UUID userId, Instant now);

    /**
     * Отзывает старейшие активные токены пользователя сверх лимита.
     * Сохраняет только keepCount самых новых токенов.
     *
     * @param userId    идентификатор пользователя
     * @param keepCount количество токенов для сохранения
     * @param now       текущее время
     * @return количество отозванных токенов
     */
    @Modifying
    @Query(value = """
        UPDATE user_service.refresh_tokens
        SET revoked = true, revoked_at = :now
        WHERE user_id = :userId
          AND revoked = false
          AND expires_at > :now
          AND id NOT IN (
              SELECT id FROM user_service.refresh_tokens
              WHERE user_id = :userId
                AND revoked = false
                AND expires_at > :now
              ORDER BY created_at DESC
              LIMIT :keepCount
          )
        """, nativeQuery = true)
    int revokeOldestTokensExceedingLimit(UUID userId, int keepCount, Instant now);
}
