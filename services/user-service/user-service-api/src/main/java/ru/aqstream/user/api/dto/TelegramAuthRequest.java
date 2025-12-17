package ru.aqstream.user.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Запрос на аутентификацию через Telegram Login Widget.
 *
 * <p>Данные приходят от Telegram Login Widget и должны быть провалидированы
 * через проверку hash согласно https://core.telegram.org/widgets/login</p>
 *
 * @param id        Telegram ID пользователя
 * @param firstName имя пользователя из Telegram
 * @param lastName  фамилия пользователя (опционально)
 * @param username  username в Telegram (опционально)
 * @param photoUrl  URL фото профиля (опционально)
 * @param authDate  Unix timestamp времени авторизации
 * @param hash      HMAC-SHA256 хеш для валидации
 */
public record TelegramAuthRequest(
    @NotNull(message = "Telegram ID обязателен")
    @Positive(message = "Telegram ID должен быть положительным числом")
    Long id,

    @NotBlank(message = "Имя обязательно")
    @Size(min = 1, max = 100, message = "Имя должно содержать от 1 до 100 символов")
    @JsonProperty("first_name")
    String firstName,

    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    @JsonProperty("last_name")
    String lastName,

    @Size(max = 100, message = "Username не должен превышать 100 символов")
    String username,

    @Size(max = 500, message = "URL фото не должен превышать 500 символов")
    @JsonProperty("photo_url")
    String photoUrl,

    @NotNull(message = "Время авторизации обязательно")
    @Positive(message = "Время авторизации должно быть положительным числом")
    @JsonProperty("auth_date")
    Long authDate,

    @NotBlank(message = "Hash обязателен для валидации")
    @Size(min = 64, max = 64, message = "Hash должен содержать ровно 64 символа")
    String hash
) {
}
