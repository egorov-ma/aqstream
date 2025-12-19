package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при попытке добавить пользователя, уже являющегося участником группы.
 * Преобразуется в HTTP 409 Conflict.
 */
public class AlreadyGroupMemberException extends ConflictException {

    /**
     * Создаёт исключение для уже существующего участника.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     */
    public AlreadyGroupMemberException(UUID groupId, UUID userId) {
        super(
            "already_group_member",
            "Пользователь уже является участником группы",
            Map.of(
                "groupId", groupId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
