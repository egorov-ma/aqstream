package ru.aqstream.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.db.repository.NotificationPreferenceRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Сервис для работы с настройками уведомлений пользователей.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Проверяет, разрешено ли отправлять уведомление данного типа пользователю.
     *
     * @param userId     ID пользователя
     * @param settingKey ключ настройки (например, "event_reminder")
     * @return true если уведомления разрешены
     */
    public boolean isNotificationEnabled(UUID userId, String settingKey) {
        return preferenceRepository.findByUserId(userId)
            .map(pref -> pref.isEnabled(settingKey))
            .orElse(true); // По умолчанию включено
    }

    /**
     * Получает настройки пользователя.
     * Если настройки не существуют, возвращает настройки по умолчанию.
     *
     * @param userId ID пользователя
     * @return настройки
     */
    public NotificationPreference getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
            .orElseGet(() -> NotificationPreference.createDefault(userId));
    }

    /**
     * Получает или создаёт настройки пользователя.
     *
     * @param userId ID пользователя
     * @return сохранённые настройки
     */
    @Transactional
    public NotificationPreference getOrCreatePreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
            .orElseGet(() -> {
                NotificationPreference pref = NotificationPreference.createDefault(userId);
                return preferenceRepository.save(pref);
            });
    }

    /**
     * Обновляет настройки пользователя.
     *
     * @param userId   ID пользователя
     * @param settings новые настройки
     * @return обновлённые настройки
     */
    @Transactional
    public NotificationPreference updatePreferences(UUID userId, Map<String, Boolean> settings) {
        NotificationPreference pref = getOrCreatePreferences(userId);
        pref.updateSettings(settings);
        NotificationPreference saved = preferenceRepository.save(pref);
        log.info("Настройки уведомлений обновлены: userId={}", userId);
        return saved;
    }

    /**
     * Устанавливает конкретную настройку.
     *
     * @param userId     ID пользователя
     * @param settingKey ключ настройки
     * @param enabled    включена/выключена
     */
    @Transactional
    public void setSetting(UUID userId, String settingKey, boolean enabled) {
        NotificationPreference pref = getOrCreatePreferences(userId);
        pref.setSetting(settingKey, enabled);
        preferenceRepository.save(pref);
        log.info("Настройка уведомления обновлена: userId={}, key={}, enabled={}", userId, settingKey, enabled);
    }
}
