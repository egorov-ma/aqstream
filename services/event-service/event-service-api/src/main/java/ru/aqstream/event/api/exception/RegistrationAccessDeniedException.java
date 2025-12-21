package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ForbiddenException;

/**
 * Исключение для случаев, когда пользователь не имеет доступа к регистрации.
 * Преобразуется в HTTP 403 Forbidden.
 */
public class RegistrationAccessDeniedException extends ForbiddenException {

    /**
     * Создаёт исключение для отказа в доступе к регистрации.
     *
     * @param registrationId идентификатор регистрации
     * @param userId         идентификатор пользователя
     */
    public RegistrationAccessDeniedException(UUID registrationId, UUID userId) {
        super(
            "registration_access_denied",
            "У вас нет доступа к этой регистрации",
            Map.of(
                "registrationId", registrationId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
