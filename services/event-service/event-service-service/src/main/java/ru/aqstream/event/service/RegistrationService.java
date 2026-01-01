package ru.aqstream.event.service;

import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.event.api.dto.CancelRegistrationRequest;
import ru.aqstream.event.api.dto.CreateRegistrationRequest;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.api.exception.EventRegistrationClosedException;
import ru.aqstream.event.api.exception.PrivateEventAccessDeniedException;
import ru.aqstream.event.api.exception.RegistrationAccessDeniedException;
import ru.aqstream.event.api.exception.RegistrationAlreadyExistsException;
import ru.aqstream.event.api.exception.RegistrationNotCancellableException;
import ru.aqstream.event.api.exception.RegistrationNotFoundException;
import ru.aqstream.event.api.exception.TicketTypeNotFoundException;
import ru.aqstream.event.api.exception.TicketTypeSalesNotOpenException;
import ru.aqstream.event.api.exception.TicketTypeSoldOutException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.user.client.UserClient;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

/**
 * Сервис управления регистрациями на события.
 * CRUD операции и бизнес-логика регистраций.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    /**
     * Символы для генерации confirmation code (без похожих: 0,O,I,L,1).
     */
    private static final String CONFIRMATION_CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CONFIRMATION_CODE_LENGTH = 8;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final RegistrationMapper registrationMapper;
    private final RegistrationEventPublisher registrationEventPublisher;
    private final UserClient userClient;
    private final SecureRandom secureRandom = new SecureRandom();

    // ==================== Создание регистрации ====================

    /**
     * Регистрирует пользователя на событие.
     *
     * @param eventId   идентификатор события
     * @param request   данные регистрации
     * @param principal авторизованный пользователь
     * @return созданная регистрация
     * @throws EventNotFoundException           если событие не найдено
     * @throws EventRegistrationClosedException если регистрация закрыта
     * @throws TicketTypeNotFoundException      если тип билета не найден
     * @throws TicketTypeSalesNotOpenException  если продажи типа билета закрыты
     * @throws TicketTypeSoldOutException       если билеты распроданы
     * @throws RegistrationAlreadyExistsException если пользователь уже зарегистрирован
     */
    @Transactional
    public RegistrationDto create(UUID eventId, CreateRegistrationRequest request, UserPrincipal principal) {
        UUID userId = principal.userId();
        UUID tenantId = TenantContext.getTenantId();
        log.info("Создание регистрации: eventId={}, userId={}, ticketTypeId={}",
            eventId, userId, request.ticketTypeId());

        // Находим событие и проверяем возможность регистрации
        Event event = findEventForRegistration(eventId, tenantId);

        // Для приватных событий проверяем членство в группе
        if (event.getGroupId() != null) {
            boolean isMember = userClient.isGroupMember(event.getGroupId(), userId);
            if (!isMember) {
                log.warn("Попытка регистрации на приватное событие без членства в группе: "
                    + "eventId={}, groupId={}, userId={}", eventId, event.getGroupId(), userId);
                throw new PrivateEventAccessDeniedException(eventId, event.getGroupId(), userId);
            }
        }

        // Проверяем, не зарегистрирован ли пользователь уже
        if (registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)) {
            throw new RegistrationAlreadyExistsException(eventId, userId);
        }

        // Находим и блокируем тип билета для обновления (pessimistic lock)
        TicketType ticketType = ticketTypeRepository.findByIdAndEventIdForUpdate(request.ticketTypeId(), eventId)
            .orElseThrow(() -> new TicketTypeNotFoundException(request.ticketTypeId(), eventId));

        // Проверяем доступность типа билета
        validateTicketTypeForRegistration(ticketType);

        // Генерируем уникальный confirmation code
        String confirmationCode = generateUniqueConfirmationCode();

        // Создаём регистрацию
        Registration registration = Registration.create(
            event,
            ticketType,
            userId,
            confirmationCode,
            request.firstName(),
            request.lastName(),
            request.email()
        );
        registration.setCustomFields(request.customFieldsOrDefault());

        // Увеличиваем счётчик проданных билетов
        ticketType.incrementSoldCount();
        ticketTypeRepository.save(ticketType);

        registration = registrationRepository.save(registration);

        log.info("Регистрация создана: registrationId={}, eventId={}, userId={}, confirmationCode={}",
            registration.getId(), eventId, userId, confirmationCode);

        // Публикуем событие в RabbitMQ для отправки уведомления
        registrationEventPublisher.publishCreated(registration);

        return registrationMapper.toDto(registration);
    }

    // ==================== Просмотр регистраций ====================

    /**
     * Возвращает регистрацию по ID.
     *
     * @param registrationId идентификатор регистрации
     * @param principal      авторизованный пользователь
     * @return регистрация
     * @throws RegistrationNotFoundException   если регистрация не найдена
     * @throws RegistrationAccessDeniedException если нет доступа
     */
    @Transactional(readOnly = true)
    public RegistrationDto getById(UUID registrationId, UserPrincipal principal) {
        UUID userId = principal.userId();
        UUID tenantId = TenantContext.getTenantId();

        Registration registration = registrationRepository.findByIdAndTenantId(registrationId, tenantId)
            .orElseThrow(() -> new RegistrationNotFoundException(registrationId, tenantId));

        // Проверяем, что это регистрация текущего пользователя или он организатор
        if (!registration.getUserId().equals(userId) && !isOrganizer(principal)) {
            throw new RegistrationAccessDeniedException(registrationId, userId);
        }

        return registrationMapper.toDto(registration);
    }

    /**
     * Возвращает список регистраций текущего пользователя.
     *
     * @param principal авторизованный пользователь
     * @param pageable  параметры пагинации
     * @return страница регистраций
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationDto> getMyRegistrations(UserPrincipal principal, Pageable pageable) {
        UUID userId = principal.userId();
        log.debug("Получение регистраций пользователя: userId={}", userId);

        Page<Registration> page = registrationRepository.findActiveByUserId(userId, pageable);
        return PageResponse.of(page, registrationMapper::toDto);
    }

    // ==================== Отмена регистрации ====================

    /**
     * Отменяет регистрацию участником.
     *
     * @param registrationId идентификатор регистрации
     * @param principal      авторизованный пользователь
     * @throws RegistrationNotFoundException     если регистрация не найдена
     * @throws RegistrationAccessDeniedException если нет доступа
     * @throws RegistrationNotCancellableException если регистрацию нельзя отменить
     */
    @Transactional
    public void cancel(UUID registrationId, UserPrincipal principal) {
        UUID userId = principal.userId();
        log.info("Отмена регистрации участником: registrationId={}, userId={}", registrationId, userId);

        Registration registration = findRegistrationForCancellation(registrationId, userId);

        // Проверяем, что это регистрация текущего пользователя
        if (!registration.getUserId().equals(userId)) {
            throw new RegistrationAccessDeniedException(registrationId, userId);
        }

        cancelRegistrationInternal(registration, null, false);
    }

    /**
     * Запрашивает повторную отправку билета в Telegram.
     *
     * @param registrationId идентификатор регистрации
     * @param principal      авторизованный пользователь
     * @throws RegistrationNotFoundException     если регистрация не найдена
     * @throws RegistrationAccessDeniedException если нет доступа
     */
    @Transactional
    public void resendTicket(UUID registrationId, UserPrincipal principal) {
        UUID userId = principal.userId();
        UUID tenantId = TenantContext.getTenantId();
        log.info("Запрос повторной отправки билета: registrationId={}, userId={}", registrationId, userId);

        Registration registration = registrationRepository.findByIdAndTenantId(registrationId, tenantId)
            .orElseThrow(() -> new RegistrationNotFoundException(registrationId, tenantId));

        // Проверяем, что это регистрация текущего пользователя
        if (!registration.getUserId().equals(userId)) {
            throw new RegistrationAccessDeniedException(registrationId, userId);
        }

        // Проверяем, что регистрация активна (CONFIRMED или CHECKED_IN)
        RegistrationStatus status = registration.getStatus();
        if (status != RegistrationStatus.CONFIRMED && status != RegistrationStatus.CHECKED_IN) {
            throw new RegistrationNotCancellableException(registrationId, status);
        }

        // Публикуем событие для отправки билета
        registrationEventPublisher.publishResendRequested(registration);

        log.info("Запрос на повторную отправку билета отправлен: registrationId={}", registrationId);
    }

    /**
     * Отменяет регистрацию организатором.
     *
     * @param eventId        идентификатор события
     * @param registrationId идентификатор регистрации
     * @param request        запрос с причиной отмены
     * @throws RegistrationNotFoundException     если регистрация не найдена
     * @throws RegistrationNotCancellableException если регистрацию нельзя отменить
     */
    @Transactional
    public void cancelByOrganizer(UUID eventId, UUID registrationId, CancelRegistrationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        log.info("Отмена регистрации организатором: registrationId={}, eventId={}", registrationId, eventId);

        // Проверяем доступ к событию
        verifyEventAccess(eventId, tenantId);

        Registration registration = registrationRepository.findByIdAndTenantId(registrationId, tenantId)
            .orElseThrow(() -> new RegistrationNotFoundException(registrationId, tenantId));

        // Проверяем, что регистрация принадлежит этому событию
        if (!registration.getEvent().getId().equals(eventId)) {
            throw new RegistrationNotFoundException(registrationId, tenantId);
        }

        String reason = request != null ? request.reason() : null;
        cancelRegistrationInternal(registration, reason, true);
    }

    // ==================== Для организатора ====================

    /**
     * Возвращает список регистраций события.
     *
     * @param eventId  идентификатор события
     * @param pageable параметры пагинации
     * @return страница регистраций
     * @throws EventNotFoundException если событие не найдено
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationDto> getEventRegistrations(UUID eventId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        verifyEventAccess(eventId, tenantId);

        Page<Registration> page = registrationRepository.findByEventIdAndTenantId(eventId, tenantId, pageable);
        return PageResponse.of(page, registrationMapper::toDto);
    }

    /**
     * Возвращает список регистраций события с фильтром по статусу.
     *
     * @param eventId  идентификатор события
     * @param status   статус регистрации
     * @param pageable параметры пагинации
     * @return страница регистраций
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationDto> getEventRegistrationsByStatus(
            UUID eventId, RegistrationStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        verifyEventAccess(eventId, tenantId);

        Page<Registration> page = registrationRepository.findByEventIdAndTenantIdAndStatus(
            eventId, tenantId, status, pageable);
        return PageResponse.of(page, registrationMapper::toDto);
    }

    /**
     * Возвращает список регистраций события с фильтром по типу билета.
     *
     * @param eventId      идентификатор события
     * @param ticketTypeId идентификатор типа билета
     * @param pageable     параметры пагинации
     * @return страница регистраций
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationDto> getEventRegistrationsByTicketType(
            UUID eventId, UUID ticketTypeId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        verifyEventAccess(eventId, tenantId);

        Page<Registration> page = registrationRepository.findByEventIdAndTenantIdAndTicketTypeId(
            eventId, tenantId, ticketTypeId, pageable);
        return PageResponse.of(page, registrationMapper::toDto);
    }

    /**
     * Поиск регистраций по имени или email.
     *
     * @param eventId  идентификатор события
     * @param query    строка поиска
     * @param pageable параметры пагинации
     * @return страница регистраций
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationDto> searchEventRegistrations(
            UUID eventId, String query, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        verifyEventAccess(eventId, tenantId);

        Page<Registration> page = registrationRepository.searchByEventIdAndTenantId(
            eventId, tenantId, query, pageable);
        return PageResponse.of(page, registrationMapper::toDto);
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Находит событие для регистрации с валидациями.
     */
    private Event findEventForRegistration(UUID eventId, UUID tenantId) {
        Event event = eventRepository.findByIdAndTenantId(eventId, tenantId)
            .orElseThrow(() -> new EventNotFoundException(eventId, tenantId));

        // Проверяем, что событие опубликовано
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new EventRegistrationClosedException(eventId,
                "Регистрация возможна только на опубликованные события");
        }

        // Проверяем, что регистрация открыта
        if (!event.isRegistrationOpen()) {
            throw new EventRegistrationClosedException(eventId);
        }

        return event;
    }

    /**
     * Проверяет доступность типа билета для регистрации.
     */
    private void validateTicketTypeForRegistration(TicketType ticketType) {
        // Проверяем, что продажи открыты
        if (!ticketType.isSalesOpen()) {
            throw new TicketTypeSalesNotOpenException(
                ticketType.getId(),
                ticketType.getSalesStart(),
                ticketType.getSalesEnd()
            );
        }

        // Проверяем, что билеты не распроданы
        if (ticketType.isSoldOut()) {
            throw new TicketTypeSoldOutException(ticketType.getId());
        }
    }

    /**
     * Генерирует уникальный confirmation code.
     */
    private String generateUniqueConfirmationCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            String code = generateConfirmationCode();
            if (!registrationRepository.existsByConfirmationCode(code)) {
                return code;
            }
        }
        // Крайне маловероятно, но на всякий случай
        throw new IllegalStateException("Не удалось сгенерировать уникальный код подтверждения");
    }

    /**
     * Генерирует случайный confirmation code (8 символов).
     */
    private String generateConfirmationCode() {
        StringBuilder sb = new StringBuilder(CONFIRMATION_CODE_LENGTH);
        for (int i = 0; i < CONFIRMATION_CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(CONFIRMATION_CODE_CHARS.length());
            sb.append(CONFIRMATION_CODE_CHARS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Находит регистрацию для отмены с валидациями.
     */
    private Registration findRegistrationForCancellation(UUID registrationId, UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        Registration registration = registrationRepository.findByIdAndTenantId(registrationId, tenantId)
            .orElseThrow(() -> new RegistrationNotFoundException(registrationId, tenantId));

        if (!registration.isCancellable()) {
            throw new RegistrationNotCancellableException(registrationId, registration.getStatus());
        }

        return registration;
    }

    /**
     * Внутренний метод отмены регистрации.
     */
    private void cancelRegistrationInternal(Registration registration, String reason, boolean byOrganizer) {
        UUID registrationId = registration.getId();

        // Проверяем, можно ли отменить
        if (!registration.isCancellable()) {
            throw new RegistrationNotCancellableException(registrationId, registration.getStatus());
        }

        // Уменьшаем счётчик проданных билетов
        TicketType ticketType = registration.getTicketType();
        ticketType.decrementSoldCount();
        ticketTypeRepository.save(ticketType);

        // Отменяем регистрацию
        if (byOrganizer) {
            registration.cancelByOrganizer(reason);
        } else {
            registration.cancel();
        }
        registrationRepository.save(registration);

        log.info("Регистрация отменена: registrationId={}, byOrganizer={}, reason={}",
            registrationId, byOrganizer, reason);

        // Публикуем событие в RabbitMQ
        registrationEventPublisher.publishCancelled(registration, byOrganizer);
    }

    /**
     * Проверяет доступ к событию (для организатора).
     */
    private void verifyEventAccess(UUID eventId, UUID tenantId) {
        if (eventRepository.findByIdAndTenantId(eventId, tenantId).isEmpty()) {
            throw new EventNotFoundException(eventId, tenantId);
        }
    }

    /**
     * Проверяет, является ли пользователь организатором.
     * Организатор — пользователь с ролью ADMIN или ORGANIZER.
     *
     * @param principal авторизованный пользователь
     * @return true если пользователь организатор
     */
    private boolean isOrganizer(UserPrincipal principal) {
        return principal.isOrganizer();
    }
}
