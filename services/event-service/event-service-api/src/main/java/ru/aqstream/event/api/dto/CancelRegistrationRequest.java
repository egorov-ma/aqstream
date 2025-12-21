package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Запрос на отмену регистрации организатором.
 *
 * @param reason причина отмены (опционально)
 */
public record CancelRegistrationRequest(
    @Size(max = 1000, message = "Причина отмены не должна превышать 1000 символов")
    String reason
) {
}
