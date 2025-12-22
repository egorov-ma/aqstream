package ru.aqstream.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * Запрос на отправку уведомления с изображением (например, билет с QR-кодом).
 *
 * @param userId       ID пользователя-получателя
 * @param templateCode код шаблона для подписи к изображению
 * @param variables    переменные для подстановки в шаблон
 * @param image        байты изображения (PNG/JPEG)
 */
@Builder
public record SendNotificationWithImageRequest(
        @NotNull(message = "userId обязателен")
        UUID userId,

        @NotBlank(message = "templateCode обязателен")
        String templateCode,

        Map<String, Object> variables,

        @NotNull(message = "image обязателен")
        byte[] image
) {
    /**
     * Создаёт запрос с пустыми переменными если не указаны.
     */
    public SendNotificationWithImageRequest {
        if (variables == null) {
            variables = Map.of();
        }
    }
}
