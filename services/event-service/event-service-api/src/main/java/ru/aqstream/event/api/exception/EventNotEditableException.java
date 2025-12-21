package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;
import ru.aqstream.event.api.dto.EventStatus;

/**
 * Исключение для попытки редактирования события в неизменяемом статусе.
 * Преобразуется в HTTP 409 Conflict.
 */
public class EventNotEditableException extends ConflictException {

    /**
     * Создаёт исключение для неизменяемого события.
     *
     * @param eventId идентификатор события
     * @param status  текущий статус события
     */
    public EventNotEditableException(UUID eventId, EventStatus status) {
        super(
            "event_not_editable",
            "Событие в статусе " + status.name() + " нельзя редактировать",
            Map.of("eventId", eventId.toString(), "status", status.name())
        );
    }
}
