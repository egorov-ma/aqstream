package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.InternalServerException;

/**
 * Исключение при ошибке генерации изображения билета.
 * Преобразуется в HTTP 500 Internal Server Error.
 */
public class TicketGenerationException extends InternalServerException {

    /**
     * Создаёт исключение при ошибке генерации билета.
     *
     * @param registrationId идентификатор регистрации
     * @param cause          причина ошибки
     */
    public TicketGenerationException(UUID registrationId, Throwable cause) {
        super(
            "ticket_generation_error",
            "Ошибка генерации билета",
            cause
        );
    }

    /**
     * Создаёт исключение при ошибке генерации билета с деталями.
     *
     * @param registrationId идентификатор регистрации
     * @param reason         причина ошибки
     */
    public TicketGenerationException(UUID registrationId, String reason) {
        super(
            "ticket_generation_error",
            "Ошибка генерации билета: " + reason,
            Map.of("registrationId", registrationId.toString())
        );
    }
}
