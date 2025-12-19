package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.user.api.dto.CreateGroupRequest;
import ru.aqstream.user.api.dto.GroupDto;
import ru.aqstream.user.api.dto.JoinGroupResponse;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.event.GroupCreatedEvent;
import ru.aqstream.user.api.event.GroupDeletedEvent;
import ru.aqstream.user.api.event.GroupMemberAddedEvent;
import ru.aqstream.user.api.event.GroupMemberRemovedEvent;
import ru.aqstream.user.api.exception.AlreadyGroupMemberException;
import ru.aqstream.user.api.exception.CannotRemoveGroupCreatorException;
import ru.aqstream.user.api.exception.GroupNotFoundException;
import ru.aqstream.user.api.exception.InsufficientGroupPermissionsException;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService")
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository orgMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private GroupMemberMapper memberMapper;

    @Mock
    private EventPublisher eventPublisher;

    private GroupService service;

    private static final Faker FAKER = new Faker();

    private UUID testUserId;
    private UUID testOrgId;
    private UUID testGroupId;
    private User testUser;
    private Organization testOrg;
    private Group testGroup;
    private OrganizationMember testOrgMember;

    @BeforeEach
    void setUp() {
        service = new GroupService(
            groupRepository,
            groupMemberRepository,
            organizationRepository,
            orgMemberRepository,
            userRepository,
            groupMapper,
            memberMapper,
            eventPublisher
        );

        testUserId = UUID.randomUUID();
        testOrgId = UUID.randomUUID();
        testGroupId = UUID.randomUUID();

        testUser = createTestUser(testUserId);
        testOrg = createTestOrganization(testOrgId, testUser);
        testGroup = createTestGroup(testGroupId, testOrg, testUser);
    }

    // ==================== Create ====================

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("OWNER может создать группу")
        void create_Owner_CreatesGroup() {
            // Arrange
            testOrgMember = createOrgMember(testOrg, testUser, OrganizationRole.OWNER);
            CreateGroupRequest request = new CreateGroupRequest(
                FAKER.company().name(),
                FAKER.lorem().paragraph()
            );

            when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(testOrg));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(testOrgMember));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(groupRepository.existsByInviteCode(any())).thenReturn(false);
            when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
                Group g = inv.getArgument(0);
                setId(g, testGroupId);
                return g;
            });
            when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));
            when(groupMapper.toDto(any(Group.class))).thenReturn(createGroupDto(testGroupId, request.name()));
            when(groupRepository.countMembersByGroupId(testGroupId)).thenReturn(1);

            // Act
            GroupDto result = service.create(testOrgId, testUserId, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(request.name());
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(eventPublisher).publish(any(GroupCreatedEvent.class));
        }

        @Test
        @DisplayName("MODERATOR может создать группу")
        void create_Moderator_CreatesGroup() {
            // Arrange
            testOrgMember = createOrgMember(testOrg, testUser, OrganizationRole.MODERATOR);
            CreateGroupRequest request = new CreateGroupRequest(
                FAKER.company().name(),
                null
            );

            when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(testOrg));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(testOrgMember));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(groupRepository.existsByInviteCode(any())).thenReturn(false);
            when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
                Group g = inv.getArgument(0);
                setId(g, testGroupId);
                return g;
            });
            when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));
            when(groupMapper.toDto(any(Group.class))).thenReturn(createGroupDto(testGroupId, request.name()));
            when(groupRepository.countMembersByGroupId(testGroupId)).thenReturn(1);

            // Act
            GroupDto result = service.create(testOrgId, testUserId, request);

            // Assert
            assertThat(result).isNotNull();
            verify(eventPublisher).publish(any(GroupCreatedEvent.class));
        }

        @Test
        @DisplayName("Обычный участник не может создать группу")
        void create_RegularMember_ThrowsException() {
            // Arrange
            // Создаём member без роли OWNER/MODERATOR (симулируем обычного участника)
            OrganizationMember regularMember = createOrgMember(testOrg, testUser, null);
            CreateGroupRequest request = new CreateGroupRequest(FAKER.company().name(), null);

            when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(testOrg));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(regularMember));

            // Act & Assert
            assertThatThrownBy(() -> service.create(testOrgId, testUserId, request))
                .isInstanceOf(InsufficientGroupPermissionsException.class);

            verify(groupRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }

    // ==================== Delete ====================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("OWNER может удалить группу")
        void delete_Owner_DeletesGroup() {
            // Arrange
            testOrgMember = createOrgMember(testOrg, testUser, OrganizationRole.OWNER);

            when(groupRepository.findByIdWithDetails(testGroupId)).thenReturn(Optional.of(testGroup));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(testOrgMember));

            // Act
            service.delete(testGroupId, testUserId);

            // Assert
            verify(groupMemberRepository).deleteByGroupId(testGroupId);
            verify(groupRepository).delete(testGroup);
            verify(eventPublisher).publish(any(GroupDeletedEvent.class));
        }

        @Test
        @DisplayName("MODERATOR не может удалить группу")
        void delete_Moderator_ThrowsException() {
            // Arrange
            testOrgMember = createOrgMember(testOrg, testUser, OrganizationRole.MODERATOR);

            when(groupRepository.findByIdWithDetails(testGroupId)).thenReturn(Optional.of(testGroup));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(testOrgMember));

            // Act & Assert
            assertThatThrownBy(() -> service.delete(testGroupId, testUserId))
                .isInstanceOf(InsufficientGroupPermissionsException.class);

            verify(groupRepository, never()).delete(any());
        }
    }

    // ==================== JoinByInviteCode ====================

    @Nested
    @DisplayName("joinByInviteCode")
    class JoinByInviteCode {

        @Test
        @DisplayName("Член организации может присоединиться к группе")
        void join_OrgMember_Joins() {
            // Arrange
            UUID newUserId = UUID.randomUUID();
            User newUser = createTestUser(newUserId);
            testOrgMember = createOrgMember(testOrg, newUser, OrganizationRole.MODERATOR);
            String inviteCode = "ABCD1234";

            when(groupRepository.findByInviteCode(inviteCode)).thenReturn(Optional.of(testGroup));
            when(userRepository.findById(newUserId)).thenReturn(Optional.of(newUser));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, newUserId))
                .thenReturn(Optional.of(testOrgMember));
            when(groupMemberRepository.existsByGroupIdAndUserId(testGroupId, newUserId)).thenReturn(false);
            when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            JoinGroupResponse response = service.joinByInviteCode(newUserId, inviteCode);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.groupId()).isEqualTo(testGroupId);
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(eventPublisher).publish(any(GroupMemberAddedEvent.class));
        }

        @Test
        @DisplayName("Нельзя присоединиться дважды")
        void join_AlreadyMember_ThrowsException() {
            // Arrange
            testOrgMember = createOrgMember(testOrg, testUser, OrganizationRole.MODERATOR);
            String inviteCode = "ABCD1234";

            when(groupRepository.findByInviteCode(inviteCode)).thenReturn(Optional.of(testGroup));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(testOrgMember));
            when(groupMemberRepository.existsByGroupIdAndUserId(testGroupId, testUserId)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> service.joinByInviteCode(testUserId, inviteCode))
                .isInstanceOf(AlreadyGroupMemberException.class);

            verify(groupMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Неверный инвайт-код выбрасывает исключение")
        void join_InvalidCode_ThrowsException() {
            // Arrange
            String invalidCode = "INVALID1";
            when(groupRepository.findByInviteCode(invalidCode)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.joinByInviteCode(testUserId, invalidCode))
                .isInstanceOf(GroupNotFoundException.class);
        }
    }

    // ==================== RemoveMember ====================

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("Можно удалить обычного участника")
        void removeMember_RegularMember_Success() {
            // Arrange
            UUID targetUserId = UUID.randomUUID();
            User targetUser = createTestUser(targetUserId);
            GroupMember targetMember = createGroupMember(testGroup, targetUser, testUser);
            testOrgMember = createOrgMember(testOrg, testUser, OrganizationRole.OWNER);

            when(groupRepository.findByIdWithDetails(testGroupId)).thenReturn(Optional.of(testGroup));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(testOrgMember));
            when(groupMemberRepository.findByGroupIdAndUserId(testGroupId, targetUserId))
                .thenReturn(Optional.of(targetMember));

            // Act
            service.removeMember(testGroupId, testUserId, targetUserId);

            // Assert
            verify(groupMemberRepository).delete(targetMember);
            verify(eventPublisher).publish(any(GroupMemberRemovedEvent.class));
        }

        @Test
        @DisplayName("Нельзя удалить создателя группы")
        void removeMember_Creator_ThrowsException() {
            // Arrange
            GroupMember creatorMember = createGroupMember(testGroup, testUser, null);
            testOrgMember = createOrgMember(testOrg, testUser, OrganizationRole.OWNER);

            when(groupRepository.findByIdWithDetails(testGroupId)).thenReturn(Optional.of(testGroup));
            when(orgMemberRepository.findByOrganizationIdAndUserId(testOrgId, testUserId))
                .thenReturn(Optional.of(testOrgMember));
            when(groupMemberRepository.findByGroupIdAndUserId(testGroupId, testUserId))
                .thenReturn(Optional.of(creatorMember));

            // Act & Assert
            assertThatThrownBy(() -> service.removeMember(testGroupId, testUserId, testUserId))
                .isInstanceOf(CannotRemoveGroupCreatorException.class);

            verify(groupMemberRepository, never()).delete(any());
        }
    }

    // ==================== Leave ====================

    @Nested
    @DisplayName("leave")
    class Leave {

        @Test
        @DisplayName("Участник может выйти из группы")
        void leave_RegularMember_Leaves() {
            // Arrange
            UUID memberUserId = UUID.randomUUID();
            User memberUser = createTestUser(memberUserId);
            GroupMember member = createGroupMember(testGroup, memberUser, testUser);

            when(groupRepository.findByIdWithDetails(testGroupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(testGroupId, memberUserId))
                .thenReturn(Optional.of(member));

            // Act
            service.leave(testGroupId, memberUserId);

            // Assert
            verify(groupMemberRepository).delete(member);

            ArgumentCaptor<GroupMemberRemovedEvent> captor = ArgumentCaptor.forClass(GroupMemberRemovedEvent.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().isSelfRemoval()).isTrue();
        }

        @Test
        @DisplayName("Создатель не может выйти из группы")
        void leave_Creator_ThrowsException() {
            // Arrange
            GroupMember creatorMember = createGroupMember(testGroup, testUser, null);

            when(groupRepository.findByIdWithDetails(testGroupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByGroupIdAndUserId(testGroupId, testUserId))
                .thenReturn(Optional.of(creatorMember));

            // Act & Assert
            assertThatThrownBy(() -> service.leave(testGroupId, testUserId))
                .isInstanceOf(CannotRemoveGroupCreatorException.class);

            verify(groupMemberRepository, never()).delete(any());
        }
    }

    // ==================== Helper Methods ====================

    private User createTestUser(UUID userId) {
        User user = User.createWithEmail(
            FAKER.internet().emailAddress(),
            FAKER.internet().password(),
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        setId(user, userId);
        return user;
    }

    private Organization createTestOrganization(UUID orgId, User owner) {
        Organization org = Organization.create(
            owner,
            FAKER.company().name(),
            FAKER.internet().slug(),
            FAKER.lorem().paragraph()
        );
        setId(org, orgId);
        return org;
    }

    private Group createTestGroup(UUID groupId, Organization org, User creator) {
        Group group = Group.create(
            org,
            creator,
            FAKER.company().name(),
            FAKER.lorem().paragraph()
        );
        setId(group, groupId);
        return group;
    }

    private OrganizationMember createOrgMember(Organization org, User user, OrganizationRole role) {
        if (role == OrganizationRole.OWNER) {
            return OrganizationMember.createOwner(org, user);
        } else if (role == OrganizationRole.MODERATOR) {
            return OrganizationMember.createModerator(org, user, null);
        }
        // Для обычного участника возвращаем null роль (не OWNER, не MODERATOR)
        OrganizationMember member = OrganizationMember.createModerator(org, user, null);
        // Меняем роль на null через reflection для теста
        try {
            java.lang.reflect.Field roleField = OrganizationMember.class.getDeclaredField("role");
            roleField.setAccessible(true);
            roleField.set(member, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return member;
    }

    private GroupMember createGroupMember(Group group, User user, User invitedBy) {
        if (invitedBy == null) {
            return GroupMember.createCreator(group, user);
        }
        return GroupMember.create(group, user, invitedBy);
    }

    private GroupDto createGroupDto(UUID groupId, String name) {
        return GroupDto.builder()
            .id(groupId)
            .organizationId(testOrgId)
            .organizationName(testOrg.getName())
            .name(name)
            .description(FAKER.lorem().paragraph())
            .inviteCode("ABCD1234")
            .createdById(testUserId)
            .createdByName(testUser.getFirstName())
            .memberCount(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private void setId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
