package ru.aqstream.user.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.user.db.repository.RefreshTokenRepository;

/**
 * Сервис для периодической очистки токенов.
 *
 * <p>Удаляет:</p>
 * <ul>
 *   <li>Истёкшие refresh токены</li>
 *   <li>Отозванные токены старше 30 дней (после периода хранения для аудита)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    /**
     * Период хранения отозванных токенов для аудита (30 дней).
     */
    private static final Duration REVOKED_TOKEN_RETENTION = Duration.ofDays(30);

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Удаляет истёкшие и старые отозванные refresh токены.
     * Выполняется каждый час.
     */
    @Scheduled(cron = "${token.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanupTokens() {
        Instant now = Instant.now();

        // Удаляем истёкшие токены
        int expiredCount = refreshTokenRepository.deleteExpiredBefore(now);

        // Удаляем отозванные токены старше 30 дней
        Instant revokedBefore = now.minus(REVOKED_TOKEN_RETENTION);
        int revokedCount = refreshTokenRepository.deleteRevokedBefore(revokedBefore);

        int totalDeleted = expiredCount + revokedCount;
        if (totalDeleted > 0) {
            log.info("Очистка токенов: удалено истёкших={}, отозванных={}", expiredCount, revokedCount);
        } else {
            log.debug("Очистка токенов: токенов для удаления не найдено");
        }
    }
}
