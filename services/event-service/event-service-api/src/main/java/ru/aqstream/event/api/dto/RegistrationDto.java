package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO регистрации для API ответов.
 *
 * @param id               идентификатор регистрации
 * @param eventId          идентификатор события
 * @param eventTitle       название события
 * @param eventSlug        slug события
 * @param eventStartsAt    дата начала события
 * @param ticketTypeId     идентификатор типа билета
 * @param ticketTypeName   название типа билета
 * @param userId           идентификатор пользователя
 * @param status           статус регистрации
 * @param confirmationCode код подтверждения
 * @param firstName        имя участника
 * @param lastName         фамилия участника
 * @param email            email участника
 * @param customFields     дополнительные поля формы
 * @param cancelledAt      дата отмены
 * @param cancellationReason причина отмены
 * @param createdAt        дата создания
 */
public record RegistrationDto(
    UUID id,
    UUID eventId,
    String eventTitle,
    String eventSlug,
    Instant eventStartsAt,
    UUID ticketTypeId,
    String ticketTypeName,
    UUID userId,
    RegistrationStatus status,
    String confirmationCode,
    String firstName,
    String lastName,
    String email,
    Map<String, Object> customFields,
    Instant cancelledAt,
    String cancellationReason,
    Instant createdAt
) {
}
