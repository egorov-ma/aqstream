package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;
import ru.aqstream.event.api.dto.RegistrationStatus;

/**
 * Исключение для случаев, когда check-in невозможен.
 * Преобразуется в HTTP 409 Conflict.
 */
public class CheckInNotAllowedException extends ConflictException {

    /**
     * Создаёт исключение для невозможного check-in из-за статуса регистрации.
     *
     * @param registrationId   идентификатор регистрации
     * @param confirmationCode код подтверждения
     * @param status           текущий статус регистрации
     */
    public CheckInNotAllowedException(UUID registrationId, String confirmationCode, RegistrationStatus status) {
        super(
            "check_in_not_allowed",
            "Check-in невозможен для регистрации в текущем статусе",
            Map.of(
                "registrationId", registrationId.toString(),
                "confirmationCode", confirmationCode,
                "status", status.name()
            )
        );
    }

    /**
     * Создаёт исключение для невозможного check-in с кастомным сообщением.
     *
     * @param registrationId   идентификатор регистрации
     * @param confirmationCode код подтверждения
     * @param reason           причина
     */
    public CheckInNotAllowedException(UUID registrationId, String confirmationCode, String reason) {
        super(
            "check_in_not_allowed",
            reason,
            Map.of(
                "registrationId", registrationId.toString(),
                "confirmationCode", confirmationCode
            )
        );
    }
}
