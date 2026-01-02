package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос подтверждения авторизации через Telegram бота.
 * Используется ботом при нажатии кнопки "Подтвердить вход".
 *
 * @param token      токен авторизации (из deeplink auth_{token})
 * @param telegramId Telegram ID пользователя
 * @param firstName  имя пользователя в Telegram
 * @param lastName   фамилия (может быть null)
 * @param username   username в Telegram (может быть null)
 * @param chatId     Chat ID для отправки сообщений
 */
public record ConfirmTelegramAuthRequest(
    @NotBlank(message = "Токен авторизации обязателен")
    String token,

    @NotNull(message = "Telegram ID обязателен")
    Long telegramId,

    @NotBlank(message = "Имя обязательно")
    String firstName,

    String lastName,

    String username,

    @NotNull(message = "Chat ID обязателен")
    Long chatId
) {}
