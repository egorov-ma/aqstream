package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * DTO участника группы.
 *
 * @param id            идентификатор записи участия
 * @param groupId       идентификатор группы
 * @param userId        идентификатор пользователя
 * @param userName      имя пользователя
 * @param userEmail     email пользователя
 * @param userAvatarUrl URL аватара пользователя
 * @param invitedById   идентификатор пригласившего (null для создателя)
 * @param invitedByName имя пригласившего
 * @param joinedAt      время присоединения
 * @param createdAt     время создания записи
 */
@Builder
public record GroupMemberDto(
    UUID id,
    UUID groupId,
    UUID userId,
    String userName,
    String userEmail,
    String userAvatarUrl,
    UUID invitedById,
    String invitedByName,
    Instant joinedAt,
    Instant createdAt
) {
}
