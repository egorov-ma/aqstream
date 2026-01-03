package ru.aqstream.event.api.exception;

import ru.aqstream.common.api.exception.ValidationException;

/**
 * Исключение при попытке опубликовать событие без типов билетов.
 * Преобразуется в HTTP 400 Bad Request.
 */
public class EventHasNoTicketTypesException extends ValidationException {

    private static final String CODE = "event_has_no_ticket_types";
    private static final String MESSAGE = "Для публикации события необходимо добавить хотя бы один тип билета";

    /**
     * Создаёт исключение для события без типов билетов.
     */
    public EventHasNoTicketTypesException() {
        super(CODE, MESSAGE);
    }
}
