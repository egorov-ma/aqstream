package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение для случаев, когда тип билета не найден.
 * Преобразуется в HTTP 404 Not Found.
 */
public class TicketTypeNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для ненайденного типа билета по ID.
     *
     * @param ticketTypeId идентификатор типа билета
     */
    public TicketTypeNotFoundException(UUID ticketTypeId) {
        super(
            "ticket_type_not_found",
            "Тип билета не найден",
            Map.of("ticketTypeId", ticketTypeId.toString())
        );
    }

    /**
     * Создаёт исключение для ненайденного типа билета в событии.
     *
     * @param ticketTypeId идентификатор типа билета
     * @param eventId      идентификатор события
     */
    public TicketTypeNotFoundException(UUID ticketTypeId, UUID eventId) {
        super(
            "ticket_type_not_found",
            "Тип билета не найден",
            Map.of(
                "ticketTypeId", ticketTypeId.toString(),
                "eventId", eventId.toString()
            )
        );
    }
}
