package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение: пользователь не найден.
 * HTTP статус: 404 Not Found
 */
public class UserNotFoundException extends EntityNotFoundException {

    private static final String CODE = "user_not_found";

    public UserNotFoundException(UUID userId) {
        super(CODE, "Пользователь не найден", Map.of("userId", userId.toString()));
    }
}
