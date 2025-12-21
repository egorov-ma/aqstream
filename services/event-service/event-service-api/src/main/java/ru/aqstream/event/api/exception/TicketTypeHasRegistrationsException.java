package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение для попытки удаления типа билета с регистрациями.
 * Преобразуется в HTTP 409 Conflict.
 */
public class TicketTypeHasRegistrationsException extends ConflictException {

    /**
     * Создаёт исключение для типа билета с регистрациями.
     *
     * @param ticketTypeId      идентификатор типа билета
     * @param registrationCount количество регистраций
     */
    public TicketTypeHasRegistrationsException(UUID ticketTypeId, int registrationCount) {
        super(
            "ticket_type_has_registrations",
            "Нельзя удалить тип билета с регистрациями. Используйте деактивацию",
            Map.of(
                "ticketTypeId", ticketTypeId.toString(),
                "registrationCount", String.valueOf(registrationCount)
            )
        );
    }
}
