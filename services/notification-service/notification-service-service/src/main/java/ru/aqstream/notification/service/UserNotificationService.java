package ru.aqstream.notification.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.notification.api.dto.UserNotificationDto;
import ru.aqstream.notification.api.dto.UserNotificationDto.LinkedEntityDto;
import ru.aqstream.notification.api.exception.UserNotificationNotFoundException;
import ru.aqstream.notification.db.entity.UserNotification;
import ru.aqstream.notification.db.repository.UserNotificationRepository;

/**
 * Сервис для работы с UI-уведомлениями пользователя (bell icon).
 *
 * <p>Все методы используют TenantContext для tenant isolation (Defense in Depth).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UserNotificationRepository repository;

    /**
     * Возвращает список уведомлений пользователя.
     *
     * @param userId ID пользователя
     * @param page   номер страницы (0-based)
     * @param size   размер страницы
     * @return страница уведомлений
     */
    @Transactional(readOnly = true)
    public PageResponse<UserNotificationDto> getNotifications(UUID userId, int page, int size) {
        UUID tenantId = TenantContext.getTenantId();
        log.debug("Получение уведомлений: tenantId={}, userId={}, page={}, size={}",
            tenantId, userId, page, size);

        Pageable pageable = PageRequest.of(page, size > 0 ? size : DEFAULT_PAGE_SIZE);
        Page<UserNotification> notificationPage =
            repository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, pageable);

        return PageResponse.of(notificationPage, this::toDto);
    }

    /**
     * Возвращает количество непрочитанных уведомлений.
     *
     * @param userId ID пользователя
     * @return количество непрочитанных
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.countUnreadByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * Отмечает уведомление как прочитанное.
     *
     * @param userId         ID пользователя
     * @param notificationId ID уведомления
     * @throws UserNotificationNotFoundException если уведомление не найдено или не принадлежит пользователю
     */
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Отметка уведомления как прочитанного: tenantId={}, notificationId={}, userId={}",
            tenantId, notificationId, userId);

        UserNotification notification = repository
            .findByIdAndTenantIdAndUserId(notificationId, tenantId, userId)
            .orElseThrow(() -> new UserNotificationNotFoundException(notificationId, userId));

        notification.markAsRead();
        repository.save(notification);
        log.info("Уведомление отмечено как прочитанное: notificationId={}", notificationId);
    }

    /**
     * Отмечает все уведомления пользователя как прочитанные.
     *
     * @param userId ID пользователя
     * @return количество отмеченных
     */
    @Transactional
    public int markAllAsRead(UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Отметка всех уведомлений как прочитанных: tenantId={}, userId={}", tenantId, userId);

        int count = repository.markAllAsReadByTenantIdAndUserId(tenantId, userId);
        log.info("Отмечено уведомлений: count={}, tenantId={}, userId={}", count, tenantId, userId);
        return count;
    }

    /**
     * Создаёт уведомление (вызывается из event listeners).
     *
     * @param notification уведомление
     * @return созданное уведомление
     */
    @Transactional
    public UserNotification create(UserNotification notification) {
        log.info("Создание уведомления: tenantId={}, userId={}, type={}, title={}",
            notification.getTenantId(), notification.getUserId(),
            notification.getType(), notification.getTitle());
        return repository.save(notification);
    }

    private UserNotificationDto toDto(UserNotification entity) {
        LinkedEntityDto linkedEntity = null;
        if (entity.getLinkedEntityType() != null && entity.getLinkedEntityId() != null) {
            linkedEntity = new LinkedEntityDto(
                entity.getLinkedEntityType(),
                entity.getLinkedEntityId()
            );
        }

        return new UserNotificationDto(
            entity.getId(),
            entity.getType(),
            entity.getTitle(),
            entity.getMessage(),
            entity.isRead(),
            linkedEntity,
            entity.getCreatedAt()
        );
    }
}
