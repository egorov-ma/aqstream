package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * DTO организации.
 *
 * @param id          идентификатор организации
 * @param ownerId     идентификатор владельца
 * @param ownerName   имя владельца
 * @param name        название организации
 * @param slug        URL-slug
 * @param description описание
 * @param logoUrl     URL логотипа
 * @param createdAt   время создания
 * @param updatedAt   время обновления
 */
@Builder
public record OrganizationDto(
    UUID id,
    UUID ownerId,
    String ownerName,
    String name,
    String slug,
    String description,
    String logoUrl,
    Instant createdAt,
    Instant updatedAt
) {
}
