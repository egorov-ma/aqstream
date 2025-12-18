package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на повторную отправку письма подтверждения email.
 *
 * @param email email пользователя
 */
public record ResendVerificationRequest(
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    String email
) {
}
