package ru.aqstream.notification.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO пользовательского уведомления для UI (bell icon).
 *
 * <p>Не содержит userId — клиент уже знает свой ID из JWT token.</p>
 *
 * @param id           идентификатор уведомления
 * @param type         тип уведомления
 * @param title        заголовок
 * @param message      текст сообщения
 * @param isRead       прочитано ли уведомление
 * @param linkedEntity связанная сущность (событие, регистрация)
 * @param createdAt    дата создания
 */
public record UserNotificationDto(
    UUID id,
    UserNotificationType type,
    String title,
    String message,
    boolean isRead,
    LinkedEntityDto linkedEntity,
    Instant createdAt
) {

    /**
     * Связанная сущность для навигации.
     *
     * @param entityType тип сущности (EVENT, REGISTRATION)
     * @param entityId   ID сущности
     */
    public record LinkedEntityDto(
        String entityType,
        UUID entityId
    ) {
    }
}
