package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO события для API ответов.
 *
 * @param id                     идентификатор события
 * @param tenantId               идентификатор организации
 * @param title                  название события
 * @param slug                   URL-slug события
 * @param description            описание в формате Markdown
 * @param status                 статус события
 * @param startsAt               дата и время начала
 * @param endsAt                 дата и время окончания
 * @param timezone               часовой пояс
 * @param locationType           тип локации
 * @param locationAddress        физический адрес
 * @param onlineUrl              ссылка на онлайн-площадку
 * @param maxCapacity            максимальное количество участников
 * @param registrationOpensAt    дата открытия регистрации
 * @param registrationClosesAt   дата закрытия регистрации
 * @param isPublic               публичность события
 * @param participantsVisibility видимость списка участников
 * @param groupId                ID группы для приватных событий
 * @param createdAt              дата создания
 * @param updatedAt              дата обновления
 */
public record EventDto(
    UUID id,
    UUID tenantId,
    String title,
    String slug,
    String description,
    EventStatus status,
    Instant startsAt,
    Instant endsAt,
    String timezone,
    LocationType locationType,
    String locationAddress,
    String onlineUrl,
    Integer maxCapacity,
    Instant registrationOpensAt,
    Instant registrationClosesAt,
    boolean isPublic,
    ParticipantsVisibility participantsVisibility,
    UUID groupId,
    Instant createdAt,
    Instant updatedAt
) {
}
