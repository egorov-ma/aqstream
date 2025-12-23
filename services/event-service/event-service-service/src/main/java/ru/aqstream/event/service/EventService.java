package ru.aqstream.event.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.CreateEventRequest;
import ru.aqstream.event.api.event.EventCancelledEvent;
import ru.aqstream.event.api.event.EventCompletedEvent;
import ru.aqstream.event.api.event.EventCreatedEvent;
import ru.aqstream.event.api.event.EventPublishedEvent;
import ru.aqstream.event.api.event.EventUpdatedEvent;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.UpdateEventRequest;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.api.exception.EventNotEditableException;
import ru.aqstream.event.api.exception.EventSlugAlreadyExistsException;
import ru.aqstream.event.api.util.SlugGenerator;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;

/**
 * Сервис управления событиями.
 * CRUD операции и управление жизненным циклом.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private static final int MAX_SLUG_ATTEMPTS = 5;

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventPublisher eventPublisher;

    // ==================== CRUD ====================

    /**
     * Создаёт новое событие в статусе DRAFT.
     *
     * @param request данные для создания
     * @return созданное событие
     */
    @Transactional
    public EventDto create(CreateEventRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Создание события: tenantId={}, title={}", tenantId, request.title());

        // Генерируем уникальный slug
        String slug = generateUniqueSlug(request.title(), tenantId);

        // Создаём событие
        Event event = Event.create(
            request.title(),
            slug,
            request.startsAt(),
            request.timezoneOrDefault()
        );

        // Устанавливаем опциональные поля
        event.setDescription(request.description());
        event.setEndsAt(request.endsAt());
        event.setLocationType(request.locationTypeOrDefault());
        event.setLocationAddress(request.locationAddress());
        event.setOnlineUrl(request.onlineUrl());
        event.setMaxCapacity(request.maxCapacity());
        event.setRegistrationOpensAt(request.registrationOpensAt());
        event.setRegistrationClosesAt(request.registrationClosesAt());
        event.setPublic(request.isPublicOrDefault());
        event.setParticipantsVisibility(request.participantsVisibilityOrDefault());
        event.setGroupId(request.groupId());

        event = eventRepository.save(event);

        log.info("Событие создано: eventId={}, slug={}, tenantId={}",
            event.getId(), event.getSlug(), tenantId);

        // Публикуем событие в RabbitMQ
        eventPublisher.publish(new EventCreatedEvent(
            event.getId(),
            tenantId,
            event.getTitle(),
            event.getSlug(),
            event.getStartsAt()
        ));

        return eventMapper.toDto(event);
    }

    /**
     * Возвращает событие по ID.
     *
     * @param eventId идентификатор события
     * @return событие
     */
    @Transactional(readOnly = true)
    public EventDto getById(UUID eventId) {
        Event event = findEventById(eventId);
        return eventMapper.toDto(event);
    }

    /**
     * Возвращает событие по slug.
     *
     * @param slug URL-slug события
     * @return событие
     */
    @Transactional(readOnly = true)
    public EventDto getBySlug(String slug) {
        UUID tenantId = TenantContext.getTenantId();
        Event event = eventRepository.findBySlugAndTenantId(slug, tenantId)
            .orElseThrow(() -> new EventNotFoundException(slug));
        return eventMapper.toDto(event);
    }

    /**
     * Обновляет событие.
     *
     * @param eventId идентификатор события
     * @param request данные для обновления
     * @return обновлённое событие
     * @throws EventNotEditableException если событие нельзя редактировать
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity") // Partial update паттерн требует много проверок
    @Transactional
    public EventDto update(UUID eventId, UpdateEventRequest request) {
        log.info("Обновление события: eventId={}", eventId);

        Event event = findEventById(eventId);

        // Проверяем, что событие можно редактировать
        if (!event.isEditable()) {
            throw new EventNotEditableException(eventId, event.getStatus());
        }

        // Обновляем только переданные поля
        if (request.title() != null) {
            event.updateInfo(request.title(), event.getDescription());
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        if (request.startsAt() != null || request.endsAt() != null) {
            event.updateDates(request.startsAt(), request.endsAt());
        }
        if (request.timezone() != null) {
            event.setTimezone(request.timezone());
        }
        if (request.locationType() != null || request.locationAddress() != null || request.onlineUrl() != null) {
            event.updateLocation(request.locationType(), request.locationAddress(), request.onlineUrl());
        }
        if (request.maxCapacity() != null || request.registrationOpensAt() != null
            || request.registrationClosesAt() != null) {
            event.updateRegistration(request.maxCapacity(), request.registrationOpensAt(),
                request.registrationClosesAt());
        }
        if (request.isPublic() != null || request.participantsVisibility() != null) {
            event.updateVisibility(
                request.isPublic() != null ? request.isPublic() : event.isPublic(),
                request.participantsVisibility()
            );
        }
        if (request.groupId() != null) {
            event.setGroupId(request.groupId());
        }

        event = eventRepository.save(event);

        log.info("Событие обновлено: eventId={}", eventId);

        // Публикуем событие в RabbitMQ
        eventPublisher.publish(new EventUpdatedEvent(
            event.getId(),
            event.getTenantId(),
            event.getTitle(),
            event.getStartsAt(),
            Instant.now()
        ));

        return eventMapper.toDto(event);
    }

    /**
     * Удаляет событие (soft delete).
     *
     * @param eventId идентификатор события
     */
    @Transactional
    public void delete(UUID eventId) {
        log.info("Удаление события: eventId={}", eventId);

        Event event = findEventById(eventId);
        event.softDelete();
        eventRepository.save(event);

        log.info("Событие удалено: eventId={}", eventId);
    }

    // ==================== Lifecycle ====================

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

        return eventMapper.toDto(event);
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

        return eventMapper.toDto(event);
    }

    /**
     * Отменяет событие (любой статус → CANCELLED).
     *
     * @param eventId идентификатор события
     * @return отменённое событие
     */
    @Transactional
    public EventDto cancel(UUID eventId) {
        log.info("Отмена события: eventId={}", eventId);

        Event event = findEventById(eventId);
        event.cancel();
        event = eventRepository.save(event);

        log.info("Событие отменено: eventId={}", eventId);

        // Публикуем событие в RabbitMQ
        // Notification-service обработает это событие и уведомит всех зарегистрированных участников
        eventPublisher.publish(new EventCancelledEvent(
            event.getId(),
            event.getTenantId(),
            event.getTitle(),
            event.getStartsAt(),
            Instant.now()
        ));

        return eventMapper.toDto(event);
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

        return eventMapper.toDto(event);
    }

    // ==================== Списки ====================

    /**
     * Возвращает страницу событий организации.
     *
     * @param pageable параметры пагинации
     * @return страница событий
     */
    @Transactional(readOnly = true)
    public PageResponse<EventDto> findAll(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Event> page = eventRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page, eventMapper::toDto);
    }

    /**
     * Возвращает страницу событий с фильтром по статусу.
     *
     * @param status   статус события
     * @param pageable параметры пагинации
     * @return страница событий
     */
    @Transactional(readOnly = true)
    public PageResponse<EventDto> findByStatus(EventStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Event> page = eventRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(page, eventMapper::toDto);
    }

    /**
     * Возвращает события группы.
     *
     * @param groupId  идентификатор группы
     * @param pageable параметры пагинации
     * @return страница событий
     */
    @Transactional(readOnly = true)
    public PageResponse<EventDto> findByGroup(UUID groupId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Event> page = eventRepository.findByTenantIdAndGroupId(tenantId, groupId, pageable);
        return PageResponse.of(page, eventMapper::toDto);
    }

    /**
     * Возвращает события в диапазоне дат.
     *
     * @param from     начало периода
     * @param to       конец периода
     * @param pageable параметры пагинации
     * @return страница событий
     */
    @Transactional(readOnly = true)
    public PageResponse<EventDto> findByDateRange(Instant from, Instant to, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Event> page = eventRepository.findByTenantIdAndDateRange(tenantId, from, to, pageable);
        return PageResponse.of(page, eventMapper::toDto);
    }

    // ==================== Публичные ====================

    /**
     * Возвращает публичное событие по slug.
     * Не требует tenant context.
     *
     * @param slug URL-slug события
     * @return событие
     */
    @Transactional(readOnly = true)
    public EventDto getPublicBySlug(String slug) {
        Event event = eventRepository.findPublicBySlug(slug)
            .orElseThrow(() -> new EventNotFoundException(slug));
        return eventMapper.toDto(event);
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
     * Генерирует уникальный slug в рамках tenant.
     *
     * @param title    название события
     * @param tenantId идентификатор организации
     * @return уникальный slug
     */
    private String generateUniqueSlug(String title, UUID tenantId) {
        String baseSlug = SlugGenerator.generate(title);

        // Проверяем уникальность
        if (!eventRepository.existsBySlugAndTenantId(baseSlug, tenantId)) {
            return baseSlug;
        }

        // Добавляем суффикс при коллизии
        for (int i = 0; i < MAX_SLUG_ATTEMPTS; i++) {
            String slugWithSuffix = SlugGenerator.generateWithSuffix(title);
            if (!eventRepository.existsBySlugAndTenantId(slugWithSuffix, tenantId)) {
                return slugWithSuffix;
            }
        }

        // Крайне маловероятно, но на всякий случай
        throw new EventSlugAlreadyExistsException(baseSlug);
    }
}
