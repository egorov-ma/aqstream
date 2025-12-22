package ru.aqstream.notification.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.aqstream.common.data.BaseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Настройки уведомлений пользователя.
 *
 * <p>Хранит предпочтения пользователя по типам уведомлений.
 * По умолчанию все уведомления включены.</p>
 */
@Entity
@Table(name = "notification_preferences", schema = "notification_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference extends BaseEntity {

    // === Константы настроек ===

    /**
     * Ключ настройки: напоминания о событиях (за 24ч).
     */
    public static final String EVENT_REMINDER = "event_reminder";

    /**
     * Ключ настройки: обновления регистраций (подтверждение, отмена).
     */
    public static final String REGISTRATION_UPDATES = "registration_updates";

    /**
     * Ключ настройки: изменения событий (дата, место).
     */
    public static final String EVENT_CHANGES = "event_changes";

    /**
     * Ключ настройки: обновления организации (одобрение заявки и т.д.).
     */
    public static final String ORGANIZATION_UPDATES = "organization_updates";

    /**
     * ID пользователя.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    /**
     * Настройки в формате {"setting_key": true/false}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "jsonb")
    private Map<String, Boolean> settings = new HashMap<>();

    // === Фабричные методы ===

    /**
     * Создаёт настройки по умолчанию для пользователя.
     * Все уведомления включены.
     *
     * @param userId ID пользователя
     * @return настройки
     */
    public static NotificationPreference createDefault(UUID userId) {
        NotificationPreference pref = new NotificationPreference();
        pref.userId = userId;
        pref.settings = new HashMap<>(Map.of(
            EVENT_REMINDER, true,
            REGISTRATION_UPDATES, true,
            EVENT_CHANGES, true,
            ORGANIZATION_UPDATES, true
        ));
        return pref;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, включена ли настройка.
     * Если настройка не указана, возвращает true (по умолчанию включена).
     *
     * @param settingKey ключ настройки
     * @return true если уведомления этого типа разрешены
     */
    public boolean isEnabled(String settingKey) {
        return settings.getOrDefault(settingKey, true);
    }

    /**
     * Устанавливает значение настройки.
     *
     * @param settingKey ключ настройки
     * @param enabled    включена/выключена
     */
    public void setSetting(String settingKey, boolean enabled) {
        settings.put(settingKey, enabled);
    }

    /**
     * Обновляет несколько настроек сразу.
     *
     * @param newSettings новые настройки
     */
    public void updateSettings(Map<String, Boolean> newSettings) {
        if (newSettings != null) {
            settings.putAll(newSettings);
        }
    }
}
