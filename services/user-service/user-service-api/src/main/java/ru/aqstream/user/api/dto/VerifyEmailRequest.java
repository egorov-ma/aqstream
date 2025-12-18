package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на подтверждение email.
 *
 * @param token токен подтверждения из письма
 */
public record VerifyEmailRequest(
    @NotBlank(message = "Токен обязателен")
    @Size(min = 32, max = 64, message = "Некорректный формат токена")
    String token
) {
}
