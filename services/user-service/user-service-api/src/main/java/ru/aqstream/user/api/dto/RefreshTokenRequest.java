package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на обновление токенов.
 *
 * @param refreshToken текущий refresh token
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token обязателен")
    String refreshToken
) {
}
