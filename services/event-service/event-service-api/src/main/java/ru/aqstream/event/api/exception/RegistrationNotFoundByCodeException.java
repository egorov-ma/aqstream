package ru.aqstream.event.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение для случаев, когда регистрация не найдена по confirmation code.
 * Преобразуется в HTTP 404 Not Found.
 */
public class RegistrationNotFoundByCodeException extends EntityNotFoundException {

    /**
     * Создаёт исключение для ненайденной регистрации по коду.
     *
     * @param confirmationCode код подтверждения
     */
    public RegistrationNotFoundByCodeException(String confirmationCode) {
        super(
            "registration_not_found",
            "Регистрация не найдена по указанному коду",
            Map.of("confirmationCode", confirmationCode)
        );
    }
}
