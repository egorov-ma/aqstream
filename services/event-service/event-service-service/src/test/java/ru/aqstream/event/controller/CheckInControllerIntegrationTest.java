package ru.aqstream.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.event.listener.OrganizationEventListener;
import ru.aqstream.user.client.UserClient;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@DisplayName("CheckInController Integration Tests")
class CheckInControllerIntegrationTest extends SharedServicesTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String CHECK_IN_URL = "/api/v1/public/check-in";

    @Autowired
    private MockMvc mockMvc;

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
    private Event testEvent;
    private TicketType testTicketType;
    private Registration testRegistration;
    private String confirmationCode;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы
        registrationRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();

        // Генерируем tenant_id
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        // Создаём тестовое событие (опубликованное)
        testEvent = Event.create(
            FAKER.book().title(),
            "check-in-test-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        testEvent.publish();
        testEvent = eventRepository.save(testEvent);

        // Создаём тестовый тип билета
        testTicketType = TicketType.create(testEvent, FAKER.commerce().productName());
        testTicketType.updateQuantity(100);
        testTicketType = ticketTypeRepository.save(testTicketType);

        // Создаём тестовую регистрацию
        confirmationCode = generateConfirmationCode();
        testRegistration = Registration.create(
            testEvent,
            testTicketType,
            UUID.randomUUID(),
            confirmationCode,
            FAKER.name().firstName(),
            FAKER.name().lastName(),
            FAKER.internet().emailAddress()
        );
        testRegistration = registrationRepository.save(testRegistration);
    }

    @Nested
    @DisplayName("GET /api/v1/public/check-in/{confirmationCode}")
    class GetCheckInInfo {

        @Test
        @DisplayName("возвращает информацию о регистрации без авторизации")
        void getCheckInInfo_ValidCode_ReturnsInfo() throws Exception {
            mockMvc.perform(get(CHECK_IN_URL + "/" + confirmationCode)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(testRegistration.getId().toString()))
                .andExpect(jsonPath("$.confirmationCode").value(confirmationCode))
                .andExpect(jsonPath("$.eventId").value(testEvent.getId().toString()))
                .andExpect(jsonPath("$.eventTitle").value(testEvent.getTitle()))
                .andExpect(jsonPath("$.ticketTypeName").value(testTicketType.getName()))
                .andExpect(jsonPath("$.firstName").value(testRegistration.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(testRegistration.getLastName()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.isCheckedIn").value(false))
                .andExpect(jsonPath("$.checkedInAt").isEmpty());
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего кода")
        void getCheckInInfo_InvalidCode_Returns404() throws Exception {
            mockMvc.perform(get(CHECK_IN_URL + "/INVALID1")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("registration_not_found"));
        }

        @Test
        @DisplayName("возвращает информацию с отметкой о check-in")
        void getCheckInInfo_AlreadyCheckedIn_ReturnsInfoWithCheckedInAt() throws Exception {
            // Given - выполняем check-in
            testRegistration.checkIn();
            registrationRepository.save(testRegistration);

            // When & Then
            mockMvc.perform(get(CHECK_IN_URL + "/" + confirmationCode)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCheckedIn").value(true))
                .andExpect(jsonPath("$.checkedInAt").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/public/check-in/{confirmationCode}")
    class CheckIn {

        @Test
        @DisplayName("успешно выполняет check-in без авторизации")
        void checkIn_ValidCode_Success() throws Exception {
            mockMvc.perform(post(CHECK_IN_URL + "/" + confirmationCode)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(testRegistration.getId().toString()))
                .andExpect(jsonPath("$.confirmationCode").value(confirmationCode))
                .andExpect(jsonPath("$.eventTitle").value(testEvent.getTitle()))
                .andExpect(jsonPath("$.ticketTypeName").value(testTicketType.getName()))
                .andExpect(jsonPath("$.firstName").value(testRegistration.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(testRegistration.getLastName()))
                .andExpect(jsonPath("$.checkedInAt").isNotEmpty())
                .andExpect(jsonPath("$.message").exists());

            // Проверяем что check-in сохранён в БД
            Registration updated = registrationRepository.findById(testRegistration.getId()).orElseThrow();
            assertThat(updated.isCheckedIn()).isTrue();
            assertThat(updated.getCheckedInAt()).isNotNull();
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего кода")
        void checkIn_InvalidCode_Returns404() throws Exception {
            mockMvc.perform(post(CHECK_IN_URL + "/INVALID1")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("registration_not_found"));
        }

        @Test
        @DisplayName("возвращает 409 при повторном check-in")
        void checkIn_AlreadyCheckedIn_Returns409() throws Exception {
            // Given - выполняем check-in
            testRegistration.checkIn();
            registrationRepository.save(testRegistration);

            // When & Then
            mockMvc.perform(post(CHECK_IN_URL + "/" + confirmationCode)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_checked_in"));
        }

        @Test
        @DisplayName("возвращает 409 для отменённой регистрации")
        void checkIn_CancelledRegistration_Returns409() throws Exception {
            // Given - отменяем регистрацию
            testRegistration.cancel();
            registrationRepository.save(testRegistration);

            // When & Then
            mockMvc.perform(post(CHECK_IN_URL + "/" + confirmationCode)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("check_in_not_allowed"));
        }
    }

    /**
     * Генерирует уникальный confirmation code (8 символов).
     */
    private String generateConfirmationCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(FAKER.random().nextInt(chars.length())));
        }
        return sb.toString();
    }
}
