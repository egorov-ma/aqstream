package ru.aqstream.common.messaging;

/**
 * Исключение при ошибке публикации события.
 */
public class EventPublishingException extends RuntimeException {

    public EventPublishingException(String message) {
        super(message);
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
