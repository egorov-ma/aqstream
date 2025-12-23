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
import ru.aqstream.user.api.dto.CreateOrganizationRequest;
import ru.aqstream.user.api.dto.InviteMemberRequest;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.dto.UpdateMemberRoleRequest;
import ru.aqstream.user.api.dto.UpdateOrganizationRequest;
import ru.aqstream.user.db.entity.Organization;
import ru.aqstream.user.db.entity.OrganizationInvite;
import ru.aqstream.user.db.entity.OrganizationMember;
import ru.aqstream.user.db.entity.OrganizationRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationInviteRepository;
import ru.aqstream.user.db.repository.OrganizationMemberRepository;
import ru.aqstream.user.db.repository.OrganizationRepository;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.UserRepository;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("OrganizationController Integration Tests")
class OrganizationControllerIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String BASE_URL = "/api/v1/organizations";

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
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private OrganizationInviteRepository inviteRepository;

    @Autowired
    private OrganizationRequestRepository requestRepository;

    private User testUser;
    private User otherUser;
    private OrganizationRequest approvedRequest;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы в правильном порядке (FK constraints)
        inviteRepository.deleteAll();
        memberRepository.deleteAll();
        organizationRepository.deleteAll();
        requestRepository.deleteAll();

        // Создаём тестового пользователя
        testUser = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        testUser = userRepository.save(testUser);

        // Создаём другого пользователя
        otherUser = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        otherUser = userRepository.save(otherUser);

        // Создаём одобренный запрос на организацию для testUser
        approvedRequest = OrganizationRequest.create(
            testUser,
            FAKER.company().name(),
            fakeSlug(),
            FAKER.company().catchPhrase()
        );
        approvedRequest.approve(testUser); // Одобряем запрос
        approvedRequest = requestRepository.save(approvedRequest);
    }

    private String fakeSlug() {
        return FAKER.internet().slug().toLowerCase().replace("_", "-");
    }

    /**
     * Создаёт организацию для пользователя (helper метод).
     */
    private Organization createOrganization(User owner, String slug) {
        Organization org = Organization.create(
            owner,
            FAKER.company().name(),
            slug,
            FAKER.company().catchPhrase()
        );
        org = organizationRepository.save(org);

        // Создаём членство OWNER
        OrganizationMember ownerMember = OrganizationMember.createOwner(org, owner);
        memberRepository.save(ownerMember);

        return org;
    }

    // ==================== CRUD Организаций ====================

    @Nested
    @DisplayName("GET /api/v1/organizations")
    class GetMyOrganizations {

        @Test
        @DisplayName("возвращает список организаций пользователя")
        void getMyOrganizations_ReturnsUserOrganizations() throws Exception {
            // Создаём организацию
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(get(BASE_URL)
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(org.getId().toString()));
        }

        @Test
        @DisplayName("возвращает пустой список если нет организаций")
        void getMyOrganizations_NoOrganizations_ReturnsEmptyList() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("возвращает 401 без аутентификации")
        void getMyOrganizations_NoAuth_Returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/organizations")
    class CreateOrganization {

        @Test
        @DisplayName("создаёт организацию при наличии одобренного запроса")
        void create_WithApprovedRequest_ReturnsCreated() throws Exception {
            String name = FAKER.company().name();
            String description = FAKER.company().catchPhrase();

            CreateOrganizationRequest request = new CreateOrganizationRequest(name, description);

            mockMvc.perform(post(BASE_URL)
                    .with(jwt(jwtTokenProvider, testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.slug").value(approvedRequest.getSlug()));

            // Проверяем что организация создана в БД
            var organizations = organizationRepository.findAll();
            assertThat(organizations).hasSize(1);
            assertThat(organizations.get(0).getName()).isEqualTo(name);

            // Проверяем что создатель стал OWNER
            var members = memberRepository.findByOrganizationId(organizations.get(0).getId());
            assertThat(members).hasSize(1);
            assertThat(members.get(0).getRole()).isEqualTo(OrganizationRole.OWNER);
        }

        @Test
        @DisplayName("возвращает 409 без одобренного запроса")
        void create_NoApprovedRequest_ReturnsConflict() throws Exception {
            // Отклоняем запрос
            approvedRequest.reject(testUser, "Отклонено");
            requestRepository.save(approvedRequest);

            CreateOrganizationRequest request = new CreateOrganizationRequest(FAKER.company().name(), null);

            mockMvc.perform(post(BASE_URL)
                    .with(jwt(jwtTokenProvider, testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("no_approved_request"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}")
    class GetById {

        @Test
        @DisplayName("возвращает организацию члену")
        void getById_Member_ReturnsOrganization() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(get(BASE_URL + "/" + org.getId())
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.getId().toString()))
                .andExpect(jsonPath("$.name").value(org.getName()));
        }

        @Test
        @DisplayName("возвращает 403 для не-члена")
        void getById_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(get(BASE_URL + "/" + org.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("возвращает 404 для несуществующей организации")
        void getById_NotFound_Returns404() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + java.util.UUID.randomUUID())
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/organizations/{id}")
    class UpdateOrganization {

        @Test
        @DisplayName("OWNER может обновить организацию")
        void update_Owner_ReturnsUpdated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());
            String newName = FAKER.company().name();
            String newDescription = FAKER.company().catchPhrase();

            UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                newName, newDescription, null
            );

            mockMvc.perform(put(BASE_URL + "/" + org.getId())
                    .with(jwt(jwtTokenProvider, testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.description").value(newDescription));

            // Проверяем в БД
            Organization updated = organizationRepository.findById(org.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("MODERATOR может обновить организацию")
        void update_Moderator_ReturnsUpdated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как MODERATOR
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            String newName = FAKER.company().name();
            UpdateOrganizationRequest request = new UpdateOrganizationRequest(newName, null, null);

            mockMvc.perform(put(BASE_URL + "/" + org.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
        }

        @Test
        @DisplayName("не-член не может обновить организацию")
        void update_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            UpdateOrganizationRequest request = new UpdateOrganizationRequest("New Name", null, null);

            mockMvc.perform(put(BASE_URL + "/" + org.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/organizations/{id}")
    class DeleteOrganization {

        @Test
        @DisplayName("OWNER может удалить организацию")
        void delete_Owner_ReturnsNoContent() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(delete(BASE_URL + "/" + org.getId())
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isNoContent());

            // Проверяем soft delete в БД (организация помечена как удалённая)
            // SQLRestriction скрывает удалённые, поэтому используем native query или проверяем отсутствие
            assertThat(organizationRepository.findById(org.getId())).isEmpty();
        }

        @Test
        @DisplayName("MODERATOR не может удалить организацию")
        void delete_Moderator_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как MODERATOR
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            mockMvc.perform(delete(BASE_URL + "/" + org.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("insufficient_permissions"));
        }
    }

    // ==================== Переключение организаций ====================

    @Nested
    @DisplayName("POST /api/v1/organizations/{id}/switch")
    class SwitchOrganization {

        @Test
        @DisplayName("переключает на организацию и возвращает новые токены")
        void switch_Member_ReturnsNewTokens() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/switch")
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("не-член не может переключиться")
        void switch_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/switch")
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isForbidden());
        }
    }

    // ==================== Управление членами ====================

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}/members")
    class GetMembers {

        @Test
        @DisplayName("возвращает список членов организации")
        void getMembers_Member_ReturnsMemberList() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем ещё одного члена
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            mockMvc.perform(get(BASE_URL + "/" + org.getId() + "/members")
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("не-член не может получить список членов")
        void getMembers_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            mockMvc.perform(get(BASE_URL + "/" + org.getId() + "/members")
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/organizations/{id}/members/{userId}")
    class UpdateMemberRole {

        @Test
        @DisplayName("OWNER может изменить роль члена")
        void updateRole_Owner_ReturnsUpdated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как MODERATOR
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            // Передаём владение (назначаем OWNER)
            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(OrganizationRole.OWNER);

            mockMvc.perform(put(BASE_URL + "/" + org.getId() + "/members/" + otherUser.getId())
                    .with(jwt(jwtTokenProvider, testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));

            // Проверяем что старый OWNER стал MODERATOR
            OrganizationMember oldOwner = memberRepository
                .findByOrganizationIdAndUserId(org.getId(), testUser.getId())
                .orElseThrow();
            assertThat(oldOwner.getRole()).isEqualTo(OrganizationRole.MODERATOR);
        }

        @Test
        @DisplayName("MODERATOR не может изменить роль")
        void updateRole_Moderator_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как MODERATOR
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(OrganizationRole.OWNER);

            mockMvc.perform(put(BASE_URL + "/" + org.getId() + "/members/" + testUser.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/organizations/{id}/members/{userId}")
    class RemoveMember {

        @Test
        @DisplayName("OWNER может удалить MODERATOR")
        void removeMember_OwnerRemovesModerator_ReturnsNoContent() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как MODERATOR
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            mockMvc.perform(delete(BASE_URL + "/" + org.getId() + "/members/" + otherUser.getId())
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isNoContent());

            // Проверяем что член удалён
            assertThat(memberRepository.findByOrganizationIdAndUserId(org.getId(), otherUser.getId()))
                .isEmpty();
        }

        @Test
        @DisplayName("нельзя удалить OWNER")
        void removeMember_CannotRemoveOwner_ReturnsConflict() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как MODERATOR
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            // Пытаемся удалить OWNER (testUser) от имени MODERATOR
            mockMvc.perform(delete(BASE_URL + "/" + org.getId() + "/members/" + testUser.getId())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("cannot_remove_owner"));
        }
    }

    // ==================== Приглашения ====================

    @Nested
    @DisplayName("POST /api/v1/organizations/{id}/invite")
    class CreateInvite {

        @Test
        @DisplayName("OWNER может создать приглашение")
        void createInvite_Owner_ReturnsCreated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            InviteMemberRequest request = new InviteMemberRequest("@telegram_user");

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/invite")
                    .with(jwt(jwtTokenProvider, testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").exists())
                .andExpect(jsonPath("$.telegramDeeplink").exists())
                .andExpect(jsonPath("$.role").value("MODERATOR"));

            // Проверяем что приглашение создано в БД
            var invites = inviteRepository.findByOrganizationId(org.getId());
            assertThat(invites).hasSize(1);
            assertThat(invites.get(0).getTelegramUsername()).isEqualTo("@telegram_user");
        }

        @Test
        @DisplayName("MODERATOR может создать приглашение")
        void createInvite_Moderator_ReturnsCreated() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как MODERATOR
            OrganizationMember moderator = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(moderator);

            InviteMemberRequest request = new InviteMemberRequest(null);

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/invite")
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").exists());
        }

        @Test
        @DisplayName("не-член не может создать приглашение")
        void createInvite_NotMember_Returns403() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            InviteMemberRequest request = new InviteMemberRequest(null);

            mockMvc.perform(post(BASE_URL + "/" + org.getId() + "/invite")
                    .with(jwt(jwtTokenProvider, otherUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organizations/{id}/invites")
    class GetActiveInvites {

        @Test
        @DisplayName("возвращает список активных приглашений")
        void getInvites_Member_ReturnsInviteList() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Создаём приглашение
            OrganizationInvite invite = OrganizationInvite.create(org, testUser, "@invited_user");
            inviteRepository.save(invite);

            mockMvc.perform(get(BASE_URL + "/" + org.getId() + "/invites")
                    .with(jwt(jwtTokenProvider, testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inviteCode").value(invite.getInviteCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/organizations/join/{inviteCode}")
    class AcceptInvite {

        @Test
        @DisplayName("пользователь может принять приглашение")
        void acceptInvite_ValidCode_JoinsOrganization() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Создаём приглашение
            OrganizationInvite invite = OrganizationInvite.create(org, testUser, null);
            invite = inviteRepository.save(invite);

            mockMvc.perform(post(BASE_URL + "/join/" + invite.getInviteCode())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(otherUser.getId().toString()))
                .andExpect(jsonPath("$.role").value("MODERATOR"));

            // Проверяем что пользователь стал членом организации
            assertThat(memberRepository.existsByOrganizationIdAndUserId(org.getId(), otherUser.getId()))
                .isTrue();

            // Проверяем что приглашение помечено как использованное
            OrganizationInvite usedInvite = inviteRepository.findById(invite.getId()).orElseThrow();
            assertThat(usedInvite.isUsed()).isTrue();
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего кода")
        void acceptInvite_InvalidCode_Returns404() throws Exception {
            mockMvc.perform(post(BASE_URL + "/join/invalid_code_12345678901234")
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("возвращает 409 для использованного приглашения")
        void acceptInvite_AlreadyUsed_ReturnsConflict() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Создаём использованное приглашение
            OrganizationInvite invite = OrganizationInvite.create(org, testUser, null);
            invite.markAsUsed(otherUser);
            invite = inviteRepository.save(invite);

            // Третий пользователь пытается использовать
            User thirdUser = User.createWithEmail(
                FAKER.internet().emailAddress(),
                "hashedPassword",
                FAKER.name().firstName(),
                FAKER.name().lastName()
            );
            thirdUser = userRepository.save(thirdUser);

            mockMvc.perform(post(BASE_URL + "/join/" + invite.getInviteCode())
                    .with(jwt(jwtTokenProvider, thirdUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite_already_used"));
        }

        @Test
        @DisplayName("возвращает 409 если пользователь уже член организации")
        void acceptInvite_AlreadyMember_ReturnsConflict() throws Exception {
            Organization org = createOrganization(testUser, fakeSlug());

            // Добавляем otherUser как члена
            OrganizationMember member = OrganizationMember.createModerator(org, otherUser, testUser);
            memberRepository.save(member);

            // Создаём приглашение
            OrganizationInvite invite = OrganizationInvite.create(org, testUser, null);
            invite = inviteRepository.save(invite);

            mockMvc.perform(post(BASE_URL + "/join/" + invite.getInviteCode())
                    .with(jwt(jwtTokenProvider, otherUser.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_organization_member"));
        }
    }
}
