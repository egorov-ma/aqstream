package ru.aqstream.event.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.security.SecurityContext;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.event.api.dto.EventAuditAction;
import ru.aqstream.event.api.dto.EventAuditLogDto;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.EventAuditLog;
import ru.aqstream.event.db.entity.EventAuditLog.FieldChange;
import ru.aqstream.event.db.repository.EventAuditLogRepository;

/**
 * Сервис для записи и чтения истории изменений событий.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventAuditService {

    private final EventAuditLogRepository auditLogRepository;

    // ==================== Запись в audit log ====================

    /**
     * Записывает создание события.
     *
     * @param event созданное событие
     */
    @Transactional
    public void logCreated(Event event) {
        EventAuditLog auditLog = createAuditLog(event.getId(), EventAuditAction.CREATED);
        auditLog.setDescription("Событие создано");
        auditLogRepository.save(auditLog);
        log.debug("Audit log: событие создано, eventId={}", event.getId());
    }

    /**
     * Записывает обновление события с детализацией изменений.
     *
     * @param eventId   идентификатор события
     * @param oldEvent  старая версия события
     * @param newEvent  новая версия события
     */
    @Transactional
    public void logUpdated(UUID eventId, Event oldEvent, Event newEvent) {
        Map<String, FieldChange> changedFields = detectChanges(oldEvent, newEvent);
        if (changedFields.isEmpty()) {
            return; // Нет изменений — не записываем
        }

        EventAuditLog auditLog = createAuditLog(eventId, EventAuditAction.UPDATED);
        auditLog.setChangedFields(changedFields);
        auditLog.setDescription(buildUpdateDescription(changedFields));
        auditLogRepository.save(auditLog);
        log.debug("Audit log: событие обновлено, eventId={}, изменений={}", eventId, changedFields.size());
    }

    /**
     * Записывает публикацию события.
     *
     * @param event опубликованное событие
     */
    @Transactional
    public void logPublished(Event event) {
        EventAuditLog auditLog = createAuditLog(event.getId(), EventAuditAction.PUBLISHED);
        auditLog.setDescription("Событие опубликовано");
        auditLogRepository.save(auditLog);
        log.debug("Audit log: событие опубликовано, eventId={}", event.getId());
    }

    /**
     * Записывает снятие с публикации.
     *
     * @param event событие
     */
    @Transactional
    public void logUnpublished(Event event) {
        EventAuditLog auditLog = createAuditLog(event.getId(), EventAuditAction.UNPUBLISHED);
        auditLog.setDescription("Событие снято с публикации");
        auditLogRepository.save(auditLog);
        log.debug("Audit log: событие снято с публикации, eventId={}", event.getId());
    }

    /**
     * Записывает отмену события.
     *
     * @param event  отменённое событие
     * @param reason причина отмены
     */
    @Transactional
    public void logCancelled(Event event, String reason) {
        EventAuditLog auditLog = createAuditLog(event.getId(), EventAuditAction.CANCELLED);
        String description = reason != null && !reason.isBlank()
            ? "Событие отменено: " + reason
            : "Событие отменено";
        auditLog.setDescription(description);
        auditLogRepository.save(auditLog);
        log.debug("Audit log: событие отменено, eventId={}", event.getId());
    }

    /**
     * Записывает завершение события.
     *
     * @param event завершённое событие
     */
    @Transactional
    public void logCompleted(Event event) {
        EventAuditLog auditLog = createAuditLog(event.getId(), EventAuditAction.COMPLETED);
        auditLog.setDescription("Событие завершено");
        auditLogRepository.save(auditLog);
        log.debug("Audit log: событие завершено, eventId={}", event.getId());
    }

    /**
     * Записывает удаление события (soft delete).
     *
     * @param event удалённое событие
     */
    @Transactional
    public void logDeleted(Event event) {
        EventAuditLog auditLog = createAuditLog(event.getId(), EventAuditAction.DELETED);
        auditLog.setDescription("Событие удалено");
        auditLogRepository.save(auditLog);
        log.debug("Audit log: событие удалено, eventId={}", event.getId());
    }

    // ==================== Чтение audit log ====================

    /**
     * Возвращает историю изменений события.
     *
     * @param eventId  идентификатор события
     * @param pageable параметры пагинации
     * @return страница записей истории
     */
    @Transactional(readOnly = true)
    public PageResponse<EventAuditLogDto> getEventHistory(UUID eventId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<EventAuditLog> page = auditLogRepository.findByEventIdAndTenantIdOrderByCreatedAtDesc(
            eventId, tenantId, pageable);
        return PageResponse.of(page, this::toDto);
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Создаёт базовую запись аудита с данными текущего пользователя.
     */
    private EventAuditLog createAuditLog(UUID eventId, EventAuditAction action) {
        UserPrincipal user = SecurityContext.getCurrentUserOptional().orElse(null);
        UUID actorId = user != null ? user.userId() : null;
        String actorEmail = user != null ? user.email() : null;

        return EventAuditLog.create(eventId, action, actorId, actorEmail, null);
    }

    /**
     * Определяет изменённые поля между старой и новой версией события.
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity") // Последовательное сравнение полей
    private Map<String, FieldChange> detectChanges(Event oldEvent, Event newEvent) {
        Map<String, FieldChange> changes = new LinkedHashMap<>();

        compareField(changes, "title", oldEvent.getTitle(), newEvent.getTitle());
        compareField(changes, "description", oldEvent.getDescription(), newEvent.getDescription());
        compareField(changes, "startsAt", toString(oldEvent.getStartsAt()), toString(newEvent.getStartsAt()));
        compareField(changes, "endsAt", toString(oldEvent.getEndsAt()), toString(newEvent.getEndsAt()));
        compareField(changes, "timezone", oldEvent.getTimezone(), newEvent.getTimezone());
        compareField(changes, "locationType", toString(oldEvent.getLocationType()),
            toString(newEvent.getLocationType()));
        compareField(changes, "locationAddress", oldEvent.getLocationAddress(), newEvent.getLocationAddress());
        compareField(changes, "onlineUrl", oldEvent.getOnlineUrl(), newEvent.getOnlineUrl());
        compareField(changes, "maxCapacity", toString(oldEvent.getMaxCapacity()), toString(newEvent.getMaxCapacity()));
        compareField(changes, "registrationOpensAt", toString(oldEvent.getRegistrationOpensAt()),
            toString(newEvent.getRegistrationOpensAt()));
        compareField(changes, "registrationClosesAt", toString(oldEvent.getRegistrationClosesAt()),
            toString(newEvent.getRegistrationClosesAt()));
        compareField(changes, "isPublic", String.valueOf(oldEvent.isPublic()), String.valueOf(newEvent.isPublic()));
        compareField(changes, "participantsVisibility", toString(oldEvent.getParticipantsVisibility()),
            toString(newEvent.getParticipantsVisibility()));
        compareField(changes, "groupId", toString(oldEvent.getGroupId()), toString(newEvent.getGroupId()));

        return changes;
    }

    private void compareField(Map<String, FieldChange> changes, String field, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.put(field, new FieldChange(oldValue, newValue));
        }
    }

    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * Формирует описание изменения на основе изменённых полей.
     */
    private String buildUpdateDescription(Map<String, FieldChange> changes) {
        if (changes.size() == 1) {
            String field = changes.keySet().iterator().next();
            return "Изменено поле: " + translateFieldName(field);
        }
        return "Изменено полей: " + changes.size();
    }

    private String translateFieldName(String field) {
        return switch (field) {
            case "title" -> "Название";
            case "description" -> "Описание";
            case "startsAt" -> "Дата начала";
            case "endsAt" -> "Дата окончания";
            case "timezone" -> "Часовой пояс";
            case "locationType" -> "Тип локации";
            case "locationAddress" -> "Адрес";
            case "onlineUrl" -> "Ссылка";
            case "maxCapacity" -> "Макс. участников";
            case "registrationOpensAt" -> "Открытие регистрации";
            case "registrationClosesAt" -> "Закрытие регистрации";
            case "isPublic" -> "Публичность";
            case "participantsVisibility" -> "Видимость участников";
            case "groupId" -> "Группа";
            default -> field;
        };
    }

    /**
     * Преобразует entity в DTO.
     */
    private EventAuditLogDto toDto(EventAuditLog entity) {
        Map<String, EventAuditLogDto.FieldChange> dtoChanges = null;
        if (entity.getChangedFields() != null) {
            dtoChanges = new LinkedHashMap<>();
            for (Map.Entry<String, FieldChange> entry : entity.getChangedFields().entrySet()) {
                dtoChanges.put(entry.getKey(),
                    new EventAuditLogDto.FieldChange(entry.getValue().from(), entry.getValue().to()));
            }
        }

        return new EventAuditLogDto(
            entity.getId(),
            entity.getEventId(),
            entity.getAction(),
            entity.getActorId(),
            entity.getActorEmail(),
            dtoChanges,
            entity.getDescription(),
            entity.getCreatedAt()
        );
    }
}
