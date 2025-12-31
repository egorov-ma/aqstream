package ru.aqstream.event.api.dto;

/**
 * Типы действий для audit log событий.
 */
public enum EventAuditAction {
    /** Событие создано */
    CREATED,
    /** Событие обновлено */
    UPDATED,
    /** Событие опубликовано */
    PUBLISHED,
    /** Событие снято с публикации */
    UNPUBLISHED,
    /** Событие отменено */
    CANCELLED,
    /** Событие завершено */
    COMPLETED,
    /** Событие удалено (soft delete) */
    DELETED
}
