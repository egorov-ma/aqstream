package ru.aqstream.notification.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.aqstream.notification.api.dto.UserNotificationType;

/**
 * Уведомление пользователя для отображения в UI (bell icon).
 *
 * <p>Хранит уведомления, которые пользователь видит в интерфейсе.
 * Отличается от NotificationLog, который хранит логи отправки.</p>
 *
 * <p>Поддерживает multi-tenancy через tenant_id для RLS изоляции.</p>
 */
@Entity
@Table(name = "user_notifications", schema = "notification_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID организации (tenant) для RLS изоляции.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * ID пользователя-получателя.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Тип уведомления.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private UserNotificationType type;

    /**
     * Заголовок уведомления.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Текст сообщения.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Прочитано ли уведомление.
     */
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    /**
     * Тип связанной сущности (EVENT, REGISTRATION).
     */
    @Column(name = "linked_entity_type", length = 30)
    private String linkedEntityType;

    /**
     * ID связанной сущности.
     */
    @Column(name = "linked_entity_id")
    private UUID linkedEntityId;

    /**
     * Время создания.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // === Lifecycle callbacks ===

    /**
     * Валидация перед сохранением.
     * tenantId должен быть установлен вызывающим кодом через фабричные методы.
     */
    @PrePersist
    protected void prePersist() {
        if (this.tenantId == null) {
            throw new IllegalStateException(
                "tenantId обязателен. Используйте фабричные методы для создания уведомлений.");
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // === Фабричные методы ===

    /**
     * Создаёт уведомление о новой регистрации.
     *
     * @param tenantId        ID организации
     * @param userId          ID пользователя (организатора)
     * @param eventId         ID события
     * @param participantName имя участника
     * @return уведомление
     */
    public static UserNotification createNewRegistration(
            UUID tenantId,
            UUID userId,
            UUID eventId,
            String participantName
    ) {
        UserNotification notification = new UserNotification();
        notification.tenantId = tenantId;
        notification.userId = userId;
        notification.type = UserNotificationType.NEW_REGISTRATION;
        notification.title = "Новая регистрация";
        // Экранируем имя участника для безопасности
        notification.message = String.format("Участник %s зарегистрировался на событие",
            sanitize(participantName));
        notification.linkedEntityType = "EVENT";
        notification.linkedEntityId = eventId;
        notification.createdAt = Instant.now();
        return notification;
    }

    /**
     * Создаёт уведомление об изменении события.
     *
     * @param tenantId   ID организации
     * @param userId     ID пользователя (участника)
     * @param eventId    ID события
     * @param eventTitle название события
     * @return уведомление
     */
    public static UserNotification createEventUpdate(
            UUID tenantId,
            UUID userId,
            UUID eventId,
            String eventTitle
    ) {
        UserNotification notification = new UserNotification();
        notification.tenantId = tenantId;
        notification.userId = userId;
        notification.type = UserNotificationType.EVENT_UPDATE;
        notification.title = "Событие обновлено";
        notification.message = String.format("Событие \"%s\" было изменено", sanitize(eventTitle));
        notification.linkedEntityType = "EVENT";
        notification.linkedEntityId = eventId;
        notification.createdAt = Instant.now();
        return notification;
    }

    /**
     * Создаёт уведомление об отмене события.
     *
     * @param tenantId   ID организации
     * @param userId     ID пользователя (участника)
     * @param eventId    ID события
     * @param eventTitle название события
     * @return уведомление
     */
    public static UserNotification createEventCancelled(
            UUID tenantId,
            UUID userId,
            UUID eventId,
            String eventTitle
    ) {
        UserNotification notification = new UserNotification();
        notification.tenantId = tenantId;
        notification.userId = userId;
        notification.type = UserNotificationType.EVENT_CANCELLED;
        notification.title = "Событие отменено";
        notification.message = String.format("Событие \"%s\" было отменено", sanitize(eventTitle));
        notification.linkedEntityType = "EVENT";
        notification.linkedEntityId = eventId;
        notification.createdAt = Instant.now();
        return notification;
    }

    /**
     * Создаёт системное уведомление.
     *
     * @param tenantId ID организации
     * @param userId   ID пользователя
     * @param title    заголовок
     * @param message  текст
     * @return уведомление
     */
    public static UserNotification createSystem(
            UUID tenantId,
            UUID userId,
            String title,
            String message
    ) {
        UserNotification notification = new UserNotification();
        notification.tenantId = tenantId;
        notification.userId = userId;
        notification.type = UserNotificationType.SYSTEM;
        notification.title = sanitize(title);
        notification.message = sanitize(message);
        notification.createdAt = Instant.now();
        return notification;
    }

    /**
     * Экранирует потенциально опасные символы в строке.
     * Предотвращает XSS при отображении в UI.
     */
    private static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    // === Бизнес-методы ===

    /**
     * Отмечает уведомление как прочитанное.
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
