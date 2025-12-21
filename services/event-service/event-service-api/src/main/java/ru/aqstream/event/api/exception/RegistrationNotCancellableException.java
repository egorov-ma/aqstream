package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;
import ru.aqstream.event.api.dto.RegistrationStatus;

/**
 * Исключение для случаев, когда регистрация не может быть отменена.
 * Преобразуется в HTTP 409 Conflict.
 */
public class RegistrationNotCancellableException extends ConflictException {

    /**
     * Создаёт исключение для регистрации, которую нельзя отменить.
     *
     * @param registrationId идентификатор регистрации
     * @param status         текущий статус регистрации
     */
    public RegistrationNotCancellableException(UUID registrationId, RegistrationStatus status) {
        super(
            "registration_not_cancellable",
            "Регистрация не может быть отменена в текущем статусе",
            Map.of(
                "registrationId", registrationId.toString(),
                "currentStatus", status.name()
            )
        );
    }
}
