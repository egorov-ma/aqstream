package ru.aqstream.event.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.CreateTicketTypeRequest;
import ru.aqstream.event.api.dto.TicketTypeDto;
import ru.aqstream.event.api.dto.UpdateTicketTypeRequest;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.api.exception.EventNotEditableException;
import ru.aqstream.event.api.exception.TicketTypeHasRegistrationsException;
import ru.aqstream.event.api.exception.TicketTypeNotFoundException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

/**
 * Сервис управления типами билетов.
 * CRUD операции для типов билетов события.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final TicketTypeMapper ticketTypeMapper;

    // ==================== CRUD ====================

    /**
     * Создаёт новый тип билета для события.
     *
     * @param eventId идентификатор события
     * @param request данные для создания
     * @return созданный тип билета
     * @throws EventNotFoundException      если событие не найдено
     * @throws EventNotEditableException   если событие нельзя редактировать
     */
    @Transactional
    public TicketTypeDto create(UUID eventId, CreateTicketTypeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Создание типа билета: eventId={}, name={}", eventId, request.name());

        // Находим событие и проверяем возможность редактирования
        Event event = findEventByIdForEdit(eventId, tenantId);

        // Определяем порядок сортировки
        int sortOrder = request.sortOrderOrDefault();
        if (sortOrder == 0) {
            Integer maxSortOrder = ticketTypeRepository.findMaxSortOrderByEventId(eventId);
            sortOrder = maxSortOrder != null ? maxSortOrder + 1 : 0;
        }

        // Создаём тип билета
        TicketType ticketType = TicketType.create(event, request.name());
        ticketType.setDescription(request.description());
        ticketType.updateQuantity(request.quantity());
        ticketType.updateSalesPeriod(request.salesStart(), request.salesEnd());
        ticketType.setSortOrder(sortOrder);

        ticketType = ticketTypeRepository.save(ticketType);

        log.info("Тип билета создан: ticketTypeId={}, eventId={}, name={}",
            ticketType.getId(), eventId, ticketType.getName());

        return ticketTypeMapper.toDto(ticketType);
    }

    /**
     * Возвращает тип билета по ID.
     *
     * @param eventId      идентификатор события
     * @param ticketTypeId идентификатор типа билета
     * @return тип билета
     * @throws TicketTypeNotFoundException если тип билета не найден
     */
    @Transactional(readOnly = true)
    public TicketTypeDto getById(UUID eventId, UUID ticketTypeId) {
        // Проверяем доступ к событию
        verifyEventAccess(eventId);

        TicketType ticketType = findTicketTypeById(ticketTypeId, eventId);
        return ticketTypeMapper.toDto(ticketType);
    }

    /**
     * Обновляет тип билета.
     *
     * <p>Для опубликованных событий с регистрациями запрещено:
     * <ul>
     *   <li>Уменьшать количество билетов ниже проданного</li>
     * </ul>
     * </p>
     *
     * @param eventId      идентификатор события
     * @param ticketTypeId идентификатор типа билета
     * @param request      данные для обновления
     * @return обновлённый тип билета
     * @throws TicketTypeNotFoundException        если тип билета не найден
     * @throws EventNotEditableException          если событие нельзя редактировать
     * @throws TicketTypeHasRegistrationsException если пытаемся уменьшить количество ниже проданного
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity") // Partial update паттерн требует много проверок
    @Transactional
    public TicketTypeDto update(UUID eventId, UUID ticketTypeId, UpdateTicketTypeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Обновление типа билета: ticketTypeId={}, eventId={}", ticketTypeId, eventId);

        // Проверяем возможность редактирования события
        Event event = findEventByIdForEdit(eventId, tenantId);

        // Находим тип билета
        TicketType ticketType = findTicketTypeById(ticketTypeId, eventId);

        // Для опубликованных событий проверяем ограничения по регистрациям
        int registrationCount = ticketType.getSoldCount() + ticketType.getReservedCount();
        if (event.isPublished() && registrationCount > 0) {
            // Нельзя уменьшать количество ниже проданного
            if (request.quantity() != null && request.quantity() < registrationCount) {
                log.warn("Попытка уменьшить количество билетов ниже проданного: "
                    + "ticketTypeId={}, requested={}, sold={}",
                    ticketTypeId, request.quantity(), registrationCount);
                throw new TicketTypeHasRegistrationsException(ticketTypeId, registrationCount);
            }
        }

        // Обновляем только переданные поля
        if (request.name() != null) {
            ticketType.updateInfo(request.name(), ticketType.getDescription());
        }
        if (request.description() != null) {
            ticketType.setDescription(request.description());
        }
        if (request.quantity() != null) {
            ticketType.updateQuantity(request.quantity());
        }
        if (request.salesStart() != null || request.salesEnd() != null) {
            ticketType.updateSalesPeriod(
                request.salesStart() != null ? request.salesStart() : ticketType.getSalesStart(),
                request.salesEnd() != null ? request.salesEnd() : ticketType.getSalesEnd()
            );
        }
        if (request.sortOrder() != null) {
            ticketType.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            if (request.isActive()) {
                ticketType.activate();
            } else {
                ticketType.deactivate();
            }
        }

        ticketType = ticketTypeRepository.save(ticketType);

        log.info("Тип билета обновлён: ticketTypeId={}, eventId={}", ticketTypeId, eventId);

        return ticketTypeMapper.toDto(ticketType);
    }

    /**
     * Удаляет тип билета.
     * Если есть регистрации — выбрасывает исключение, предлагая деактивацию.
     *
     * @param eventId      идентификатор события
     * @param ticketTypeId идентификатор типа билета
     * @throws TicketTypeNotFoundException        если тип билета не найден
     * @throws TicketTypeHasRegistrationsException если есть регистрации
     * @throws EventNotEditableException          если событие нельзя редактировать
     */
    @Transactional
    public void delete(UUID eventId, UUID ticketTypeId) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Удаление типа билета: ticketTypeId={}, eventId={}", ticketTypeId, eventId);

        // Проверяем возможность редактирования события
        findEventByIdForEdit(eventId, tenantId);

        // Находим тип билета
        TicketType ticketType = findTicketTypeById(ticketTypeId, eventId);

        // Проверяем наличие регистраций
        int registrationCount = ticketType.getSoldCount() + ticketType.getReservedCount();
        if (registrationCount > 0) {
            throw new TicketTypeHasRegistrationsException(ticketTypeId, registrationCount);
        }

        ticketTypeRepository.delete(ticketType);

        log.info("Тип билета удалён: ticketTypeId={}, eventId={}", ticketTypeId, eventId);
    }

    /**
     * Деактивирует тип билета (вместо удаления).
     *
     * @param eventId      идентификатор события
     * @param ticketTypeId идентификатор типа билета
     * @return деактивированный тип билета
     */
    @Transactional
    public TicketTypeDto deactivate(UUID eventId, UUID ticketTypeId) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Деактивация типа билета: ticketTypeId={}, eventId={}", ticketTypeId, eventId);

        // Проверяем возможность редактирования события
        findEventByIdForEdit(eventId, tenantId);

        // Находим и деактивируем тип билета
        TicketType ticketType = findTicketTypeById(ticketTypeId, eventId);
        ticketType.deactivate();
        ticketType = ticketTypeRepository.save(ticketType);

        log.info("Тип билета деактивирован: ticketTypeId={}, eventId={}", ticketTypeId, eventId);

        return ticketTypeMapper.toDto(ticketType);
    }

    // ==================== Списки ====================

    /**
     * Возвращает все типы билетов события.
     * Для организатора — все типы, включая неактивные.
     *
     * @param eventId идентификатор события
     * @return список типов билетов
     */
    @Transactional(readOnly = true)
    public List<TicketTypeDto> findAllByEventId(UUID eventId) {
        // Проверяем доступ к событию
        verifyEventAccess(eventId);

        List<TicketType> ticketTypes = ticketTypeRepository.findByEventIdOrderBySortOrderAsc(eventId);
        return ticketTypes.stream()
            .map(ticketTypeMapper::toDto)
            .toList();
    }

    /**
     * Возвращает активные типы билетов события.
     * Для публичного отображения — фильтрует по активности и периоду продаж.
     *
     * @param eventId идентификатор события
     * @return список активных типов билетов с открытыми продажами
     */
    @Transactional(readOnly = true)
    public List<TicketTypeDto> findActiveByEventId(UUID eventId) {
        List<TicketType> ticketTypes = ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId);
        return ticketTypes.stream()
            .filter(TicketType::isSalesOpen)  // Фильтруем по периоду продаж
            .map(ticketTypeMapper::toDto)
            .toList();
    }

    /**
     * Возвращает типы билетов для публичного события по slug.
     * Не требует tenant context. Показывает только активные типы в период продаж.
     *
     * @param slug URL-slug события
     * @return список доступных типов билетов
     * @throws EventNotFoundException если событие не найдено или не публичное
     */
    @Transactional(readOnly = true)
    public List<TicketTypeDto> findPublicByEventSlug(String slug) {
        log.debug("Получение типов билетов для публичного события: slug={}", slug);

        // Находим публичное событие
        Event event = eventRepository.findPublicBySlug(slug)
            .orElseThrow(() -> new EventNotFoundException(slug));

        // Возвращаем активные типы билетов в период продаж
        List<TicketType> ticketTypes = ticketTypeRepository
            .findByEventIdAndActiveIsTrueOrderBySortOrderAsc(event.getId());
        return ticketTypes.stream()
            .filter(TicketType::isSalesOpen)  // Фильтруем по периоду продаж
            .map(ticketTypeMapper::toDto)
            .toList();
    }

    // ==================== Вспомогательные ====================

    /**
     * Находит событие по ID и проверяет возможность редактирования.
     * Defense in depth: проверка tenant_id на уровне приложения.
     *
     * @param eventId  идентификатор события
     * @param tenantId идентификатор организации
     * @return событие
     * @throws EventNotFoundException    если событие не найдено
     * @throws EventNotEditableException если событие нельзя редактировать
     */
    private Event findEventByIdForEdit(UUID eventId, UUID tenantId) {
        Event event = eventRepository.findByIdAndTenantId(eventId, tenantId)
            .orElseThrow(() -> new EventNotFoundException(eventId, tenantId));

        if (!event.isEditable()) {
            throw new EventNotEditableException(eventId, event.getStatus());
        }

        return event;
    }

    /**
     * Проверяет доступ к событию (для чтения).
     * Defense in depth: проверка tenant_id на уровне приложения.
     *
     * @param eventId идентификатор события
     * @throws EventNotFoundException если событие не найдено или принадлежит другому tenant
     */
    private void verifyEventAccess(UUID eventId) {
        UUID tenantId = TenantContext.getTenantId();
        if (!eventRepository.findByIdAndTenantId(eventId, tenantId).isPresent()) {
            throw new EventNotFoundException(eventId, tenantId);
        }
    }

    /**
     * Находит тип билета по ID.
     *
     * @param ticketTypeId идентификатор типа билета
     * @param eventId      идентификатор события
     * @return тип билета
     * @throws TicketTypeNotFoundException если тип билета не найден
     */
    private TicketType findTicketTypeById(UUID ticketTypeId, UUID eventId) {
        return ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId)
            .orElseThrow(() -> new TicketTypeNotFoundException(ticketTypeId, eventId));
    }
}
