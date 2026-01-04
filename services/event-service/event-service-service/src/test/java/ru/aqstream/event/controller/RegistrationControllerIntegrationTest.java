package ru.aqstream.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;
import static io.qameta.allure.SeverityLevel.CRITICAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.event.listener.OrganizationEventListener;
import ru.aqstream.user.client.UserClient;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.api.dto.CreateRegistrationRequest;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@Feature(AllureFeatures.Features.REGISTRATIONS)
@DisplayName("RegistrationController Integration Tests")
class RegistrationControllerIntegrationTest extends SharedServicesTestContainer {

    private static final Faker FAKER = new Faker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private EventPublisher eventPublisher;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private OrganizationEventListener organizationEventListener;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private UUID tenantId;
    private UUID userId;
    private Event testEvent;
    private TicketType testTicketType;

    private String testFirstName;
    private String testLastName;
    private String testEmail;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы
        registrationRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();

        // Генерируем userId и tenant_id
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        // Генерируем тестовые данные
        testFirstName = FAKER.name().firstName();
        testLastName = FAKER.name().lastName();
        testEmail = FAKER.internet().emailAddress();

        // Создаём тестовое событие (опубликованное)
        testEvent = Event.create(
            FAKER.book().title(),
            "test-event-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        testEvent.publish();
        testEvent = eventRepository.save(testEvent);

        // Создаём тестовый тип билета
        testTicketType = TicketType.create(testEvent, FAKER.commerce().productName());
        testTicketType.updateQuantity(100);
        testTicketType = ticketTypeRepository.save(testTicketType);
    }

    /**
     * Создаёт JWT аутентификацию для текущего пользователя и tenant.
     */
    private RequestPostProcessor userAuth() {
        return jwt(jwtTokenProvider, userId, null, tenantId, Set.of("USER"));
    }

    /**
     * Создаёт JWT аутентификацию для другого пользователя.
     */
    private RequestPostProcessor otherUserAuth() {
        UUID otherUserId = UUID.randomUUID();
        return jwt(jwtTokenProvider, otherUserId, null, tenantId, Set.of("USER"));
    }

    /**
     * Создаёт тестовую регистрацию.
     */
    private Registration createTestRegistration() {
        Registration registration = Registration.create(
            testEvent,
            testTicketType,
            userId,
            generateConfirmationCode(),
            testFirstName,
            testLastName,
            testEmail
        );
        return registrationRepository.save(registration);
    }

    private String generateConfirmationCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    @Nested
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("POST /api/v1/events/{eventId}/registrations")
    class Create {

