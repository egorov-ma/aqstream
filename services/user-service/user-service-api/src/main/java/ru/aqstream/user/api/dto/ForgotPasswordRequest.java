package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на сброс пароля.
 *
 * @param email email пользователя
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    String email
) {
}
