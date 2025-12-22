package ru.aqstream.notification.api.exception;

import ru.aqstream.common.api.exception.InternalServerException;

import java.util.Map;

/**
 * Исключение при ошибке отправки сообщения в Telegram.
 * Преобразуется в HTTP 500 Internal Server Error.
 */
public class TelegramSendException extends InternalServerException {

    private static final String CODE = "telegram_send_error";

    /**
     * Создаёт исключение с указанием chatId.
     *
     * @param chatId ID чата, в который не удалось отправить сообщение
     */
    public TelegramSendException(Long chatId) {
        super(CODE, "Ошибка отправки сообщения в Telegram", Map.of("chatId", chatId));
    }

    /**
     * Создаёт исключение с причиной.
     *
     * @param chatId ID чата
     * @param cause  причина ошибки
     */
    public TelegramSendException(Long chatId, Throwable cause) {
        super(CODE, "Ошибка отправки сообщения в Telegram: " + cause.getMessage(), cause);
    }

    /**
     * Создаёт исключение с кодом ошибки Telegram API.
     *
     * @param chatId      ID чата
     * @param errorCode   код ошибки Telegram
     * @param description описание ошибки
     */
    public TelegramSendException(Long chatId, int errorCode, String description) {
        super(CODE, String.format("Ошибка Telegram API: code=%d, description=%s", errorCode, description),
                Map.of("chatId", chatId, "errorCode", errorCode, "description", description));
    }
}
