package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Запрос на изменение роли члена организации.
 *
 * @param role новая роль (OWNER или MODERATOR)
 */
public record UpdateMemberRoleRequest(

    @NotNull(message = "Роль обязательна")
    OrganizationRole role
) {
}
