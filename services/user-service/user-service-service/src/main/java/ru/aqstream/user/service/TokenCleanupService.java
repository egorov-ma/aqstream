package ru.aqstream.user.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.VerificationTokenRepository;

/**
 * Сервис для периодической очистки токенов.
 *
 * <p>Удаляет:</p>
 * <ul>
 *   <li>Истёкшие refresh токены</li>
 *   <li>Отозванные токены старше 30 дней (после периода хранения для аудита)</li>
 *   <li>Истёкшие токены верификации</li>
 *   <li>Использованные токены верификации старше 7 дней</li>
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

    /**
     * Период хранения использованных токенов верификации для аудита (7 дней).
     */
    private static final Duration USED_VERIFICATION_TOKEN_RETENTION = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    /**
     * Удаляет истёкшие и старые отозванные refresh токены.
     * Выполняется каждый час.
     */
    @Scheduled(cron = "${token.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanupTokens() {
        Instant now = Instant.now();

        // Очистка refresh токенов
        int expiredCount = refreshTokenRepository.deleteExpiredBefore(now);
        Instant revokedBefore = now.minus(REVOKED_TOKEN_RETENTION);
        int revokedCount = refreshTokenRepository.deleteRevokedBefore(revokedBefore);

        int refreshDeleted = expiredCount + revokedCount;
        if (refreshDeleted > 0) {
            log.info("Очистка refresh токенов: удалено истёкших={}, отозванных={}",
                expiredCount, revokedCount);
        }

        // Очистка токенов верификации
        int verificationExpiredCount = verificationTokenRepository.deleteExpiredBefore(now);
        Instant usedBefore = now.minus(USED_VERIFICATION_TOKEN_RETENTION);
        int verificationUsedCount = verificationTokenRepository.deleteUsedBefore(usedBefore);

        int verificationDeleted = verificationExpiredCount + verificationUsedCount;
        if (verificationDeleted > 0) {
            log.info("Очистка токенов верификации: удалено истёкших={}, использованных={}",
                verificationExpiredCount, verificationUsedCount);
        }

        if (refreshDeleted == 0 && verificationDeleted == 0) {
            log.debug("Очистка токенов: токенов для удаления не найдено");
        }
    }
}
