package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение при отсутствии группы.
 * Преобразуется в HTTP 404 Not Found.
 */
public class GroupNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для группы по идентификатору.
     *
     * @param groupId идентификатор группы
     */
    public GroupNotFoundException(UUID groupId) {
        super(
            "group_not_found",
            "Группа не найдена",
            Map.of("groupId", groupId.toString())
        );
    }

    /**
     * Создаёт исключение для группы по инвайт-коду.
     *
     * @param inviteCode код приглашения
     */
    public GroupNotFoundException(String inviteCode) {
        super(
            "group_not_found",
            "Группа с таким кодом не найдена",
            Map.of("inviteCode", inviteCode)
        );
    }
}
