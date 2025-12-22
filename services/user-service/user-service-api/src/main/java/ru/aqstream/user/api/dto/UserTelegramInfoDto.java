package ru.aqstream.user.api.dto;

import java.util.UUID;

/**
 * DTO с Telegram информацией пользователя.
 * Используется для внутренних вызовов между сервисами.
 *
 * @param userId         идентификатор пользователя
 * @param telegramChatId Telegram Chat ID для отправки уведомлений (может быть null)
 * @param firstName      имя пользователя
 * @param lastName       фамилия пользователя
 */
public record UserTelegramInfoDto(
        UUID userId,
        String telegramChatId,
        String firstName,
        String lastName
) {
    /**
     * Проверяет, привязан ли Telegram к аккаунту.
     */
    public boolean hasTelegram() {
        return telegramChatId != null && !telegramChatId.isBlank();
    }
}
