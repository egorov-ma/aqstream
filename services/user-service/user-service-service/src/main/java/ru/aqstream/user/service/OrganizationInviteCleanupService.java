package ru.aqstream.user.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;

/**
 * Сервис очистки истёкших приглашений.
 * Запускается по расписанию для удаления неиспользованных приглашений.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationInviteCleanupService {

    private final OrganizationInviteRepository inviteRepository;

    /**
     * Удаляет истёкшие приглашения.
     * Запускается каждый час.
     */
    @Scheduled(cron = "${organization.invite.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredInvites() {
        int deleted = inviteRepository.deleteExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Очистка приглашений: удалено={}", deleted);
        }
    }
}
