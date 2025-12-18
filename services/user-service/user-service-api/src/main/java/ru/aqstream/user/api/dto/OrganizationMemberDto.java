package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * DTO члена организации.
 *
 * @param id            идентификатор записи членства
 * @param userId        идентификатор пользователя
 * @param userName      имя пользователя
 * @param userEmail     email пользователя
 * @param userAvatarUrl URL аватара пользователя
 * @param role          роль в организации
 * @param invitedById   идентификатор пригласившего
 * @param invitedByName имя пригласившего
 * @param joinedAt      время присоединения
 * @param createdAt     время создания записи
 */
@Builder
public record OrganizationMemberDto(
    UUID id,
    UUID userId,
    String userName,
    String userEmail,
    String userAvatarUrl,
    OrganizationRole role,
    UUID invitedById,
    String invitedByName,
    Instant joinedAt,
    Instant createdAt
) {
}
