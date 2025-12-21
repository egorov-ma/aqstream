package ru.aqstream.event.api.exception;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение для случаев, когда продажи типа билета не открыты.
 * Преобразуется в HTTP 409 Conflict.
 */
public class TicketTypeSalesNotOpenException extends ConflictException {

    /**
     * Создаёт исключение для типа билета вне периода продаж.
     *
     * @param ticketTypeId идентификатор типа билета
     * @param salesStart   начало продаж
     * @param salesEnd     окончание продаж
     */
    public TicketTypeSalesNotOpenException(UUID ticketTypeId, Instant salesStart, Instant salesEnd) {
        super(
            "ticket_type_sales_not_open",
            "Продажи билетов закрыты",
            Map.of(
                "ticketTypeId", ticketTypeId.toString(),
                "salesStart", salesStart != null ? salesStart.toString() : "не указано",
                "salesEnd", salesEnd != null ? salesEnd.toString() : "не указано"
            )
        );
    }
}
