package ru.aqstream.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * Запрос на отправку уведомления.
 *
 * @param userId         ID пользователя-получателя
 * @param templateCode   код шаблона уведомления
 * @param variables      переменные для подстановки в шаблон
 * @param channel        канал отправки (по умолчанию TELEGRAM)
 */
@Builder
public record SendNotificationRequest(
        @NotNull(message = "userId обязателен")
        UUID userId,

        @NotBlank(message = "templateCode обязателен")
        String templateCode,

        Map<String, Object> variables,

        NotificationChannel channel
) {
    /**
     * Создаёт запрос с каналом по умолчанию (TELEGRAM).
     */
    public SendNotificationRequest {
        if (channel == null) {
            channel = NotificationChannel.TELEGRAM;
        }
        if (variables == null) {
            variables = Map.of();
        }
    }
}
