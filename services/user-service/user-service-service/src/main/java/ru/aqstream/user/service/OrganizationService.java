package ru.aqstream.user.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TokenHasher;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.CreateOrganizationRequest;
import ru.aqstream.user.api.dto.OrganizationDto;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.dto.UpdateMemberRoleRequest;
import ru.aqstream.user.api.dto.UpdateOrganizationRequest;
import ru.aqstream.user.api.exception.CannotRemoveOwnerException;
import ru.aqstream.user.api.exception.InsufficientOrganizationPermissionsException;
import ru.aqstream.user.api.exception.NoApprovedRequestException;
import ru.aqstream.user.api.exception.OrganizationMemberNotFoundException;
import ru.aqstream.user.api.exception.OrganizationNotFoundException;
import ru.aqstream.user.api.exception.OrganizationSlugAlreadyExistsException;
import ru.aqstream.user.api.exception.SlugReservationExpiredException;
import ru.aqstream.user.api.exception.UserNotFoundException;
import ru.aqstream.user.api.event.OrganizationDeletedEvent;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.OrganizationRequest;
import ru.aqstream.user.db.entity.RefreshToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис управления организациями.
 * CRUD организаций, управление членами, приглашения, переключение.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private static final Set<String> DEFAULT_USER_ROLES = Set.of("USER");

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper memberMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

    @Value("${jwt.access-token-expiration:15m}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:7d}")
    private Duration refreshTokenExpiration;

    // ==================== CRUD Организаций ====================

    /**
     * Создаёт организацию.
     * Требуется одобренный OrganizationRequest — slug берётся из него.
     *
     * @param userId  идентификатор пользователя
     * @param request данные для создания
     * @return созданная организация
     */
    @Transactional
    public OrganizationDto create(UUID userId, CreateOrganizationRequest request) {
        log.info("Создание организации: userId={}, name={}", userId, request.name());

        User user = findUserById(userId);

        // Находим одобренный запрос пользователя
        OrganizationRequest approvedRequest = requestRepository.findApprovedByUserId(userId)
            .orElseThrow(NoApprovedRequestException::new);

        String slug = approvedRequest.getSlug();

        // Проверяем, не истёк ли срок резервации slug (7 дней после одобрения)
        if (approvedRequest.isSlugReservationExpired()) {
            log.info("Срок резервации slug истёк: userId={}, slug={}", userId, slug);
            throw new SlugReservationExpiredException(slug);
        }

        // Проверяем уникальность slug среди организаций
        if (organizationRepository.existsBySlug(slug)) {
            throw new OrganizationSlugAlreadyExistsException(slug);
        }

        // Создаём организацию
        Organization organization = Organization.create(
            user,
            request.name(),
            slug,
            request.description()
        );
        organization = organizationRepository.save(organization);

        // Создаём membership для владельца
        OrganizationMember ownerMember = OrganizationMember.createOwner(organization, user);
        memberRepository.save(ownerMember);

        log.info("Организация создана: organizationId={}, slug={}, ownerId={}",
            organization.getId(), slug, userId);

        return organizationMapper.toDto(organization);
    }

    /**
     * Возвращает список организаций пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список организаций
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getMyOrganizations(UUID userId) {
        List<OrganizationMember> memberships = memberRepository.findByUserId(userId);
        return memberships.stream()
            .map(member -> organizationMapper.toDto(member.getOrganization()))
            .toList();
    }

    /**
     * Возвращает организацию по ID.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя (для проверки членства)
     * @return организация
     */
    @Transactional(readOnly = true)
    public OrganizationDto getById(UUID organizationId, UUID userId) {
        Organization organization = findOrganizationWithOwner(organizationId);
        checkMembership(organizationId, userId);
        return organizationMapper.toDto(organization);
    }

    /**
     * Обновляет организацию.
     * Только OWNER или MODERATOR.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @param request        данные для обновления
     * @return обновлённая организация
     */
    @Transactional
    public OrganizationDto update(UUID organizationId, UUID userId, UpdateOrganizationRequest request) {
        log.info("Обновление организации: organizationId={}, userId={}", organizationId, userId);

        Organization organization = findOrganizationWithOwner(organizationId);
        OrganizationMember member = checkMembership(organizationId, userId);

        // Только OWNER или MODERATOR может редактировать
        if (!member.isOwner() && !member.isModerator()) {
            throw new InsufficientOrganizationPermissionsException("редактирование организации", "MODERATOR");
        }

        organization.updateInfo(request.name(), request.description());
        if (request.logoUrl() != null) {
            organization.updateLogo(request.logoUrl());
        }

        organization = organizationRepository.save(organization);

        log.info("Организация обновлена: organizationId={}", organizationId);

        return organizationMapper.toDto(organization);
    }

    /**
     * Удаляет организацию (soft delete).
     * Только OWNER.
     * Публикует событие organization.deleted для архивирования связанных данных.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     */
    @Transactional
    public void delete(UUID organizationId, UUID userId) {
        log.info("Удаление организации: organizationId={}, userId={}", organizationId, userId);

        Organization organization = findOrganizationWithOwner(organizationId);
        OrganizationMember member = checkMembership(organizationId, userId);

        // Только OWNER может удалить
        if (!member.isOwner()) {
            throw new InsufficientOrganizationPermissionsException("удаление организации", "OWNER");
        }

        organization.softDelete();
        organizationRepository.save(organization);

        // Публикуем событие для архивирования связанных данных (события и т.д.)
        eventPublisher.publish(new OrganizationDeletedEvent(
            organizationId,
            organization.getName(),
            userId
        ));

        log.info("Организация удалена: organizationId={}", organizationId);
    }

    // ==================== Переключение организаций ====================

    /**
     * Переключает на другую организацию.
     * Возвращает новые токены с tenantId = organizationId.
     *
     * @param userId         идентификатор пользователя
     * @param organizationId идентификатор организации для переключения
     * @param userAgent      User-Agent клиента
     * @param ipAddress      IP адрес клиента
     * @return токены с новым tenantId
     */
    @Transactional
    public AuthResponse switchOrganization(
        UUID userId,
        UUID organizationId,
        String userAgent,
        String ipAddress
    ) {
        log.info("Переключение организации: userId={}, organizationId={}", userId, organizationId);

        User user = findUserById(userId);
        checkMembership(organizationId, userId);

        // Генерируем новые токены с tenantId = organizationId
        Set<String> roles = user.isAdmin()
            ? Set.of("USER", "ADMIN")
            : DEFAULT_USER_ROLES;

        UserPrincipal principal = new UserPrincipal(
            user.getId(),
            user.getEmail(),
            organizationId,  // tenantId = organizationId
            roles
        );

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Сохраняем refresh token
        RefreshToken tokenEntity = RefreshToken.create(
            user,
            TokenHasher.hash(refreshToken),
            Instant.now().plus(refreshTokenExpiration),
            userAgent,
            ipAddress
        );
        refreshTokenRepository.save(tokenEntity);

        log.info("Переключение выполнено: userId={}, newTenantId={}", userId, organizationId);

        return AuthResponse.bearer(
            accessToken,
            refreshToken,
            accessTokenExpiration.toSeconds(),
            userMapper.toDto(user)
        );
    }

    // ==================== Управление членами ====================

    /**
     * Возвращает список членов организации.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @return список членов
     */
    @Transactional(readOnly = true)
    public List<OrganizationMemberDto> getMembers(UUID organizationId, UUID userId) {
        checkMembership(organizationId, userId);

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        return members.stream()
            .map(memberMapper::toDto)
            .toList();
    }

    /**
     * Возвращает членство текущего пользователя в организации.
     * Используется для определения роли пользователя (OWNER/MODERATOR) на фронтенде.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @return членство пользователя
     */
    @Transactional(readOnly = true)
    public OrganizationMemberDto getMyMembership(UUID organizationId, UUID userId) {
        OrganizationMember member = checkMembership(organizationId, userId);
        return memberMapper.toDto(member);
    }

    /**
     * Изменяет роль члена организации.
     * Только OWNER может изменять роли.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя (кто изменяет)
     * @param targetUserId   идентификатор пользователя (чья роль меняется)
     * @param request        новая роль
     * @return обновлённый член
     */
    @Transactional
    public OrganizationMemberDto updateMemberRole(
        UUID organizationId,
        UUID userId,
        UUID targetUserId,
        UpdateMemberRoleRequest request
    ) {
        log.info("Изменение роли: organizationId={}, userId={}, targetUserId={}, newRole={}",
            organizationId, userId, targetUserId, request.role());

        OrganizationMember actor = checkMembership(organizationId, userId);

        // Только OWNER может изменять роли
        if (!actor.isOwner()) {
            throw new InsufficientOrganizationPermissionsException("изменение роли члена", "OWNER");
        }

        // OWNER не может изменить свою роль (чтобы не потерять контроль)
        if (userId.equals(targetUserId) && request.role() != OrganizationRole.OWNER) {
            throw new InsufficientOrganizationPermissionsException(
                "изменение собственной роли владельца"
            );
        }

        OrganizationMember targetMember = memberRepository
            .findByOrganizationIdAndUserId(organizationId, targetUserId)
            .orElseThrow(() -> new OrganizationMemberNotFoundException(organizationId, targetUserId));

        // Если назначаем нового OWNER, нужно понизить текущего до MODERATOR
        if (request.role() == OrganizationRole.OWNER && !targetMember.isOwner()) {
            actor.setRole(OrganizationRole.MODERATOR);
            memberRepository.save(actor);
        }

        targetMember.setRole(request.role());
        targetMember = memberRepository.save(targetMember);

        log.info("Роль изменена: targetUserId={}, newRole={}", targetUserId, request.role());

        return memberMapper.toDto(targetMember);
    }

    /**
     * Удаляет члена из организации.
     * OWNER и MODERATOR могут удалять членов.
     * OWNER не может быть удалён.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя (кто удаляет)
     * @param targetUserId   идентификатор пользователя (кого удаляют)
     */
    @Transactional
    public void removeMember(UUID organizationId, UUID userId, UUID targetUserId) {
        log.info("Удаление члена: organizationId={}, userId={}, targetUserId={}",
            organizationId, userId, targetUserId);

        OrganizationMember actor = checkMembership(organizationId, userId);
        OrganizationMember targetMember = memberRepository
            .findByOrganizationIdAndUserId(organizationId, targetUserId)
            .orElseThrow(() -> new OrganizationMemberNotFoundException(organizationId, targetUserId));

        // OWNER не может быть удалён
        if (targetMember.isOwner()) {
            throw new CannotRemoveOwnerException();
        }

        // Для удаления других членов нужна роль OWNER или MODERATOR
        if (!actor.isOwner() && !actor.isModerator()) {
            throw new InsufficientOrganizationPermissionsException("удаление члена организации", "MODERATOR");
        }

        memberRepository.delete(targetMember);

        log.info("Член удалён: targetUserId={}", targetUserId);
    }

    // ==================== Internal API ====================

    /**
     * Находит организацию по ID для внутреннего использования (без проверки членства).
     * Используется event-service для получения названия организатора.
     *
     * @param organizationId идентификатор организации
     * @return Optional с DTO организации
     */
    @Transactional(readOnly = true)
    public java.util.Optional<OrganizationDto> findByIdInternal(UUID organizationId) {
        return organizationRepository.findByIdWithOwner(organizationId)
            .map(organizationMapper::toDto);
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Находит пользователя по ID.
     */
    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Находит организацию по ID.
     */
    private Organization findOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    /**
     * Находит организацию по ID с загруженным владельцем.
     */
    private Organization findOrganizationWithOwner(UUID organizationId) {
        return organizationRepository.findByIdWithOwner(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    /**
     * Проверяет членство пользователя в организации.
     *
     * @return членство пользователя
     * @throws OrganizationMemberNotFoundException если пользователь не член организации
     */
    private OrganizationMember checkMembership(UUID organizationId, UUID userId) {
        return memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new OrganizationMemberNotFoundException(organizationId, userId));
    }
}
