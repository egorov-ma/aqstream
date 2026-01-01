package ru.aqstream.user.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.user.api.dto.CreateGroupRequest;
import ru.aqstream.user.api.dto.GroupDto;
import ru.aqstream.user.api.dto.GroupMemberDto;
import ru.aqstream.user.api.dto.JoinGroupResponse;
import ru.aqstream.user.api.dto.UpdateGroupRequest;
import ru.aqstream.user.api.event.GroupCreatedEvent;
import ru.aqstream.user.api.event.GroupDeletedEvent;
import ru.aqstream.user.api.event.GroupMemberAddedEvent;
import ru.aqstream.user.api.event.GroupMemberRemovedEvent;
import ru.aqstream.user.api.event.GroupUpdatedEvent;
import ru.aqstream.user.api.exception.AlreadyGroupMemberException;
import ru.aqstream.user.api.exception.CannotRemoveGroupCreatorException;
import ru.aqstream.user.api.exception.GroupMemberNotFoundException;
import ru.aqstream.user.api.exception.GroupNotFoundException;
import ru.aqstream.user.api.exception.InsufficientGroupPermissionsException;
import ru.aqstream.user.api.exception.InviteCodeGenerationException;
import ru.aqstream.user.api.exception.OrganizationMemberNotFoundException;
import ru.aqstream.user.api.exception.OrganizationNotFoundException;
import ru.aqstream.user.api.exception.UserNotFoundException;
import ru.aqstream.user.api.util.InviteCodeGenerator;
import ru.aqstream.user.db.entity.Group;
import ru.aqstream.user.db.entity.GroupMember;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.GroupMemberRepository;
import ru.aqstream.user.db.repository.GroupRepository;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис для управления группами.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private static final int MAX_INVITE_CODE_GENERATION_ATTEMPTS = 5;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository orgMemberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final EventPublisher eventPublisher;

    // ==================== Мои группы ====================

    /**
     * Получает список групп, в которых состоит пользователь.
     *
     * @param userId идентификатор пользователя
     * @return список групп
     */
    @Transactional(readOnly = true)
    public List<GroupDto> getMyGroups(UUID userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);

        return memberships.stream()
            .map(membership -> toDtoWithMemberCount(membership.getGroup()))
            .toList();
    }

    // ==================== CRUD Групп ====================

    /**
     * Получает список групп организации.
     * OWNER/MODERATOR видят все группы, обычный участник — только свои.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @return список групп
     */
    @Transactional(readOnly = true)
    public List<GroupDto> getGroupsByOrganization(UUID organizationId, UUID userId) {
        OrganizationMember orgMember = checkOrganizationMembership(organizationId, userId);

        List<Group> groups;
        if (orgMember.isOwner() || orgMember.isModerator()) {
            // OWNER/MODERATOR видят все группы организации
            groups = groupRepository.findByOrganizationId(organizationId);
        } else {
            // Обычный участник видит только свои группы
            groups = groupRepository.findByOrganizationIdAndMemberUserId(organizationId, userId);
        }

        return groups.stream()
            .map(this::toDtoWithMemberCount)
            .toList();
    }

    /**
     * Создаёт новую группу.
     *
     * @param organizationId идентификатор организации
     * @param userId         идентификатор пользователя
     * @param request        данные для создания группы
     * @return созданная группа
     */
    @Transactional
    public GroupDto create(UUID organizationId, UUID userId, CreateGroupRequest request) {
        log.info("Создание группы: organizationId={}, userId={}, name={}", organizationId, userId, request.name());

        Organization organization = findOrganizationById(organizationId);
        OrganizationMember orgMember = checkOrganizationMembership(organizationId, userId);

        // Только OWNER или MODERATOR могут создавать группы
        if (!orgMember.isOwner() && !orgMember.isModerator()) {
            throw new InsufficientGroupPermissionsException("создание группы", "MODERATOR");
        }

        User creator = findUserById(userId);

        // Создаём группу с уникальным кодом (с retry при коллизии)
        Group group = createGroupWithUniqueCode(organization, creator, request);
        group = groupRepository.save(group);

        // Создатель автоматически становится участником
        GroupMember creatorMember = GroupMember.createCreator(group, creator);
        groupMemberRepository.save(creatorMember);

        eventPublisher.publish(new GroupCreatedEvent(
            group.getId(), organizationId, group.getName(), userId
        ));

        log.info("Группа создана: groupId={}, inviteCode={}", group.getId(), group.getInviteCode());

        return toDtoWithMemberCount(group);
    }

    /**
     * Получает группу по идентификатору.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     * @return группа
     */
    @Transactional(readOnly = true)
    public GroupDto getById(UUID groupId, UUID userId) {
        Group group = findGroupWithDetails(groupId);
        checkGroupAccess(group, userId);
        return toDtoWithMemberCount(group);
    }

    /**
     * Обновляет группу.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     * @param request данные для обновления
     * @return обновлённая группа
     */
    @Transactional
    public GroupDto update(UUID groupId, UUID userId, UpdateGroupRequest request) {
        log.info("Обновление группы: groupId={}, userId={}", groupId, userId);

        Group group = findGroupWithDetails(groupId);
        checkGroupManagementAccess(group, userId);

        group.updateInfo(request.name(), request.description());
        group = groupRepository.save(group);

        eventPublisher.publish(new GroupUpdatedEvent(
            group.getId(), group.getOrganizationId(), group.getName(), userId
        ));

        log.info("Группа обновлена: groupId={}", groupId);

        return toDtoWithMemberCount(group);
    }

    /**
     * Удаляет группу.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     */
    @Transactional
    public void delete(UUID groupId, UUID userId) {
        log.info("Удаление группы: groupId={}, userId={}", groupId, userId);

        Group group = findGroupWithDetails(groupId);
        OrganizationMember orgMember = checkOrganizationMembership(group.getOrganizationId(), userId);

        // Только OWNER может удалять группы
        if (!orgMember.isOwner()) {
            throw new InsufficientGroupPermissionsException("удаление группы", "OWNER");
        }

        UUID organizationId = group.getOrganizationId();

        // Удаляем всех участников, затем группу
        groupMemberRepository.deleteByGroupId(groupId);
        groupRepository.delete(group);

        eventPublisher.publish(new GroupDeletedEvent(groupId, organizationId, userId));

        log.info("Группа удалена: groupId={}", groupId);
    }

    /**
     * Регенерирует инвайт-код группы.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     * @return обновлённая группа
     */
    @Transactional
    public GroupDto regenerateInviteCode(UUID groupId, UUID userId) {
        log.info("Регенерация инвайт-кода: groupId={}, userId={}", groupId, userId);

        Group group = findGroupWithDetails(groupId);
        checkGroupManagementAccess(group, userId);

        // Генерируем новый уникальный код
        String newCode = generateUniqueInviteCode();
        group.setInviteCode(newCode);
        group = groupRepository.save(group);

        log.info("Инвайт-код регенерирован: groupId={}, newCode={}", groupId, newCode);

        return toDtoWithMemberCount(group);
    }

    // ==================== Управление участниками ====================

    /**
     * Получает список участников группы.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     * @return список участников
     */
    @Transactional(readOnly = true)
    public List<GroupMemberDto> getMembers(UUID groupId, UUID userId) {
        Group group = findGroupWithDetails(groupId);
        checkGroupAccess(group, userId);

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return members.stream()
            .map(memberMapper::toDto)
            .toList();
    }

    /**
     * Удаляет участника из группы.
     *
     * @param groupId      идентификатор группы
     * @param userId       идентификатор пользователя, выполняющего операцию
     * @param targetUserId идентификатор удаляемого участника
     */
    @Transactional
    public void removeMember(UUID groupId, UUID userId, UUID targetUserId) {
        log.info("Удаление участника: groupId={}, userId={}, targetUserId={}", groupId, userId, targetUserId);

        Group group = findGroupWithDetails(groupId);
        checkGroupManagementAccess(group, userId);

        GroupMember targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
            .orElseThrow(() -> new GroupMemberNotFoundException(groupId, targetUserId));

        // Нельзя удалить создателя группы
        if (targetMember.isCreator()) {
            throw new CannotRemoveGroupCreatorException();
        }

        groupMemberRepository.delete(targetMember);

        eventPublisher.publish(new GroupMemberRemovedEvent(
            groupId, targetUserId, userId, false
        ));

        log.info("Участник удалён: groupId={}, targetUserId={}", groupId, targetUserId);
    }

    /**
     * Выход участника из группы.
     *
     * @param groupId идентификатор группы
     * @param userId  идентификатор пользователя
     */
    @Transactional
    public void leave(UUID groupId, UUID userId) {
        log.info("Выход из группы: groupId={}, userId={}", groupId, userId);

        Group group = findGroupWithDetails(groupId);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new GroupMemberNotFoundException(groupId, userId));

        // Создатель не может выйти (должен передать права или удалить группу)
        if (member.isCreator()) {
            throw new CannotRemoveGroupCreatorException();
        }

        groupMemberRepository.delete(member);

        eventPublisher.publish(new GroupMemberRemovedEvent(
            groupId, userId, userId, true
        ));

        log.info("Пользователь вышел из группы: groupId={}, userId={}", groupId, userId);
    }

    // ==================== Присоединение по коду ====================

    /**
     * Присоединяется к группе по инвайт-коду.
     *
     * @param userId     идентификатор пользователя
     * @param inviteCode код приглашения
     * @return информация о присоединении
     */
    @Transactional
    public JoinGroupResponse joinByInviteCode(UUID userId, String inviteCode) {
        log.info("Присоединение по коду: userId={}, inviteCode={}", userId, inviteCode);

        Group group = groupRepository.findByInviteCode(inviteCode.toUpperCase())
            .orElseThrow(() -> new GroupNotFoundException(inviteCode));

        User user = findUserById(userId);

        // Проверяем, что пользователь является членом организации
        checkOrganizationMembership(group.getOrganizationId(), userId);

        // Проверяем, не является ли уже участником
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            throw new AlreadyGroupMemberException(group.getId(), userId);
        }

        // Добавляем в группу
        GroupMember member = GroupMember.create(group, user, null);
        groupMemberRepository.save(member);

        eventPublisher.publish(new GroupMemberAddedEvent(
            group.getId(), userId, null
        ));

        log.info("Пользователь присоединился: groupId={}, userId={}", group.getId(), userId);

        return JoinGroupResponse.builder()
            .groupId(group.getId())
            .groupName(group.getName())
            .organizationId(group.getOrganizationId())
            .organizationName(group.getOrganization().getName())
            .joinedAt(member.getJoinedAt())
            .build();
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

    private Group findGroupWithDetails(UUID groupId) {
        return groupRepository.findByIdWithDetails(groupId)
            .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    private OrganizationMember checkOrganizationMembership(UUID organizationId, UUID userId) {
        return orgMemberRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new OrganizationMemberNotFoundException(organizationId, userId));
    }

    private void checkGroupAccess(Group group, UUID userId) {
        OrganizationMember orgMember = checkOrganizationMembership(group.getOrganizationId(), userId);

        // OWNER/MODERATOR имеют доступ ко всем группам
        if (orgMember.isOwner() || orgMember.isModerator()) {
            return;
        }

        // Обычный пользователь должен быть участником группы
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            throw new GroupMemberNotFoundException(group.getId(), userId);
        }
    }

    private void checkGroupManagementAccess(Group group, UUID userId) {
        OrganizationMember orgMember = checkOrganizationMembership(group.getOrganizationId(), userId);

        // Только OWNER или MODERATOR могут управлять группами
        if (!orgMember.isOwner() && !orgMember.isModerator()) {
            throw new InsufficientGroupPermissionsException("управление группой", "MODERATOR");
        }
    }

    private Group createGroupWithUniqueCode(
        Organization organization, User creator, CreateGroupRequest request
    ) {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_GENERATION_ATTEMPTS; attempt++) {
            Group group = Group.create(organization, creator, request.name(), request.description());
            if (!groupRepository.existsByInviteCode(group.getInviteCode())) {
                return group;
            }
        }
        // Крайне маловероятная ситуация
        throw new InviteCodeGenerationException();
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = InviteCodeGenerator.generate();
            if (!groupRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new InviteCodeGenerationException();
    }

    private GroupDto toDtoWithMemberCount(Group group) {
        GroupDto dto = groupMapper.toDto(group);
        int memberCount = groupRepository.countMembersByGroupId(group.getId());
        return GroupDto.builder()
            .id(dto.id())
            .organizationId(dto.organizationId())
            .organizationName(dto.organizationName())
            .name(dto.name())
            .description(dto.description())
            .inviteCode(dto.inviteCode())
            .createdById(dto.createdById())
            .createdByName(dto.createdByName())
            .memberCount(memberCount)
            .createdAt(dto.createdAt())
            .updatedAt(dto.updatedAt())
            .build();
    }
}
