package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ForbiddenException;

/**
 * Исключение при попытке доступа к организации пользователем, не являющимся её членом.
 *
 * <p>Возвращает HTTP 403 Forbidden, так как организация существует,
 * но у пользователя нет доступа к ней.</p>
 */
public class OrganizationMemberNotFoundException extends ForbiddenException {

    /**
     * Создаёт исключение для ненайденного членства.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     */
    public OrganizationMemberNotFoundException(UUID organizationId, UUID userId) {
        super(
            "not_organization_member",
            "Пользователь не является членом организации",
            Map.of(
                "organizationId", organizationId.toString(),
                "userId", userId.toString()
            )
        );
    }
}
