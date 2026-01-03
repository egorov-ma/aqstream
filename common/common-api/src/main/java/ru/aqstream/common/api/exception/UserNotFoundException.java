package ru.aqstream.common.api.exception;

import java.util.UUID;

/**
 * Исключение при отсутствии пользователя.
 * Выбрасывается когда UserClient.findById() возвращает empty Optional.
 */
public class UserNotFoundException extends EntityNotFoundException {

    public UserNotFoundException(UUID userId) {
        super("User", userId);
    }
}
