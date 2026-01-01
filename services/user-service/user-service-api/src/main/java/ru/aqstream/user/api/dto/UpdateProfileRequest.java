package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на обновление профиля пользователя.
 *
 * <p>Email нельзя изменить через этот endpoint.</p>
 *
 * @param firstName имя пользователя (обязательно)
 * @param lastName  фамилия пользователя (опционально)
 */
public record UpdateProfileRequest(
    @NotBlank(message = "Имя обязательно")
    @Size(min = 1, max = 100, message = "Имя должно содержать от 1 до 100 символов")
    String firstName,

    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    String lastName
) {
}
