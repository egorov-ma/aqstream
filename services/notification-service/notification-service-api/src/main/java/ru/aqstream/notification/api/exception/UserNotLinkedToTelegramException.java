package ru.aqstream.notification.api.exception;

import ru.aqstream.common.api.exception.ValidationException;

import java.util.Map;
import java.util.UUID;

/**
 * Исключение когда у пользователя не привязан Telegram.
 * Преобразуется в HTTP 400 Bad Request.
 */
public class UserNotLinkedToTelegramException extends ValidationException {

    /**
     * Создаёт исключение с указанием userId.
     *
     * @param userId ID пользователя
     */
    public UserNotLinkedToTelegramException(UUID userId) {
        super("Пользователь не привязал Telegram", Map.of("userId", userId.toString()));
    }
}
