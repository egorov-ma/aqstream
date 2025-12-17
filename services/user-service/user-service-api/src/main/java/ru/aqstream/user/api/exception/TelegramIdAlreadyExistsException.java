package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;
import ru.aqstream.user.api.util.TelegramUtils;

/**
 * Исключение: Telegram ID уже привязан к другому аккаунту.
 * HTTP статус: 409 Conflict
 */
public class TelegramIdAlreadyExistsException extends ConflictException {

    private static final String CODE = "telegram_id_already_exists";

    public TelegramIdAlreadyExistsException() {
        super(CODE, "Telegram аккаунт уже привязан к другому пользователю");
    }

    public TelegramIdAlreadyExistsException(String telegramId) {
        super(CODE, "Telegram аккаунт " + TelegramUtils.maskTelegramId(telegramId)
            + " уже привязан к другому пользователю");
    }
}
