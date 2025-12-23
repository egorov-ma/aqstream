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
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.dto.UpdateOrganizationRequest;
import ru.aqstream.user.db.entity.Organization;
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

}
