package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение при ненайденном члене организации.
 */
public class OrganizationMemberNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для ненайденного членства.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     */
    public OrganizationMemberNotFoundException(UUID organizationId, UUID userId) {
        super(
            "organization_member_not_found",
            "Пользователь не является членом организации",
            Map.of(
                "organizationId", organizationId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
