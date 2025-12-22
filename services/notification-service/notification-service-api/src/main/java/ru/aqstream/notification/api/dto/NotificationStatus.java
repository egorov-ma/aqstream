package ru.aqstream.notification.api.dto;

/**
 * Статусы отправки уведомлений.
 */
public enum NotificationStatus {

    /**
     * Уведомление создано, ожидает отправки.
     */
    PENDING,

    /**
     * Уведомление успешно отправлено.
     */
    SENT,

    /**
     * Ошибка отправки.
     */
    FAILED,

    /**
     * Пользователь заблокировал бота или chat_id недоступен.
     */
    BLOCKED
}
