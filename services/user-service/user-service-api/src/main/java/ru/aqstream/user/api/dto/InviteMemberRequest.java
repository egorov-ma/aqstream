package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Запрос на приглашение нового члена в организацию.
 *
 * @param telegramUsername Telegram username приглашаемого (опционально, для отображения)
 */
public record InviteMemberRequest(

    @Size(max = 100, message = "Telegram username не должен превышать 100 символов")
    String telegramUsername
) {
}
