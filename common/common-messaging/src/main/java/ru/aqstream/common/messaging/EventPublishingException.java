package ru.aqstream.common.messaging;

import ru.aqstream.common.api.exception.InternalServerException;

/**
 * Исключение при ошибке публикации события.
 * HTTP статус: 500 Internal Server Error.
 */
public class EventPublishingException extends InternalServerException {

    private static final String CODE = "event_publishing_failed";

    public EventPublishingException(String message) {
        super(CODE, message);
    }

    public EventPublishingException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
