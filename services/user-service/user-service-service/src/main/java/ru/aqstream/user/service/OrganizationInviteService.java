package ru.aqstream.user.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.user.api.dto.InviteMemberRequest;
import ru.aqstream.user.api.dto.OrganizationInviteDto;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.exception.AlreadyOrganizationMemberException;
import ru.aqstream.user.api.exception.InsufficientOrganizationPermissionsException;
import ru.aqstream.user.api.exception.OrganizationInviteAlreadyUsedException;
import ru.aqstream.user.api.exception.OrganizationInviteExpiredException;
import ru.aqstream.user.api.exception.OrganizationInviteNotFoundException;
import ru.aqstream.user.api.exception.OrganizationMemberNotFoundException;
import ru.aqstream.user.api.exception.OrganizationNotFoundException;
import ru.aqstream.user.api.exception.UserNotFoundException;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationInvite;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис управления приглашениями в организации.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationInviteService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberMapper memberMapper;
    private final OrganizationInviteMapper inviteMapper;

    /**
     * Создаёт приглашение в организацию.
     * OWNER и MODERATOR могут приглашать.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @param request        данные приглашения
     * @return приглашение
     */
    @Transactional
    public OrganizationInviteDto createInvite(
        UUID organizationId,
        UUID userId,
        InviteMemberRequest request
    ) {
        log.info("Создание приглашения: organizationId={}, userId={}", organizationId, userId);

        Organization organization = findOrganizationById(organizationId);
        OrganizationMember actor = checkMembership(organizationId, userId);
        User inviter = findUserById(userId);

        // OWNER и MODERATOR могут приглашать
        if (!actor.isOwner() && !actor.isModerator()) {
            throw new InsufficientOrganizationPermissionsException("приглашение членов", "MODERATOR");
        }

        OrganizationInvite invite = OrganizationInvite.create(
            organization,
            inviter,
            request.telegramUsername()
        );
        invite = inviteRepository.save(invite);

        log.info("Приглашение создано: inviteId={}, inviteCode={}",
            invite.getId(), invite.getInviteCode());

        return inviteMapper.toDto(invite);
    }

    /**
     * Возвращает активные приглашения организации.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @return список приглашений
     */
    @Transactional(readOnly = true)
    public List<OrganizationInviteDto> getActiveInvites(UUID organizationId, UUID userId) {
        checkMembership(organizationId, userId);

        List<OrganizationInvite> invites = inviteRepository.findActiveByOrganizationId(
            organizationId, Instant.now()
        );

        return invites.stream()
            .map(inviteMapper::toDto)
            .toList();
    }

    /**
     * Принимает приглашение.
     *
     * @param userId     идентификатор пользователя
     * @param inviteCode код приглашения
     * @return членство в организации
     */
    @Transactional
    public OrganizationMemberDto acceptInvite(UUID userId, String inviteCode) {
        log.info("Принятие приглашения: userId={}, inviteCode={}", userId, inviteCode);

        OrganizationInvite invite = inviteRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new OrganizationInviteNotFoundException(inviteCode));

        // Проверяем валидность
        if (invite.isExpired()) {
            throw new OrganizationInviteExpiredException();
        }
        if (invite.isUsed()) {
            throw new OrganizationInviteAlreadyUsedException();
        }

        User user = findUserById(userId);
        Organization organization = invite.getOrganization();

        // Проверяем, не является ли пользователь уже членом
        if (memberRepository.existsByOrganizationIdAndUserId(organization.getId(), userId)) {
            throw new AlreadyOrganizationMemberException(organization.getId(), userId);
        }

        // Отмечаем приглашение как использованное
        invite.markAsUsed(user);
        inviteRepository.save(invite);

        // Создаём членство
        OrganizationMember member = OrganizationMember.createModerator(
            organization,
            user,
            invite.getInvitedBy()
        );
        member = memberRepository.save(member);

        log.info("Приглашение принято: userId={}, organizationId={}", userId, organization.getId());

        return memberMapper.toDto(member);
    }

    // ==================== Вспомогательные методы ====================

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Organization findOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private OrganizationMember checkMembership(UUID organizationId, UUID userId) {
        return memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new OrganizationMemberNotFoundException(organizationId, userId));
    }
}
