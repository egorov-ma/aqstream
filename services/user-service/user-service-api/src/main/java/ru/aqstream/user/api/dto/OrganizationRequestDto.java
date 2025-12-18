package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * DTO запроса на создание организации.
 *
 * @param id            идентификатор запроса
 * @param userId        идентификатор пользователя-заявителя
 * @param userName      имя пользователя-заявителя
 * @param name          название организации
 * @param slug          URL-slug
 * @param description   описание
 * @param status        статус запроса
 * @param reviewedById  идентификатор админа-ревьюера
 * @param reviewComment комментарий админа
 * @param reviewedAt    время рассмотрения
 * @param createdAt     время создания
 */
@Builder
public record OrganizationRequestDto(
    UUID id,
    UUID userId,
    String userName,
    String name,
    String slug,
    String description,
    OrganizationRequestStatus status,
    UUID reviewedById,
    String reviewComment,
    Instant reviewedAt,
    Instant createdAt
) {
}
