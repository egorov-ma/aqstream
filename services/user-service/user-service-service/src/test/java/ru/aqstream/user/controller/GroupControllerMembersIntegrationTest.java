package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
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
 * Интеграционные тесты для управления участниками групп.
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("GroupController Members Integration Tests")
class GroupControllerMembersIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String GROUPS_URL = "/api/v1/groups";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository orgMemberRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private User owner;
    private User moderator;
    private User outsider;
    private Organization organization;

    @BeforeEach
    void setUp() {
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        orgMemberRepository.deleteAll();
        organizationRepository.deleteAll();

        owner = createUser();
        moderator = createUser();
        outsider = createUser();

        organization = createOrganization(owner);

        orgMemberRepository.save(OrganizationMember.createOwner(organization, owner));
        orgMemberRepository.save(OrganizationMember.createModerator(organization, moderator, owner));
    }

    private User createUser() {
        User user = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        return userRepository.save(user);
    }

    private Organization createOrganization(User createdBy) {
        Organization org = Organization.create(
            createdBy,
            FAKER.company().name(),
            FAKER.internet().slug().toLowerCase().replace("_", "-"),
            FAKER.company().catchPhrase()
        );
        return organizationRepository.save(org);
    }

    private Group createGroup(Organization org, User creator) {
        Group group = Group.create(
            org,
            creator,
            FAKER.company().name(),
            FAKER.lorem().paragraph()
        );
        group = groupRepository.save(group);
        groupMemberRepository.save(GroupMember.createCreator(group, creator));
        return group;
    }

    @Nested
    @DisplayName("GET /api/v1/groups/{id}/members")
    class GetGroupMembers {

        @Test
        @DisplayName("Участник группы видит список участников")
        void getMembers_GroupMember_ReturnsMemberList() throws Exception {
            Group group = createGroup(organization, owner);
            groupMemberRepository.save(GroupMember.create(group, moderator, owner));

            mockMvc.perform(get(GROUPS_URL + "/" + group.getId() + "/members")
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/groups/{id}/members/{userId}")
    class RemoveGroupMember {

        @Test
        @DisplayName("OWNER может удалить участника")
        void removeMember_Owner_ReturnsNoContent() throws Exception {
            Group group = createGroup(organization, owner);
            groupMemberRepository.save(GroupMember.create(group, moderator, owner));

            mockMvc.perform(delete(GROUPS_URL + "/" + group.getId() + "/members/" + moderator.getId())
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isNoContent());

            assertThat(groupMemberRepository.existsByGroupIdAndUserId(group.getId(), moderator.getId()))
                .isFalse();
        }

        @Test
        @DisplayName("Нельзя удалить создателя группы")
        void removeMember_CannotRemoveCreator_ReturnsConflict() throws Exception {
            Group group = createGroup(organization, moderator);

            mockMvc.perform(delete(GROUPS_URL + "/" + group.getId() + "/members/" + moderator.getId())
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("cannot_remove_group_creator"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/groups/{id}/leave")
    class LeaveGroup {

        @Test
        @DisplayName("Участник может покинуть группу")
        void leave_Member_ReturnsNoContent() throws Exception {
            Group group = createGroup(organization, owner);
            groupMemberRepository.save(GroupMember.create(group, moderator, owner));

            mockMvc.perform(post(GROUPS_URL + "/" + group.getId() + "/leave")
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isNoContent());

            assertThat(groupMemberRepository.existsByGroupIdAndUserId(group.getId(), moderator.getId()))
                .isFalse();
        }

        @Test
        @DisplayName("Создатель не может покинуть группу")
        void leave_Creator_ReturnsConflict() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(post(GROUPS_URL + "/" + group.getId() + "/leave")
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("cannot_remove_group_creator"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/groups/join/{inviteCode}")
    class JoinByInviteCode {

        @Test
        @DisplayName("Член организации может присоединиться по коду")
        void join_OrgMember_ReturnsSuccess() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(post(GROUPS_URL + "/join/" + group.getInviteCode())
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(group.getId().toString()))
                .andExpect(jsonPath("$.groupName").value(group.getName()));

            assertThat(groupMemberRepository.existsByGroupIdAndUserId(group.getId(), moderator.getId()))
                .isTrue();
        }

        @Test
        @DisplayName("Не-член организации получает 403")
        void join_NotOrgMember_Returns403() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(post(GROUPS_URL + "/join/" + group.getInviteCode())
                    .with(jwt(jwtTokenProvider, outsider.getId())))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Уже участник группы получает 409")
        void join_AlreadyMember_ReturnsConflict() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(post(GROUPS_URL + "/join/" + group.getInviteCode())
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_group_member"));
        }

        @Test
        @DisplayName("Неверный инвайт-код возвращает 404")
        void join_InvalidCode_Returns404() throws Exception {
            mockMvc.perform(post(GROUPS_URL + "/join/INVALID1")
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Инвайт-код регистронезависим")
        void join_LowercaseCode_Works() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(post(GROUPS_URL + "/join/" + group.getInviteCode().toLowerCase())
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isOk());
        }
    }
}
