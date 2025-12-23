package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение: токен привязки Telegram не найден.
 */
public class TelegramLinkTokenNotFoundException extends EntityNotFoundException {

    private static final String CODE = "telegram_link_token_not_found";
    private static final String MESSAGE = "Токен привязки не найден или недействителен";

    public TelegramLinkTokenNotFoundException() {
        super(CODE, MESSAGE, Map.of());
    }
}
