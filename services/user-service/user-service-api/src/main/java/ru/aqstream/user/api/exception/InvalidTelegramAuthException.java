package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение: невалидные данные Telegram аутентификации.
 * HTTP статус: 401 Unauthorized
 *
 * <p>Возникает когда:</p>
 * <ul>
 *   <li>Hash не соответствует данным (подделка)</li>
 *   <li>auth_date слишком старый (более 1 часа)</li>
 * </ul>
 */
public class InvalidTelegramAuthException extends AqStreamException {

    private static final String CODE = "invalid_telegram_auth";
    private static final String MESSAGE = "Невалидные данные Telegram аутентификации";

    public InvalidTelegramAuthException() {
        super(CODE, MESSAGE);
    }

    public InvalidTelegramAuthException(String reason) {
        super(CODE, MESSAGE + ": " + reason);
    }
}
