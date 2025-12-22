package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ForbiddenException;

/**
 * Исключение для случаев, когда пользователь пытается зарегистрироваться
 * на приватное событие, не являясь членом группы.
 * Преобразуется в HTTP 403 Forbidden.
 */
public class PrivateEventAccessDeniedException extends ForbiddenException {

    /**
     * Создаёт исключение для отказа в доступе к приватному событию.
     *
     * @param eventId идентификатор события
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     */
    public PrivateEventAccessDeniedException(UUID eventId, UUID groupId, UUID userId) {
        super(
            "private_event_access_denied",
            "Это приватное событие доступно только участникам группы",
            Map.of(
                "eventId", eventId.toString(),
                "groupId", groupId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
