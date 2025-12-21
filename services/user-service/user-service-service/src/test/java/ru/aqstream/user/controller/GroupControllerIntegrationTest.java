package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.user.api.dto.CreateGroupRequest;
import ru.aqstream.user.api.dto.UpdateGroupRequest;
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

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("GroupController Integration Tests")
class GroupControllerIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String GROUPS_URL = "/api/v1/groups";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        // Очищаем таблицы в правильном порядке (FK constraints)
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        orgMemberRepository.deleteAll();
        organizationRepository.deleteAll();

        // Создаём пользователей
        owner = createUser();
        moderator = createUser();
        outsider = createUser();

        // Создаём организацию
        organization = createOrganization(owner);

        // Добавляем членов организации (все члены — OWNER или MODERATOR)
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

        // Создатель автоматически становится участником
        groupMemberRepository.save(GroupMember.createCreator(group, creator));

        return group;
    }

    private String organizationsGroupsUrl(Organization org) {
        return "/api/v1/organizations/" + org.getId() + "/groups";
    }

    // ==================== CRUD Групп ====================

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}/groups")
    class GetOrganizationGroups {

        @Test
        @DisplayName("OWNER видит все группы организации")
        void getGroups_Owner_ReturnsAllGroups() throws Exception {
            Group group1 = createGroup(organization, owner);
            Group group2 = createGroup(organization, moderator);

            mockMvc.perform(get(organizationsGroupsUrl(organization))
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("MODERATOR видит все группы организации")
        void getGroups_Moderator_ReturnsAllGroups() throws Exception {
            Group group1 = createGroup(organization, owner);
            Group group2 = createGroup(organization, moderator);

            mockMvc.perform(get(organizationsGroupsUrl(organization))
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Не-член организации получает 403")
        void getGroups_NotMember_Returns403() throws Exception {
            mockMvc.perform(get(organizationsGroupsUrl(organization))
                    .with(jwt(jwtTokenProvider, outsider.getId())))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Без аутентификации возвращает 401")
        void getGroups_NoAuth_Returns401() throws Exception {
            mockMvc.perform(get(organizationsGroupsUrl(organization)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/organizations/{id}/groups")
    class CreateGroup {

        @Test
        @DisplayName("OWNER может создать группу")
        void create_Owner_ReturnsCreated() throws Exception {
            String groupName = FAKER.company().name();
            String description = FAKER.lorem().paragraph();
            CreateGroupRequest request = new CreateGroupRequest(groupName, description);

            mockMvc.perform(post(organizationsGroupsUrl(organization))
                    .with(jwt(jwtTokenProvider, owner.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(groupName))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.inviteCode").exists())
                .andExpect(jsonPath("$.memberCount").value(1));

            // Проверяем что группа создана в БД
            var groups = groupRepository.findByOrganizationId(organization.getId());
            assertThat(groups).hasSize(1);
            assertThat(groups.get(0).getName()).isEqualTo(groupName);
        }

        @Test
        @DisplayName("MODERATOR может создать группу")
        void create_Moderator_ReturnsCreated() throws Exception {
            CreateGroupRequest request = new CreateGroupRequest(FAKER.company().name(), null);

            mockMvc.perform(post(organizationsGroupsUrl(organization))
                    .with(jwt(jwtTokenProvider, moderator.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").exists());
        }

        @Test
        @DisplayName("Не-член организации не может создать группу")
        void create_NotMember_Returns403() throws Exception {
            CreateGroupRequest request = new CreateGroupRequest(FAKER.company().name(), null);

            mockMvc.perform(post(organizationsGroupsUrl(organization))
                    .with(jwt(jwtTokenProvider, outsider.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Валидация: пустое имя возвращает 400")
        void create_EmptyName_Returns400() throws Exception {
            CreateGroupRequest request = new CreateGroupRequest("", null);

            mockMvc.perform(post(organizationsGroupsUrl(organization))
                    .with(jwt(jwtTokenProvider, owner.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/groups/{id}")
    class GetGroupById {

        @Test
        @DisplayName("Участник группы может получить детали")
        void getById_GroupMember_ReturnsGroup() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(get(GROUPS_URL + "/" + group.getId())
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId().toString()))
                .andExpect(jsonPath("$.name").value(group.getName()))
                .andExpect(jsonPath("$.inviteCode").value(group.getInviteCode()));
        }

        @Test
        @DisplayName("MODERATOR организации может получить детали любой группы")
        void getById_OrgModerator_ReturnsGroup() throws Exception {
            Group group = createGroup(organization, owner);

            // moderator не в группе, но MODERATOR организации
            mockMvc.perform(get(GROUPS_URL + "/" + group.getId())
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId().toString()));
        }

        @Test
        @DisplayName("Не-член организации получает 403")
        void getById_NotOrgMember_Returns403() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(get(GROUPS_URL + "/" + group.getId())
                    .with(jwt(jwtTokenProvider, outsider.getId())))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Несуществующая группа возвращает 404")
        void getById_NotFound_Returns404() throws Exception {
            mockMvc.perform(get(GROUPS_URL + "/" + java.util.UUID.randomUUID())
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/groups/{id}")
    class UpdateGroup {

        @Test
        @DisplayName("OWNER может обновить группу")
        void update_Owner_ReturnsUpdated() throws Exception {
            Group group = createGroup(organization, moderator);
            String newName = FAKER.company().name();
            String newDescription = FAKER.lorem().paragraph();

            UpdateGroupRequest request = new UpdateGroupRequest(newName, newDescription);

            mockMvc.perform(put(GROUPS_URL + "/" + group.getId())
                    .with(jwt(jwtTokenProvider, owner.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.description").value(newDescription));

            // Проверяем в БД
            Group updated = groupRepository.findById(group.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("MODERATOR может обновить группу")
        void update_Moderator_ReturnsUpdated() throws Exception {
            Group group = createGroup(organization, owner);
            String newName = FAKER.company().name();

            UpdateGroupRequest request = new UpdateGroupRequest(newName, null);

            mockMvc.perform(put(GROUPS_URL + "/" + group.getId())
                    .with(jwt(jwtTokenProvider, moderator.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/groups/{id}")
    class DeleteGroup {

        @Test
        @DisplayName("OWNER может удалить группу")
        void delete_Owner_ReturnsNoContent() throws Exception {
            Group group = createGroup(organization, moderator);

            mockMvc.perform(delete(GROUPS_URL + "/" + group.getId())
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isNoContent());

            // Проверяем что группа удалена
            assertThat(groupRepository.findById(group.getId())).isEmpty();
        }

        @Test
        @DisplayName("MODERATOR не может удалить группу")
        void delete_Moderator_Returns403() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(delete(GROUPS_URL + "/" + group.getId())
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("insufficient_group_permissions"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/groups/{id}/regenerate-code")
    class RegenerateInviteCode {

        @Test
        @DisplayName("OWNER может регенерировать инвайт-код")
        void regenerate_Owner_ReturnsNewCode() throws Exception {
            Group group = createGroup(organization, moderator);
            String oldCode = group.getInviteCode();

            mockMvc.perform(post(GROUPS_URL + "/" + group.getId() + "/regenerate-code")
                    .with(jwt(jwtTokenProvider, owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").exists());

            // Проверяем что код изменился
            Group updated = groupRepository.findById(group.getId()).orElseThrow();
            assertThat(updated.getInviteCode()).isNotEqualTo(oldCode);
        }

        @Test
        @DisplayName("MODERATOR может регенерировать инвайт-код")
        void regenerate_Moderator_ReturnsNewCode() throws Exception {
            Group group = createGroup(organization, owner);

            mockMvc.perform(post(GROUPS_URL + "/" + group.getId() + "/regenerate-code")
                    .with(jwt(jwtTokenProvider, moderator.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").exists());
        }
    }

    // ==================== Управление участниками ====================

    @Nested
    @DisplayName("GET /api/v1/groups/{id}/members")
    class GetGroupMembers {

        @Test
        @DisplayName("Участник группы видит список участников")
        void getMembers_GroupMember_ReturnsMemberList() throws Exception {
            Group group = createGroup(organization, owner);

            // Добавляем ещё одного участника
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

            // Проверяем что участник удалён
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

            // Проверяем что участник вышел
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

    // ==================== Присоединение по коду ====================

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
                .andExpect(jsonPath("$.groupName").value(group.getName()))
                .andExpect(jsonPath("$.organizationId").value(organization.getId().toString()));

            // Проверяем что стал участником
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("group_not_found"));
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
