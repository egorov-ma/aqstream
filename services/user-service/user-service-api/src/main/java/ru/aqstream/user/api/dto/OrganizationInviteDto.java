package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * DTO приглашения в организацию.
 *
 * @param id               идентификатор приглашения
 * @param organizationId   идентификатор организации
 * @param organizationName название организации
 * @param inviteCode       код приглашения
 * @param telegramDeeplink Telegram deeplink для приглашения
 * @param invitedById      идентификатор пригласившего
 * @param invitedByName    имя пригласившего
 * @param telegramUsername Telegram username приглашённого
 * @param role             роль приглашённого
 * @param expiresAt        когда истекает
 * @param usedAt           когда использовано (null если не использовано)
 * @param usedById         идентификатор использовавшего
 * @param createdAt        время создания
 */
@Builder
public record OrganizationInviteDto(
    UUID id,
    UUID organizationId,
    String organizationName,
    String inviteCode,
    String telegramDeeplink,
    UUID invitedById,
    String invitedByName,
    String telegramUsername,
    OrganizationRole role,
    Instant expiresAt,
    Instant usedAt,
    UUID usedById,
    Instant createdAt
) {
}
