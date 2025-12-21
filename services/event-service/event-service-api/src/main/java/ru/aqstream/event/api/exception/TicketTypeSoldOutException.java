package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение для случаев, когда билеты распроданы.
 * Преобразуется в HTTP 409 Conflict.
 */
public class TicketTypeSoldOutException extends ConflictException {

    /**
     * Создаёт исключение для распроданного типа билета.
     *
     * @param ticketTypeId идентификатор типа билета
     */
    public TicketTypeSoldOutException(UUID ticketTypeId) {
        super(
            "ticket_type_sold_out",
            "Билеты данного типа распроданы",
            Map.of("ticketTypeId", ticketTypeId.toString())
        );
    }
}