        @Test
        @Severity(CRITICAL)
        @DisplayName("создаёт регистрацию с валидными данными")
        void create_ValidRequest_ReturnsCreated() throws Exception {
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                testTicketType.getId(),
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.firstName").value(testFirstName))
                .andExpect(jsonPath("$.lastName").value(testLastName))
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.confirmationCode").isNotEmpty())
                .andExpect(jsonPath("$.eventId").value(testEvent.getId().toString()))
                .andExpect(jsonPath("$.ticketTypeId").value(testTicketType.getId().toString()));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("увеличивает soldCount при создании регистрации")
        void create_ValidRequest_IncrementsSoldCount() throws Exception {
            int initialSoldCount = testTicketType.getSoldCount();
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                testTicketType.getId(),
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

            // Проверяем что soldCount увеличился
            TicketType updated = ticketTypeRepository.findById(testTicketType.getId()).orElseThrow();
            assertThat(updated.getSoldCount()).isEqualTo(initialSoldCount + 1);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 409 при повторной регистрации")
        void create_AlreadyRegistered_ReturnsConflict() throws Exception {
            // Создаём первую регистрацию
            createTestRegistration();

            CreateRegistrationRequest request = new CreateRegistrationRequest(
                testTicketType.getId(),
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("registration_already_exists"));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 404 для несуществующего события")
        void create_NonExistingEvent_ReturnsNotFound() throws Exception {
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                testTicketType.getId(),
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + UUID.randomUUID() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 409 для неопубликованного события")
        void create_DraftEvent_ReturnsConflict() throws Exception {
            // Создаём событие в статусе DRAFT
            Event draftEvent = Event.create(
                FAKER.book().title(),
                "draft-event-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "Europe/Moscow"
            );
            draftEvent = eventRepository.save(draftEvent);

            TicketType draftTicketType = TicketType.create(draftEvent, "Билет");
            draftTicketType = ticketTypeRepository.save(draftTicketType);

            CreateRegistrationRequest request = new CreateRegistrationRequest(
                draftTicketType.getId(),
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + draftEvent.getId() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("event_registration_closed"));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 409 при распроданных билетах")
        void create_SoldOut_ReturnsConflict() throws Exception {
            // Распродаём все билеты
            testTicketType.updateQuantity(1);
            testTicketType.incrementSoldCount();
            ticketTypeRepository.save(testTicketType);

            CreateRegistrationRequest request = new CreateRegistrationRequest(
                testTicketType.getId(),
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ticket_type_sold_out"));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 400 без обязательного ticketTypeId")
        void create_MissingTicketTypeId_ReturnsBadRequest() throws Exception {
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                null, // missing ticketTypeId (обязательное поле)
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 401 без авторизации")
        void create_NoAuth_ReturnsUnauthorized() throws Exception {
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                testTicketType.getId(),
                testFirstName,
                testLastName,
                testEmail,
                null
            );

            mockMvc.perform(post("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("автозаполняет данные из профиля когда они не переданы")
        void create_NoPersonalInfo_FetchesFromUserService() throws Exception {
            // Подготовка: мокируем UserClient
            ru.aqstream.user.api.dto.UserDto userDto = new ru.aqstream.user.api.dto.UserDto(
                userId, testEmail, testFirstName, testLastName,
                null, true, false, Instant.now()
            );
            org.mockito.Mockito.when(userClient.findById(userId))
                .thenReturn(java.util.Optional.of(userDto));

            // Запрос БЕЗ личных данных
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                testTicketType.getId(), null, null, null, null
            );

            mockMvc.perform(post("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value(testFirstName))
                .andExpect(jsonPath("$.lastName").value(testLastName))
                .andExpect(jsonPath("$.email").value(testEmail));

            // Проверяем что UserClient был вызван
            org.mockito.Mockito.verify(userClient).findById(userId);
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("GET /api/v1/registrations/my")
    class GetMyRegistrations {

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает регистрации текущего пользователя")
        void getMyRegistrations_HasRegistrations_ReturnsRegistrations() throws Exception {
            createTestRegistration();

            mockMvc.perform(get("/api/v1/registrations/my")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].firstName").value(testFirstName));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает пустой список если нет регистраций")
        void getMyRegistrations_NoRegistrations_ReturnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/v1/registrations/my")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("не возвращает отменённые регистрации")
        void getMyRegistrations_CancelledRegistration_NotReturned() throws Exception {
            Registration registration = createTestRegistration();
            registration.cancel();
            registrationRepository.save(registration);

            mockMvc.perform(get("/api/v1/registrations/my")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("GET /api/v1/registrations/{id}")
    class GetById {

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает регистрацию по ID для владельца")
        void getById_Owner_ReturnsRegistration() throws Exception {
            Registration registration = createTestRegistration();

            mockMvc.perform(get("/api/v1/registrations/" + registration.getId())
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(registration.getId().toString()))
                .andExpect(jsonPath("$.confirmationCode").value(registration.getConfirmationCode()));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 404 для несуществующей регистрации")
        void getById_NotFound_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/registrations/" + UUID.randomUUID())
                    .with(userAuth()))
                .andExpect(status().isNotFound());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 404 для чужой регистрации (не раскрывает существование)")
        void getById_NotOwner_ReturnsNotFound() throws Exception {
            Registration registration = createTestRegistration();

            mockMvc.perform(get("/api/v1/registrations/" + registration.getId())
                    .with(otherUserAuth()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("DELETE /api/v1/registrations/{id}")
    class Cancel {

        @Test
        @Severity(CRITICAL)
        @DisplayName("отменяет регистрацию участником")
        void cancel_Owner_ReturnsNoContent() throws Exception {
            Registration registration = createTestRegistration();
            // Увеличиваем soldCount чтобы потом проверить уменьшение
            testTicketType.incrementSoldCount();
            ticketTypeRepository.save(testTicketType);
            int soldCountBefore = testTicketType.getSoldCount();

            mockMvc.perform(delete("/api/v1/registrations/" + registration.getId())
                    .with(userAuth()))
                .andExpect(status().isNoContent());

            // Проверяем что регистрация отменена
            Registration updated = registrationRepository.findById(registration.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
            assertThat(updated.getCancelledAt()).isNotNull();

            // Проверяем что soldCount уменьшился
            TicketType updatedTicketType = ticketTypeRepository.findById(testTicketType.getId()).orElseThrow();
            assertThat(updatedTicketType.getSoldCount()).isEqualTo(soldCountBefore - 1);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 404 для чужой регистрации (не раскрывает существование)")
        void cancel_NotOwner_ReturnsNotFound() throws Exception {
            Registration registration = createTestRegistration();

            mockMvc.perform(delete("/api/v1/registrations/" + registration.getId())
                    .with(otherUserAuth()))
                .andExpect(status().isNotFound());

            // Регистрация не должна быть отменена
            Registration notCancelled = registrationRepository.findById(registration.getId()).orElseThrow();
            assertThat(notCancelled.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("возвращает 409 для уже отменённой регистрации")
        void cancel_AlreadyCancelled_ReturnsConflict() throws Exception {
            Registration registration = createTestRegistration();
            registration.cancel();
            registrationRepository.save(registration);

            mockMvc.perform(delete("/api/v1/registrations/" + registration.getId())
                    .with(userAuth()))
                .andExpect(status().isConflict());
        }
    }

}
