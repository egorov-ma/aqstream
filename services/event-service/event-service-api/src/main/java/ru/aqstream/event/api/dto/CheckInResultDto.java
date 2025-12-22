package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO с результатом check-in операции.
 *
 * @param registrationId   идентификатор регистрации
 * @param confirmationCode код подтверждения
 * @param eventTitle       название события
 * @param ticketTypeName   название типа билета
 * @param firstName        имя участника
 * @param lastName         фамилия участника
 * @param checkedInAt      время check-in
 * @param message          сообщение о результате
 */
public record CheckInResultDto(
    UUID registrationId,
    String confirmationCode,
    String eventTitle,
    String ticketTypeName,
    String firstName,
    String lastName,
    Instant checkedInAt,
    String message
) {

    /**
     * Создаёт результат успешного check-in.
     */
    public static CheckInResultDto success(
            UUID registrationId,
            String confirmationCode,
            String eventTitle,
            String ticketTypeName,
            String firstName,
            String lastName,
            Instant checkedInAt
    ) {
        return new CheckInResultDto(
            registrationId,
            confirmationCode,
            eventTitle,
            ticketTypeName,
            firstName,
            lastName,
            checkedInAt,
            "Check-in выполнен успешно"
        );
    }
}
