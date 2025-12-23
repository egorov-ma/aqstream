package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос привязки Telegram к аккаунту через токен.
 * Используется ботом при обработке /start link_{token}.
 *
 * @param linkToken      токен привязки
 * @param telegramId     Telegram ID пользователя
 * @param telegramChatId Telegram Chat ID для уведомлений
 */
public record LinkTelegramByTokenRequest(
    @NotBlank(message = "Токен привязки обязателен")
    String linkToken,

    @NotNull(message = "Telegram ID обязателен")
    Long telegramId,

    @NotNull(message = "Telegram Chat ID обязателен")
    Long telegramChatId
) {
}
