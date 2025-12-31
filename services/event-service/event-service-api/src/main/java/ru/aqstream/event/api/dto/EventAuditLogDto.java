package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO для записи истории изменений события.
 *
 * @param id             идентификатор записи
 * @param eventId        идентификатор события
 * @param action         тип действия
 * @param actorId        идентификатор пользователя (null для системных действий)
 * @param actorEmail     email пользователя (null для системных действий)
 * @param changedFields  изменённые поля в формате {"field": {"from": "old", "to": "new"}}
 * @param description    описание изменения
 * @param createdAt      время изменения
 */
public record EventAuditLogDto(
    UUID id,
    UUID eventId,
    EventAuditAction action,
    UUID actorId,
    String actorEmail,
    Map<String, FieldChange> changedFields,
    String description,
    Instant createdAt
) {

    /**
     * Изменение поля.
     *
     * @param from старое значение
     * @param to   новое значение
     */
    public record FieldChange(
        String from,
        String to
    ) {}
}
