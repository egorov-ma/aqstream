package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.adminPrincipal;
import static ru.aqstream.common.test.SecurityTestUtils.userPrincipal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.user.api.dto.CreateOrganizationRequestRequest;
import ru.aqstream.user.api.dto.RejectOrganizationRequestRequest;
import ru.aqstream.user.db.entity.OrganizationRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.OrganizationRequestRepository;
import ru.aqstream.user.db.repository.UserRepository;

@IntegrationTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrganizationRequestController Integration Tests")
class OrganizationRequestControllerIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String BASE_URL = "/api/v1/organization-requests";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRequestRepository requestRepository;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы
        requestRepository.deleteAll();

        // Создаём тестового пользователя
        testUser = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        testUser = userRepository.save(testUser);

        // Создаём админа
        adminUser = User.createWithEmail(
            FAKER.internet().emailAddress(),
            "hashedPassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
        adminUser.setAdmin(true);
        adminUser = userRepository.save(adminUser);
    }

    private String fakeSlug() {
        return FAKER.internet().slug().toLowerCase().replace("_", "-");
    }

    @Nested
    @DisplayName("POST /api/v1/organization-requests")
    class Create {

        @Test
        @DisplayName("создаёт запрос для аутентифицированного пользователя")
        void create_Authenticated_ReturnsCreated() throws Exception {
            String name = FAKER.company().name();
            String slug = fakeSlug();
            String description = FAKER.company().catchPhrase();

            CreateOrganizationRequestRequest request = new CreateOrganizationRequestRequest(
                name, slug, description
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userPrincipal(testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.slug").value(slug.toLowerCase()))
                .andExpect(jsonPath("$.status").value("PENDING"));

            // Проверяем что запрос сохранён в БД
            var savedRequests = requestRepository.findByUserId(testUser.getId());
            assertThat(savedRequests).hasSize(1);
            assertThat(savedRequests.get(0).getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("возвращает 409 при наличии активного запроса")
        void create_PendingExists_ReturnsConflict() throws Exception {
            // Создаём первый запрос
            OrganizationRequest existing = OrganizationRequest.create(
                testUser, "Existing Org", fakeSlug(), "Description"
            );
            requestRepository.save(existing);

            CreateOrganizationRequestRequest request = new CreateOrganizationRequestRequest(
                FAKER.company().name(), fakeSlug(), "Description"
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userPrincipal(testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("pending_request_already_exists"));
        }

        @Test
        @DisplayName("возвращает 409 при занятом slug")
        void create_SlugTaken_ReturnsConflict() throws Exception {
            String takenSlug = fakeSlug();

            // Создаём запрос с этим slug от другого пользователя
            User otherUser = User.createWithEmail(
                FAKER.internet().emailAddress(), "hash", FAKER.name().firstName(), null
            );
            otherUser = userRepository.save(otherUser);

            OrganizationRequest existing = OrganizationRequest.create(
                otherUser, "Other Org", takenSlug, null
            );
            requestRepository.save(existing);

            CreateOrganizationRequestRequest request = new CreateOrganizationRequestRequest(
                FAKER.company().name(), takenSlug, null
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userPrincipal(testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slug_already_exists"));
        }

        @Test
        @DisplayName("возвращает 400 при невалидном slug")
        void create_InvalidSlug_ReturnsBadRequest() throws Exception {
            CreateOrganizationRequestRequest request = new CreateOrganizationRequestRequest(
                "Company", "Invalid Slug!", null  // slug с пробелами и спецсимволами
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userPrincipal(testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organization-requests/my")
    class GetMyRequests {

        @Test
        @DisplayName("возвращает запросы текущего пользователя")
        void getMy_ReturnsUserRequests() throws Exception {
            // Создаём запросы — только один, т.к. у пользователя может быть только один pending
            OrganizationRequest request1 = OrganizationRequest.create(
                testUser, "Org 1", fakeSlug(), null
            );
            requestRepository.save(request1);

            mockMvc.perform(get(BASE_URL + "/my")
                    .with(userPrincipal(testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organization-requests/{id}")
    class GetById {

        @Test
        @DisplayName("возвращает запрос владельцу")
        void getById_Owner_ReturnsRequest() throws Exception {
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "My Org", fakeSlug(), null
            );
            request = requestRepository.save(request);

            mockMvc.perform(get(BASE_URL + "/" + request.getId())
                    .with(userPrincipal(testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Org"));
        }

        @Test
        @DisplayName("возвращает 404 при несуществующем запросе")
        void getById_NotFound_Returns404() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                    .with(userPrincipal(testUser.getId())))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("возвращает 403 при доступе к чужому запросу")
        void getById_OtherUser_Returns403() throws Exception {
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "My Org", fakeSlug(), null
            );
            request = requestRepository.save(request);

            // Другой пользователь
            User otherUser = User.createWithEmail(
                FAKER.internet().emailAddress(), "hash", FAKER.name().firstName(), null
            );
            otherUser = userRepository.save(otherUser);

            mockMvc.perform(get(BASE_URL + "/" + request.getId())
                    .with(userPrincipal(otherUser.getId())))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("админ может получить любой запрос")
        void getById_Admin_ReturnsAnyRequest() throws Exception {
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "User Org", fakeSlug(), null
            );
            request = requestRepository.save(request);

            mockMvc.perform(get(BASE_URL + "/" + request.getId())
                    .with(adminPrincipal(adminUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User Org"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/organization-requests/{id}/approve")
    class Approve {

        @Test
        @DisplayName("одобряет запрос (админ)")
        void approve_Admin_ApprovesRequest() throws Exception {
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "Pending Org", fakeSlug(), null
            );
            request = requestRepository.save(request);

            mockMvc.perform(post(BASE_URL + "/" + request.getId() + "/approve")
                    .with(adminPrincipal(adminUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

            // Проверяем в БД
            OrganizationRequest updated = requestRepository.findById(request.getId()).orElseThrow();
            assertThat(updated.isApproved()).isTrue();
        }

        @Test
        @DisplayName("возвращает 403 для обычного пользователя")
        void approve_User_Returns403() throws Exception {
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "Pending Org", fakeSlug(), null
            );
            request = requestRepository.save(request);

            mockMvc.perform(post(BASE_URL + "/" + request.getId() + "/approve")
                    .with(userPrincipal(testUser.getId())))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/organization-requests/{id}/reject")
    class Reject {

        @Test
        @DisplayName("отклоняет запрос с причиной (админ)")
        void reject_Admin_RejectsRequest() throws Exception {
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "Pending Org", fakeSlug(), null
            );
            request = requestRepository.save(request);

            RejectOrganizationRequestRequest rejectRequest = new RejectOrganizationRequestRequest(
                "Недостаточно информации о деятельности организации"
            );

            mockMvc.perform(post(BASE_URL + "/" + request.getId() + "/reject")
                    .with(adminPrincipal(adminUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewComment").value(rejectRequest.comment()));

            // Проверяем в БД
            OrganizationRequest updated = requestRepository.findById(request.getId()).orElseThrow();
            assertThat(updated.isRejected()).isTrue();
            assertThat(updated.getReviewComment()).isEqualTo(rejectRequest.comment());
        }

        @Test
        @DisplayName("возвращает 403 для обычного пользователя")
        void reject_User_Returns403() throws Exception {
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "Pending Org", fakeSlug(), null
            );
            request = requestRepository.save(request);

            RejectOrganizationRequestRequest rejectRequest = new RejectOrganizationRequestRequest(
                "Недостаточно информации"
            );

            mockMvc.perform(post(BASE_URL + "/" + request.getId() + "/reject")
                    .with(userPrincipal(testUser.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/organization-requests (admin)")
    class GetAll {

        @Test
        @DisplayName("админ получает список всех запросов")
        void getAll_Admin_ReturnsAllRequests() throws Exception {
            // Создаём запрос
            OrganizationRequest request = OrganizationRequest.create(
                testUser, "Org", fakeSlug(), null
            );
            requestRepository.save(request);

            mockMvc.perform(get(BASE_URL)
                    .with(adminPrincipal(adminUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("обычный пользователь получает 403")
        void getAll_User_Returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .with(userPrincipal(testUser.getId())))
                .andExpect(status().isForbidden());
        }
    }
}
