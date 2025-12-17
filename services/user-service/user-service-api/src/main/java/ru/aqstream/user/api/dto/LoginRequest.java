package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на вход по email и паролю.
 *
 * @param email    email пользователя
 * @param password пароль
 */
public record LoginRequest(
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    String email,

    @NotBlank(message = "Пароль обязателен")
    String password
) {
}
