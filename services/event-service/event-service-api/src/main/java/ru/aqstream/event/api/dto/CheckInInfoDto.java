package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO с информацией о регистрации для check-in.
 * Используется для отображения данных перед подтверждением check-in.
 *
 * @param registrationId   идентификатор регистрации
 * @param confirmationCode код подтверждения
 * @param eventId          идентификатор события
 * @param eventTitle       название события
 * @param eventStartsAt    дата начала события
 * @param ticketTypeName   название типа билета
 * @param firstName        имя участника
 * @param lastName         фамилия участника
 * @param email            email участника
 * @param status           статус регистрации
 * @param isCheckedIn      прошёл ли участник check-in
 * @param checkedInAt      время check-in (null если не прошёл)
 */
public record CheckInInfoDto(
    UUID registrationId,
    String confirmationCode,
    UUID eventId,
    String eventTitle,
    Instant eventStartsAt,
    String ticketTypeName,
    String firstName,
    String lastName,
    String email,
    RegistrationStatus status,
    boolean isCheckedIn,
    Instant checkedInAt
) {
}
