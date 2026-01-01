package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO публичного события для списка на главной странице.
 * Содержит только поля, необходимые для карточки события.
 *
 * @param id              идентификатор события
 * @param title           название события
 * @param slug            URL-slug события
 * @param description     краткое описание (обрезанное до 150 символов)
 * @param startsAt        дата и время начала
 * @param timezone        часовой пояс
 * @param locationType    тип локации (ONLINE, OFFLINE, HYBRID)
 * @param locationAddress физический адрес (для OFFLINE/HYBRID)
 * @param coverImageUrl   URL обложки события
 * @param organizerName   название организации
 */
public record PublicEventSummaryDto(
    UUID id,
    String title,
    String slug,
    String description,
    Instant startsAt,
    String timezone,
    LocationType locationType,
    String locationAddress,
    String coverImageUrl,
    String organizerName
) {}
