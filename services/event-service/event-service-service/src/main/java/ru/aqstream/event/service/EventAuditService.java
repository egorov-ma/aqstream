package ru.aqstream.event.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
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

    /**
     * Переводы названий полей для audit log.
     */
    private static final Map<String, String> FIELD_TRANSLATIONS = Map.ofEntries(
        Map.entry("title", "Название"),
        Map.entry("description", "Описание"),
        Map.entry("startsAt", "Дата начала"),
        Map.entry("endsAt", "Дата окончания"),
        Map.entry("timezone", "Часовой пояс"),
        Map.entry("locationType", "Тип локации"),
        Map.entry("locationAddress", "Адрес"),
        Map.entry("onlineUrl", "Ссылка"),
        Map.entry("maxCapacity", "Макс. участников"),
        Map.entry("registrationOpensAt", "Открытие регистрации"),
        Map.entry("registrationClosesAt", "Закрытие регистрации"),
        Map.entry("isPublic", "Публичность"),
        Map.entry("participantsVisibility", "Видимость участников"),
        Map.entry("groupId", "Группа")
    );

    /**
     * Определение поля для аудита.
     */
    private record AuditedField(String name, Function<Event, String> getter) { }

    /**
     * Список полей события для отслеживания изменений.
     */
    private static final List<AuditedField> AUDITED_FIELDS = List.of(
        new AuditedField("title", Event::getTitle),
        new AuditedField("description", Event::getDescription),
        new AuditedField("startsAt", e -> toString(e.getStartsAt())),
        new AuditedField("endsAt", e -> toString(e.getEndsAt())),
        new AuditedField("timezone", Event::getTimezone),
        new AuditedField("locationType", e -> toString(e.getLocationType())),
        new AuditedField("locationAddress", Event::getLocationAddress),
        new AuditedField("onlineUrl", Event::getOnlineUrl),
        new AuditedField("maxCapacity", e -> toString(e.getMaxCapacity())),
        new AuditedField("registrationOpensAt", e -> toString(e.getRegistrationOpensAt())),
        new AuditedField("registrationClosesAt", e -> toString(e.getRegistrationClosesAt())),
        new AuditedField("isPublic", e -> String.valueOf(e.isPublic())),
        new AuditedField("participantsVisibility", e -> toString(e.getParticipantsVisibility())),
        new AuditedField("groupId", e -> toString(e.getGroupId()))
    );

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
     * Использует список AUDITED_FIELDS для снижения cyclomatic complexity.
     */
    private Map<String, FieldChange> detectChanges(Event oldEvent, Event newEvent) {
        Map<String, FieldChange> changes = new LinkedHashMap<>();

        for (AuditedField field : AUDITED_FIELDS) {
            String oldValue = field.getter().apply(oldEvent);
            String newValue = field.getter().apply(newEvent);
            if (!Objects.equals(oldValue, newValue)) {
                changes.put(field.name(), new FieldChange(oldValue, newValue));
            }
        }

        return changes;
    }

    private static String toString(Object value) {
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
        return FIELD_TRANSLATIONS.getOrDefault(field, field);
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
