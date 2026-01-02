package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение: токен авторизации через Telegram бота не найден.
 */
public class TelegramAuthTokenNotFoundException extends EntityNotFoundException {

    private static final String CODE = "telegram_auth_token_not_found";
    private static final String MESSAGE = "Токен авторизации не найден";

    public TelegramAuthTokenNotFoundException() {
        super(CODE, MESSAGE, Map.of());
    }
}
