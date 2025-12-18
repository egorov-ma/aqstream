package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при попытке добавить пользователя, уже являющегося членом организации.
 */
public class AlreadyOrganizationMemberException extends ConflictException {

    /**
     * Создаёт исключение для уже существующего члена.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     */
    public AlreadyOrganizationMemberException(UUID organizationId, UUID userId) {
        super(
            "already_organization_member",
            "Пользователь уже является членом организации",
            Map.of(
                "organizationId", organizationId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
