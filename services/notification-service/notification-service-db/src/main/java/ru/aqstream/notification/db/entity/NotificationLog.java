package ru.aqstream.notification.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.aqstream.notification.api.dto.NotificationChannel;
import ru.aqstream.notification.api.dto.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Лог отправки уведомления.
 *
 * <p>Хранит информацию обо всех попытках отправки уведомлений:
 * статус, ошибки, количество повторных попыток.</p>
 */
@Entity
@Table(name = "notification_logs", schema = "notification_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog {

    public static final int MAX_RETRY_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID пользователя-получателя.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Канал отправки.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    /**
     * Код шаблона (для аналитики).
     */
    @Column(name = "template_code", length = 50)
    private String templateCode;

    /**
     * Адрес получателя (telegram_chat_id или email).
     */
    @Column(name = "recipient", length = 255)
    private String recipient;

    /**
     * Тема письма (только для EMAIL).
     */
    @Column(name = "subject", length = 255)
    private String subject;

    /**
     * Отрендеренное тело сообщения.
     */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * Статус отправки.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * Сообщение об ошибке (при неудаче).
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Количество попыток отправки.
     */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /**
     * Время успешной отправки.
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Время создания записи.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // === Фабричные методы ===

    /**
     * Создаёт новую запись лога для Telegram уведомления.
     *
     * @param userId       ID пользователя
     * @param templateCode код шаблона
     * @param chatId       Telegram chat ID
     * @param body         тело сообщения
     * @return запись лога
     */
    public static NotificationLog createTelegram(UUID userId, String templateCode, String chatId, String body) {
        NotificationLog log = new NotificationLog();
        log.userId = userId;
        log.channel = NotificationChannel.TELEGRAM;
        log.templateCode = templateCode;
        log.recipient = chatId;
        log.body = body;
        log.status = NotificationStatus.PENDING;
        log.createdAt = Instant.now();
        return log;
    }

    /**
     * Создаёт новую запись лога для Email уведомления.
     *
     * @param userId       ID пользователя
     * @param templateCode код шаблона
     * @param email        email адрес
     * @param subject      тема письма
     * @param body         тело сообщения
     * @return запись лога
     */
    public static NotificationLog createEmail(
            UUID userId,
            String templateCode,
            String email,
            String subject,
            String body) {
        NotificationLog log = new NotificationLog();
        log.userId = userId;
        log.channel = NotificationChannel.EMAIL;
        log.templateCode = templateCode;
        log.recipient = email;
        log.subject = subject;
        log.body = body;
        log.status = NotificationStatus.PENDING;
        log.createdAt = Instant.now();
        return log;
    }

    // === Бизнес-методы ===

    /**
     * Отмечает уведомление как успешно отправленное.
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    /**
     * Отмечает уведомление как неудачное.
     *
     * @param errorMessage сообщение об ошибке
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * Отмечает уведомление как заблокированное (пользователь заблокировал бота).
     *
     * @param reason причина блокировки
     */
    public void markAsBlocked(String reason) {
        this.status = NotificationStatus.BLOCKED;
        this.errorMessage = reason;
    }

    /**
     * Проверяет, можно ли повторить отправку.
     *
     * @return true если количество попыток меньше максимального
     */
    public boolean canRetry() {
        return retryCount < MAX_RETRY_COUNT && status == NotificationStatus.FAILED;
    }

    /**
     * Сбрасывает статус для повторной попытки.
     */
    public void resetForRetry() {
        this.status = NotificationStatus.PENDING;
        this.errorMessage = null;
    }
}
