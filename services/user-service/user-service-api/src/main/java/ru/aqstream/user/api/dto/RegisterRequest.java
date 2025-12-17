package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на регистрацию пользователя по email.
 *
 * <p>Базовая валидация (синтаксис) выполняется аннотациями.
 * Бизнес-валидация пароля (буквы + цифры) выполняется в PasswordService.</p>
 *
 * @param email     email пользователя (обязателен, уникален)
 * @param password  пароль (минимум 8 символов)
 * @param firstName имя пользователя
 * @param lastName  фамилия пользователя (опционально)
 */
public record RegisterRequest(
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не должен превышать 255 символов")
    String email,

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
    String password,

    @NotBlank(message = "Имя обязательно")
    @Size(min = 1, max = 100, message = "Имя должно содержать от 1 до 100 символов")
    String firstName,

    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    String lastName
) {
}
