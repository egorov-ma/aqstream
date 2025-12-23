package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Запрос принятия приглашения в организацию через Telegram deeplink.
 *
 * @param userId     идентификатор пользователя
 * @param inviteCode код приглашения из deeplink
 */
public record AcceptInviteByTelegramRequest(
    @NotNull(message = "userId обязателен")
    UUID userId,

    @NotBlank(message = "Код приглашения обязателен")
    String inviteCode
) {
}
