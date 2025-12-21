package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение для случаев, когда пользователь уже зарегистрирован на событие.
 * Преобразуется в HTTP 409 Conflict.
 */
public class RegistrationAlreadyExistsException extends ConflictException {

    /**
     * Создаёт исключение для повторной регистрации.
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     */
    public RegistrationAlreadyExistsException(UUID eventId, UUID userId) {
        super(
            "registration_already_exists",
            "Вы уже зарегистрированы на это событие",
            Map.of(
                "eventId", eventId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
