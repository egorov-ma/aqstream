package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * DTO группы.
 *
 * @param id               идентификатор группы
 * @param organizationId   идентификатор организации
 * @param organizationName название организации
 * @param name             название группы
 * @param description      описание группы
 * @param inviteCode       код приглашения (8 символов)
 * @param createdById      идентификатор создателя
 * @param createdByName    имя создателя
 * @param memberCount      количество участников
 * @param createdAt        время создания
 * @param updatedAt        время последнего обновления
 */
@Builder
public record GroupDto(
    UUID id,
    UUID organizationId,
    String organizationName,
    String name,
    String description,
    String inviteCode,
    UUID createdById,
    String createdByName,
    int memberCount,
    Instant createdAt,
    Instant updatedAt
) {
}
