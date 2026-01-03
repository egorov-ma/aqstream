package ru.aqstream.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.client.UserClient;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.api.dto.CancelRegistrationRequest;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

/**
 * Интеграционные тесты для операций организатора с регистрациями.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@DisplayName("RegistrationController Organizer Integration Tests")
class RegistrationControllerOrganizerIntegrationTest extends SharedServicesTestContainer {

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
        registrationRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();

        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        testFirstName = FAKER.name().firstName();
        testLastName = FAKER.name().lastName();
        testEmail = FAKER.internet().emailAddress();

        // Настройка мока UserClient для всех тестов
        // Когда передаются данные (hasPersonalInfo() == true), UserClient НЕ вызывается
        // Но мок должен быть готов на случай автозаполнения
        UserDto defaultUser = new UserDto(
            userId, testEmail, testFirstName, testLastName,
            null, true, false, Instant.now()
        );
        when(userClient.findById(any(UUID.class)))
            .thenReturn(Optional.of(defaultUser));

        testEvent = Event.create(
            FAKER.book().title(),
            "test-event-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        testEvent.publish();
        testEvent = eventRepository.save(testEvent);

        testTicketType = TicketType.create(testEvent, FAKER.commerce().productName());
        testTicketType.updateQuantity(100);
        testTicketType = ticketTypeRepository.save(testTicketType);
    }

    private RequestPostProcessor userAuth() {
        return jwt(jwtTokenProvider, userId, null, tenantId, Set.of("USER"));
    }

    private RequestPostProcessor organizerAuth() {
        return jwt(jwtTokenProvider, userId, null, tenantId, Set.of("USER", "ORGANIZER"));
    }

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
    @DisplayName("GET /api/v1/events/{eventId}/registrations")
    class GetEventRegistrations {

        @Test
        @DisplayName("возвращает регистрации события для организатора")
        void getEventRegistrations_Organizer_ReturnsRegistrations() throws Exception {
            createTestRegistration();

            mockMvc.perform(get("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(organizerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего события")
        void getEventRegistrations_NotFound_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/events/" + UUID.randomUUID() + "/registrations")
                    .with(organizerAuth()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/events/{eventId}/registrations/{registrationId}")
    class CancelByOrganizer {

        @Test
        @DisplayName("отменяет регистрацию организатором с причиной")
        void cancelByOrganizer_WithReason_ReturnsNoContent() throws Exception {
            Registration registration = createTestRegistration();
            testTicketType.incrementSoldCount();
            ticketTypeRepository.save(testTicketType);

            CancelRegistrationRequest request = new CancelRegistrationRequest("Мероприятие отменено");

            mockMvc.perform(delete("/api/v1/events/" + testEvent.getId() + "/registrations/" + registration.getId())
                    .with(organizerAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

            Registration updated = registrationRepository.findById(registration.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
            assertThat(updated.getCancellationReason()).isEqualTo("Мероприятие отменено");
        }

        @Test
        @DisplayName("отменяет регистрацию организатором без причины")
        void cancelByOrganizer_NoReason_ReturnsNoContent() throws Exception {
            Registration registration = createTestRegistration();
            testTicketType.incrementSoldCount();
            ticketTypeRepository.save(testTicketType);

            mockMvc.perform(delete("/api/v1/events/" + testEvent.getId() + "/registrations/" + registration.getId())
                    .with(organizerAuth()))
                .andExpect(status().isNoContent());

            Registration updated = registrationRepository.findById(registration.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
            assertThat(updated.getCancellationReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Organizer Role Requirement")
    class OrganizerRoleRequirement {

        @Test
        @DisplayName("возвращает 403 для getEventRegistrations без роли организатора")
        void getEventRegistrations_NoOrganizerRole_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(userAuth()))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("возвращает 403 для cancelByOrganizer без роли организатора")
        void cancelByOrganizer_NoOrganizerRole_ReturnsForbidden() throws Exception {
            Registration registration = createTestRegistration();

            mockMvc.perform(delete("/api/v1/events/" + testEvent.getId() + "/registrations/" + registration.getId())
                    .with(userAuth()))
                .andExpect(status().isForbidden());

            Registration notCancelled = registrationRepository.findById(registration.getId()).orElseThrow();
            assertThat(notCancelled.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        }
    }
}
