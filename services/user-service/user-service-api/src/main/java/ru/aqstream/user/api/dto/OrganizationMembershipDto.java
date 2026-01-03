package ru.aqstream.user.api.dto;

import java.util.UUID;

/**
 * DTO для проверки членства пользователя в организации.
 * Используется для internal API между сервисами.
 *
 * @param organizationId идентификатор организации
 * @param userId         идентификатор пользователя
 * @param role           роль в организации (OWNER/MODERATOR)
 * @param isMember       true если пользователь является членом организации
 */
public record OrganizationMembershipDto(
    UUID organizationId,
    UUID userId,
    OrganizationRole role,
    boolean isMember
) {

    /**
     * Создаёт DTO для члена организации.
     */
    public static OrganizationMembershipDto member(UUID organizationId, UUID userId, OrganizationRole role) {
        return new OrganizationMembershipDto(organizationId, userId, role, true);
    }

    /**
     * Создаёт DTO для пользователя, который не является членом организации.
     */
    public static OrganizationMembershipDto notMember(UUID organizationId, UUID userId) {
        return new OrganizationMembershipDto(organizationId, userId, null, false);
    }
}
