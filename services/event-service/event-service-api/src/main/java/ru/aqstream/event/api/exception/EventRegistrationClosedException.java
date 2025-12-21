package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение для случаев, когда регистрация на событие закрыта.
 * Преобразуется в HTTP 409 Conflict.
 */
public class EventRegistrationClosedException extends ConflictException {

    /**
     * Создаёт исключение для закрытой регистрации.
     *
     * @param eventId идентификатор события
     */
    public EventRegistrationClosedException(UUID eventId) {
        super(
            "event_registration_closed",
            "Регистрация на событие закрыта",
            Map.of("eventId", eventId.toString())
        );
    }

    /**
     * Создаёт исключение для закрытой регистрации с причиной.
     *
     * @param eventId идентификатор события
     * @param reason  причина закрытия
     */
    public EventRegistrationClosedException(UUID eventId, String reason) {
        super(
            "event_registration_closed",
            reason,
            Map.of("eventId", eventId.toString())
        );
    }
}
