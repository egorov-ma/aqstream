package ru.aqstream.event.api.exception;

import ru.aqstream.common.api.exception.ValidationException;

/**
 * Исключение при попытке опубликовать событие с датой в прошлом.
 * Преобразуется в HTTP 400 Bad Request.
 */
public class EventInPastException extends ValidationException {

    /**
     * Создаёт исключение с стандартным сообщением.
     */
    public EventInPastException() {
        super("Нельзя опубликовать событие с датой в прошлом");
    }
}
