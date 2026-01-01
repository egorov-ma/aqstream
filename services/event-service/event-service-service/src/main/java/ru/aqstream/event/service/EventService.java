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
import ru.aqstream.event.api.dto.CreateRecurrenceRuleRequest;
import ru.aqstream.event.api.dto.RecurrenceRuleDto;
import ru.aqstream.event.api.event.EventCreatedEvent;
import ru.aqstream.event.api.event.EventUpdatedEvent;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.UpdateEventRequest;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.api.exception.EventNotEditableException;
import ru.aqstream.event.api.exception.EventSlugAlreadyExistsException;
import ru.aqstream.event.api.util.SlugGenerator;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.RecurrenceRule;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RecurrenceRuleRepository;

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
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final EventMapper eventMapper;
    private final RecurrenceRuleMapper recurrenceRuleMapper;
    private final EventPublisher eventPublisher;
    private final EventAuditService eventAuditService;
    private final EventLifecycleService eventLifecycleService;
    private final OrganizationNameResolver organizationNameResolver;

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

        // Обрабатываем правило повторения если указано
        RecurrenceRule recurrenceRule = null;
        if (request.recurrenceRule() != null) {
            recurrenceRule = createRecurrenceRule(request.recurrenceRule(), tenantId);
            event.setRecurrenceRuleId(recurrenceRule.getId());
            log.info("Создано правило повторения: ruleId={}, frequency={}",
                recurrenceRule.getId(), recurrenceRule.getFrequency());
        }

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

        // Записываем в audit log
        eventAuditService.logCreated(event);

        RecurrenceRuleDto ruleDto = recurrenceRule != null
            ? recurrenceRuleMapper.toDto(recurrenceRule)
            : null;
        return eventMapper.toDto(event, ruleDto);
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
        return mapToDto(event);
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
        return mapToDto(event);
    }

    /**
     * Обновляет событие.
     *
     * @param eventId идентификатор события
     * @param request данные для обновления
     * @return обновлённое событие
     * @throws EventNotEditableException если событие нельзя редактировать
     */
    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:MethodLength"})
    // Partial update паттерн требует много проверок
    @Transactional
    public EventDto update(UUID eventId, UpdateEventRequest request) {
        log.info("Обновление события: eventId={}", eventId);

        Event event = findEventById(eventId);

        // Проверяем, что событие можно редактировать
        if (!event.isEditable()) {
            throw new EventNotEditableException(eventId, event.getStatus());
        }

        // Сохраняем снимок для audit log
        Event oldSnapshot = createSnapshot(event);

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

        // Записываем в audit log
        eventAuditService.logUpdated(eventId, oldSnapshot, event);

        return mapToDto(event);
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

        // Записываем в audit log
        eventAuditService.logDeleted(event);
    }

    // ==================== Lifecycle ====================
    // Lifecycle методы делегируются в EventLifecycleService

    /**
     * Публикует событие (DRAFT → PUBLISHED).
     *
     * @param eventId идентификатор события
     * @return опубликованное событие
     */
    public EventDto publish(UUID eventId) {
        return eventLifecycleService.publish(eventId);
    }

    /**
     * Снимает событие с публикации (PUBLISHED → DRAFT).
     *
     * @param eventId идентификатор события
     * @return событие в статусе DRAFT
     */
    public EventDto unpublish(UUID eventId) {
        return eventLifecycleService.unpublish(eventId);
    }

    /**
     * Отменяет событие (любой статус → CANCELLED).
     *
     * @param eventId идентификатор события
     * @return отменённое событие
     */
    public EventDto cancel(UUID eventId) {
        return eventLifecycleService.cancel(eventId);
    }

    /**
     * Отменяет событие с указанием причины (любой статус → CANCELLED).
     *
     * @param eventId идентификатор события
     * @param reason  причина отмены (опционально)
     * @return отменённое событие
     */
    public EventDto cancel(UUID eventId, String reason) {
        return eventLifecycleService.cancel(eventId, reason);
    }

    /**
     * Завершает событие (PUBLISHED → COMPLETED).
     *
     * @param eventId идентификатор события
     * @return завершённое событие
     */
    public EventDto complete(UUID eventId) {
        return eventLifecycleService.complete(eventId);
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
        return PageResponse.of(page, this::mapToDto);
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
        return PageResponse.of(page, this::mapToDto);
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
        return PageResponse.of(page, this::mapToDto);
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
        return PageResponse.of(page, this::mapToDto);
    }

    // ==================== Публичные ====================

    /**
     * Возвращает публичное событие по slug.
     * Не требует tenant context.
     * Включает название организатора из user-service.
     *
     * @param slug URL-slug события
     * @return событие с названием организатора
     */
    @Transactional(readOnly = true)
    public EventDto getPublicBySlug(String slug) {
        Event event = eventRepository.findPublicBySlug(slug)
            .orElseThrow(() -> new EventNotFoundException(slug));

        // Получаем название организатора из user-service (с кэшированием)
        String organizerName = organizationNameResolver.resolve(event.getTenantId());

        RecurrenceRuleDto ruleDto = null;
        if (event.getRecurrenceRuleId() != null) {
            ruleDto = recurrenceRuleRepository.findById(event.getRecurrenceRuleId())
                .map(recurrenceRuleMapper::toDto)
                .orElse(null);
        }

        return eventMapper.toDto(event, organizerName, ruleDto);
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

    /**
     * Создаёт снимок события для сравнения в audit log.
     * Копирует только поля, которые отслеживаются в audit.
     *
     * @param event исходное событие
     * @return снимок события
     */
    private Event createSnapshot(Event event) {
        Event snapshot = Event.create(
            event.getTitle(),
            event.getSlug(),
            event.getStartsAt(),
            event.getTimezone()
        );
        snapshot.setDescription(event.getDescription());
        snapshot.setEndsAt(event.getEndsAt());
        snapshot.setLocationType(event.getLocationType());
        snapshot.setLocationAddress(event.getLocationAddress());
        snapshot.setOnlineUrl(event.getOnlineUrl());
        snapshot.setMaxCapacity(event.getMaxCapacity());
        snapshot.setRegistrationOpensAt(event.getRegistrationOpensAt());
        snapshot.setRegistrationClosesAt(event.getRegistrationClosesAt());
        snapshot.setPublic(event.isPublic());
        snapshot.setParticipantsVisibility(event.getParticipantsVisibility());
        snapshot.setGroupId(event.getGroupId());
        return snapshot;
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

    /**
     * Создаёт правило повторения из запроса.
     *
     * @param request  запрос на создание правила
     * @param tenantId идентификатор организации
     * @return созданное правило повторения
     */
    private RecurrenceRule createRecurrenceRule(CreateRecurrenceRuleRequest request, UUID tenantId) {
        RecurrenceRule rule = recurrenceRuleMapper.toEntity(request);
        rule.setTenantId(tenantId);

        // Устанавливаем интервал с дефолтом
        if (rule.getIntervalValue() < 1) {
            rule.setIntervalValue(request.intervalOrDefault());
        }

        return recurrenceRuleRepository.save(rule);
    }
}
