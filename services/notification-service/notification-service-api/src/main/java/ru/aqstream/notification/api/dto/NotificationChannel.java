package ru.aqstream.notification.api.dto;

/**
 * Каналы отправки уведомлений.
 */
public enum NotificationChannel {

    /**
     * Telegram — основной канал уведомлений.
     */
    TELEGRAM,

    /**
     * Email — только для аутентификации (верификация, сброс пароля).
     */
    EMAIL
}
