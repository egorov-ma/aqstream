package ru.aqstream.event.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.RecurrenceRuleDto;
import ru.aqstream.event.api.event.EventCancelledEvent;
import ru.aqstream.event.api.event.EventCompletedEvent;
import ru.aqstream.event.api.event.EventPublishedEvent;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RecurrenceRuleRepository;

/**
 * Сервис управления жизненным циклом событий.
 * Отвечает за переходы между статусами: DRAFT → PUBLISHED → COMPLETED/CANCELLED.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventLifecycleService {

    private final EventRepository eventRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final EventMapper eventMapper;
    private final RecurrenceRuleMapper recurrenceRuleMapper;
    private final EventPublisher eventPublisher;
    private final EventAuditService eventAuditService;

    /**
     * Публикует событие (DRAFT → PUBLISHED).
     *
     * @param eventId идентификатор события
     * @return опубликованное событие
     */
    @Transactional
    public EventDto publish(UUID eventId) {
        log.info("Публикация события: eventId={}", eventId);

        Event event = findEventById(eventId);
        event.publish();
        event = eventRepository.save(event);

        log.info("Событие опубликовано: eventId={}", eventId);

        // Публикуем событие в RabbitMQ
        eventPublisher.publish(new EventPublishedEvent(
            event.getId(),
            event.getTenantId(),
            event.getTitle(),
            event.getSlug(),
            event.getStartsAt(),
            Instant.now()
        ));

        // Записываем в audit log
        eventAuditService.logPublished(event);

        return mapToDto(event);
    }

    /**
     * Снимает событие с публикации (PUBLISHED → DRAFT).
     *
     * @param eventId идентификатор события
     * @return событие в статусе DRAFT
     */
    @Transactional
    public EventDto unpublish(UUID eventId) {
        log.info("Снятие с публикации: eventId={}", eventId);

        Event event = findEventById(eventId);
        event.unpublish();
        event = eventRepository.save(event);

        log.info("Событие снято с публикации: eventId={}", eventId);

        // Записываем в audit log
        eventAuditService.logUnpublished(event);

        return mapToDto(event);
    }

    /**
     * Отменяет событие (любой статус → CANCELLED).
     *
     * @param eventId идентификатор события
     * @return отменённое событие
     */
    @Transactional
    public EventDto cancel(UUID eventId) {
        return cancel(eventId, null);
    }

    /**
     * Отменяет событие с указанием причины (любой статус → CANCELLED).
     *
     * @param eventId идентификатор события
     * @param reason  причина отмены (опционально)
     * @return отменённое событие
     */
    @Transactional
    public EventDto cancel(UUID eventId, String reason) {
        log.info("Отмена события: eventId={}, reason={}", eventId, reason != null ? "указана" : "не указана");

        Event event = findEventById(eventId);
        event.cancel(reason);
        event = eventRepository.save(event);

        log.info("Событие отменено: eventId={}", eventId);

        // Публикуем событие в RabbitMQ
        // Notification-service обработает это событие и уведомит всех зарегистрированных участников
        eventPublisher.publish(new EventCancelledEvent(
            event.getId(),
            event.getTenantId(),
            event.getTitle(),
            event.getStartsAt(),
            event.getCancelledAt(),
            event.getCancelReason()
        ));

        // Записываем в audit log
        eventAuditService.logCancelled(event, reason);

        return mapToDto(event);
    }

    /**
     * Завершает событие (PUBLISHED → COMPLETED).
     *
     * @param eventId идентификатор события
     * @return завершённое событие
     */
    @Transactional
    public EventDto complete(UUID eventId) {
        log.info("Завершение события: eventId={}", eventId);

        Event event = findEventById(eventId);
        event.complete();
        event = eventRepository.save(event);

        log.info("Событие завершено: eventId={}", eventId);

        // Публикуем событие в RabbitMQ
        eventPublisher.publish(new EventCompletedEvent(
            event.getId(),
            event.getTenantId(),
            event.getTitle(),
            Instant.now()
        ));

        // Записываем в audit log
        eventAuditService.logCompleted(event);

        return mapToDto(event);
    }

    // ==================== Вспомогательные ====================

    /**
     * Находит событие по ID в рамках текущего tenant.
     * Defense in depth: проверка tenant_id на уровне приложения + RLS на уровне БД.
     *
     * @param eventId идентификатор события
     * @return событие
     * @throws EventNotFoundException если событие не найдено или принадлежит другому tenant
     */
    private Event findEventById(UUID eventId) {
        UUID tenantId = TenantContext.getTenantId();
        return eventRepository.findByIdAndTenantId(eventId, tenantId)
            .orElseThrow(() -> new EventNotFoundException(eventId, tenantId));
    }

    /**
     * Преобразует Event в EventDto с загрузкой правила повторения.
     *
     * @param event событие
     * @return DTO события
     */
    private EventDto mapToDto(Event event) {
        RecurrenceRuleDto ruleDto = null;
        if (event.getRecurrenceRuleId() != null) {
            ruleDto = recurrenceRuleRepository.findById(event.getRecurrenceRuleId())
                .map(recurrenceRuleMapper::toDto)
                .orElse(null);
        }
        return eventMapper.toDto(event, ruleDto);
    }
}
