package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение при отсутствии участника группы.
 * Преобразуется в HTTP 404 Not Found.
 */
public class GroupMemberNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для отсутствующего участника.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     */
    public GroupMemberNotFoundException(UUID groupId, UUID userId) {
        super(
            "group_member_not_found",
            "Участник группы не найден",
            Map.of(
                "groupId", groupId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
