package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение для случаев, когда регистрация не найдена.
 * Преобразуется в HTTP 404 Not Found.
 */
public class RegistrationNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для ненайденной регистрации.
     *
     * @param registrationId идентификатор регистрации
     */
    public RegistrationNotFoundException(UUID registrationId) {
        super(
            "registration_not_found",
            "Регистрация не найдена",
            Map.of("registrationId", registrationId.toString())
        );
    }

    /**
     * Создаёт исключение для ненайденной регистрации с tenantId.
     *
     * @param registrationId идентификатор регистрации
     * @param tenantId       идентификатор организации
     */
    public RegistrationNotFoundException(UUID registrationId, UUID tenantId) {
        super(
            "registration_not_found",
            "Регистрация не найдена",
            Map.of(
                "registrationId", registrationId.toString(),
                "tenantId", tenantId.toString()
            )
        );
    }
}
