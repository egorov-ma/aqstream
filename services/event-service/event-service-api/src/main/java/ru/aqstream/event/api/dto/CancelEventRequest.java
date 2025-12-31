package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Запрос на отмену события.
 *
 * @param reason причина отмены (опционально, до 1000 символов)
 */
public record CancelEventRequest(
    @Size(max = 1000, message = "Причина отмены не должна превышать 1000 символов")
    String reason
) {
}
