package ru.aqstream.notification.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Запрос на обновление настроек уведомлений.
 *
 * @param settings настройки в формате {"setting_key": enabled}
 */
public record UpdatePreferencesRequest(
    @NotNull(message = "Настройки обязательны")
    Map<String, Boolean> settings
) {
}
