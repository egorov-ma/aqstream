package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение: токен авторизации через Telegram бота истёк или уже использован.
 */
public class TelegramAuthTokenExpiredException extends EntityNotFoundException {

    private static final String CODE = "telegram_auth_token_expired";
    private static final String MESSAGE = "Ссылка для входа устарела. Попробуйте снова.";

    public TelegramAuthTokenExpiredException() {
        super(CODE, MESSAGE, Map.of());
    }
}
