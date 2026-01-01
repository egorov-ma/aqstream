package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на смену пароля пользователя.
 *
 * <p>Требует текущий пароль для подтверждения.
 * Бизнес-валидация нового пароля (буквы + цифры) выполняется в PasswordService.</p>
 *
 * @param currentPassword текущий пароль для подтверждения
 * @param newPassword     новый пароль (минимум 8 символов)
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Текущий пароль обязателен")
    String currentPassword,

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 8, max = 100, message = "Новый пароль должен содержать от 8 до 100 символов")
    String newPassword
) {
}
