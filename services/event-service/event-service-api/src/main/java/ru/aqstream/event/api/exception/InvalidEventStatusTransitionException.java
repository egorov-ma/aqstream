package ru.aqstream.event.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.ConflictException;
import ru.aqstream.event.api.dto.EventStatus;

/**
 * Исключение для некорректного перехода статуса события.
 * Преобразуется в HTTP 409 Conflict.
 */
public class InvalidEventStatusTransitionException extends ConflictException {

    /**
     * Создаёт исключение для некорректного перехода статуса.
     *
     * @param currentStatus текущий статус события
     * @param targetStatus  целевой статус
     */
    public InvalidEventStatusTransitionException(EventStatus currentStatus, EventStatus targetStatus) {
        super(
            "invalid_state_transition",
            "Невозможно перевести событие из состояния " + currentStatus.name() + " в " + targetStatus.name(),
            Map.of("currentStatus", currentStatus.name(), "targetStatus", targetStatus.name())
        );
    }

    /**
     * Создаёт исключение с кастомным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public InvalidEventStatusTransitionException(String message) {
        super("invalid_event_status", message);
    }
}
