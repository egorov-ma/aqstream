package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при попытке выполнить операцию, недопустимую для опубликованного события.
 * Преобразуется в HTTP 409 Conflict.
 */
public class EventAlreadyPublishedException extends ConflictException {

    /**
     * Создаёт исключение для опубликованного события.
     *
     * @param eventId идентификатор события
     */
    public EventAlreadyPublishedException(UUID eventId) {
        super(
            "event_already_published",
            "Событие уже опубликовано",
            Map.of("eventId", eventId.toString())
        );
    }

    /**
     * Создаёт исключение с кастомным сообщением.
     *
     * @param eventId идентификатор события
     * @param message сообщение об ошибке
     */
    public EventAlreadyPublishedException(UUID eventId, String message) {
        super(
            "event_already_published",
            message,
            Map.of("eventId", eventId.toString())
        );
    }
}
