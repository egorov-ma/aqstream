package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на установку нового пароля.
 *
 * @param token       токен сброса пароля из письма
 * @param newPassword новый пароль
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Токен обязателен")
    @Size(min = 32, max = 64, message = "Некорректный формат токена")
    String token,

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
    String newPassword
) {
}
