package ru.aqstream.user.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.user.api.dto.CreateOrganizationRequestRequest;
import ru.aqstream.user.api.dto.OrganizationRequestDto;
import ru.aqstream.user.api.dto.OrganizationRequestStatus;
import ru.aqstream.user.api.dto.RejectOrganizationRequestRequest;
import ru.aqstream.user.api.exception.AccessDeniedException;
import ru.aqstream.user.api.exception.OrganizationRequestAlreadyReviewedException;
import ru.aqstream.user.api.exception.OrganizationRequestNotFoundException;
import ru.aqstream.user.api.exception.PendingRequestAlreadyExistsException;
import ru.aqstream.user.api.exception.SlugAlreadyExistsException;
import ru.aqstream.user.api.exception.UserNotFoundException;
import ru.aqstream.user.db.entity.OrganizationRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис управления запросами на создание организаций.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationRequestService {

    private final OrganizationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final OrganizationRequestMapper requestMapper;

    /**
     * Создаёт запрос на создание организации.
     *
     * @param userId  идентификатор пользователя
     * @param request данные запроса
     * @return созданный запрос
     */
    @Transactional
    public OrganizationRequestDto create(UUID userId, CreateOrganizationRequestRequest request) {
        log.info("Создание запроса на организацию: userId={}, slug={}", userId, request.slug());

        // Проверяем, нет ли у пользователя активного запроса
        if (requestRepository.existsPendingByUserId(userId)) {
            log.debug("У пользователя уже есть активный запрос: userId={}", userId);
            throw new PendingRequestAlreadyExistsException();
        }

        // Проверяем уникальность slug
        String normalizedSlug = request.slug().toLowerCase().trim();
        if (requestRepository.existsPendingBySlug(normalizedSlug)) {
            log.debug("Slug уже используется в pending запросе: slug={}", normalizedSlug);
            throw new SlugAlreadyExistsException(normalizedSlug);
        }

        // TODO: проверить slug в таблице organizations когда она будет создана

        // Получаем пользователя
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Создаём запрос
        OrganizationRequest orgRequest = OrganizationRequest.create(
            user,
            request.name(),
            normalizedSlug,
            request.description()
        );
        orgRequest = requestRepository.save(orgRequest);

        log.info("Запрос на организацию создан: requestId={}, userId={}", orgRequest.getId(), userId);

        // TODO: Опубликовать событие organization.request.created для уведомления админов

        return requestMapper.toDto(orgRequest);
    }

    /**
     * Возвращает запрос по ID.
     *
     * @param requestId  идентификатор запроса
     * @param userId     идентификатор текущего пользователя
     * @param isAdmin    является ли пользователь админом
     * @return запрос
     */
    @Transactional(readOnly = true)
    public OrganizationRequestDto getById(UUID requestId, UUID userId, boolean isAdmin) {
        OrganizationRequest request = requestRepository.findByIdWithUser(requestId)
            .orElseThrow(() -> new OrganizationRequestNotFoundException(requestId));

        // Проверяем доступ: админ видит все, пользователь только свои
        if (!isAdmin && !request.getUserId().equals(userId)) {
            throw new AccessDeniedException("Доступ к запросу запрещён");
        }

        return requestMapper.toDto(request);
    }

    /**
     * Возвращает список запросов пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список запросов
     */
    @Transactional(readOnly = true)
    public List<OrganizationRequestDto> getByUser(UUID userId) {
        return requestRepository.findByUserId(userId).stream()
            .map(requestMapper::toDto)
            .toList();
    }

    /**
     * Возвращает список всех pending запросов (для админа).
     *
     * @param page номер страницы
     * @param size размер страницы
     * @return страница запросов
     */
    @Transactional(readOnly = true)
    public PageResponse<OrganizationRequestDto> getPendingRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<OrganizationRequest> requestPage = requestRepository.findPendingWithUser(pageable);

        return PageResponse.of(requestPage, requestMapper::toDto);
    }

    /**
     * Возвращает список всех запросов (для админа).
     *
     * @param page   номер страницы
     * @param size   размер страницы
     * @param status фильтр по статусу (nullable)
     * @return страница запросов
     */
    @Transactional(readOnly = true)
    public PageResponse<OrganizationRequestDto> getAllRequests(int page, int size, OrganizationRequestStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrganizationRequest> requestPage;
        if (status != null) {
            requestPage = requestRepository.findByStatus(status, pageable);
        } else {
            requestPage = requestRepository.findAllWithUser(pageable);
        }

        return PageResponse.of(requestPage, requestMapper::toDto);
    }

    /**
     * Одобряет запрос на создание организации.
     *
     * @param requestId идентификатор запроса
     * @param adminId   идентификатор администратора
     * @return обновлённый запрос
     */
    @Transactional
    public OrganizationRequestDto approve(UUID requestId, UUID adminId) {
        log.info("Одобрение запроса: requestId={}, adminId={}", requestId, adminId);

        OrganizationRequest request = requestRepository.findByIdWithUser(requestId)
            .orElseThrow(() -> new OrganizationRequestNotFoundException(requestId));

        if (!request.isPending()) {
            throw new OrganizationRequestAlreadyReviewedException(requestId, request.getStatus().name());
        }

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new UserNotFoundException(adminId));

        request.approve(admin);
        request = requestRepository.save(request);

        log.info("Запрос одобрен: requestId={}, userId={}", requestId, request.getUserId());

        // TODO: Опубликовать событие organization.request.approved для уведомления пользователя

        return requestMapper.toDto(request);
    }

    /**
     * Отклоняет запрос на создание организации.
     *
     * @param requestId идентификатор запроса
     * @param adminId   идентификатор администратора
     * @param request   причина отклонения
     * @return обновлённый запрос
     */
    @Transactional
    public OrganizationRequestDto reject(UUID requestId, UUID adminId, RejectOrganizationRequestRequest request) {
        log.info("Отклонение запроса: requestId={}, adminId={}", requestId, adminId);

        OrganizationRequest orgRequest = requestRepository.findByIdWithUser(requestId)
            .orElseThrow(() -> new OrganizationRequestNotFoundException(requestId));

        if (!orgRequest.isPending()) {
            throw new OrganizationRequestAlreadyReviewedException(requestId, orgRequest.getStatus().name());
        }

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new UserNotFoundException(adminId));

        orgRequest.reject(admin, request.comment());
        orgRequest = requestRepository.save(orgRequest);

        log.info("Запрос отклонён: requestId={}, userId={}", requestId, orgRequest.getUserId());

        // TODO: Опубликовать событие organization.request.rejected для уведомления пользователя

        return requestMapper.toDto(orgRequest);
    }

    /**
     * Проверяет, есть ли у пользователя одобренный запрос.
     *
     * @param userId идентификатор пользователя
     * @return true если есть одобренный запрос
     */
    @Transactional(readOnly = true)
    public boolean hasApprovedRequest(UUID userId) {
        return requestRepository.findApprovedByUserId(userId).isPresent();
    }
}
