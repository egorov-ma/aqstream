package ru.aqstream.notification.api.dto;

import java.util.Map;

/**
 * DTO настроек уведомлений пользователя.
 *
 * @param settings настройки в формате {"setting_key": enabled}
 */
public record NotificationPreferencesDto(
    Map<String, Boolean> settings
) {
    /**
     * Проверяет, включена ли настройка.
     *
     * @param key ключ настройки
     * @return true если включена (по умолчанию true)
     */
    public boolean isEnabled(String key) {
        return settings == null || settings.getOrDefault(key, true);
    }
}
