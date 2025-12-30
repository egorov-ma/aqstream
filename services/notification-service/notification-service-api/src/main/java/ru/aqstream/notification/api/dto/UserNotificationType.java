package ru.aqstream.notification.api.dto;

/**
 * Тип пользовательского уведомления для UI.
 */
public enum UserNotificationType {

    /**
     * Новая регистрация на событие организатора.
     */
    NEW_REGISTRATION,

    /**
     * Изменение события.
     */
    EVENT_UPDATE,

    /**
     * Отмена события.
     */
    EVENT_CANCELLED,

    /**
     * Напоминание о событии.
     */
    EVENT_REMINDER,

    /**
     * Системное уведомление.
     */
    SYSTEM
}
