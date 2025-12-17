package ru.aqstream.user.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.user.db.repository.RefreshTokenRepository;

/**
 * Сервис для периодической очистки истёкших токенов.
 *
 * <p>Удаляет refresh токены, срок действия которых истёк,
 * для предотвращения неограниченного роста таблицы.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Удаляет истёкшие refresh токены.
     * Выполняется каждый час.
     */
    @Scheduled(cron = "${token.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredBefore(Instant.now());

        if (deletedCount > 0) {
            log.info("Очистка токенов: удалено истёкших токенов={}", deletedCount);
        } else {
            log.debug("Очистка токенов: истёкших токенов не найдено");
        }
    }
}
