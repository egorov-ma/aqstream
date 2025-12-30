package ru.aqstream.notification.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение, выбрасываемое когда уведомление не найдено.
 */
public class UserNotificationNotFoundException extends EntityNotFoundException {

    public UserNotificationNotFoundException(UUID notificationId) {
        super("UserNotification", notificationId);
    }

    public UserNotificationNotFoundException(UUID notificationId, UUID userId) {
        super(
            "user_notification_not_found",
            "Уведомление не найдено или не принадлежит пользователю",
            Map.of(
                "notificationId", notificationId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
