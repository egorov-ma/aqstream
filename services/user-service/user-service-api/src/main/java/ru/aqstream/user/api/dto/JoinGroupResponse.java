package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * Ответ при присоединении к группе по инвайт-коду.
 *
 * @param groupId          идентификатор группы
 * @param groupName        название группы
 * @param organizationId   идентификатор организации
 * @param organizationName название организации
 * @param joinedAt         время присоединения
 */
@Builder
public record JoinGroupResponse(
    UUID groupId,
    String groupName,
    UUID organizationId,
    String organizationName,
    Instant joinedAt
) {
}
