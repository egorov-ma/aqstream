package ru.aqstream.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.datafaker.Faker;
import org.hamcrest.Matchers;
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
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.api.dto.CreateRegistrationRequest;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

/**
 * Интеграционные тесты для продвинутых сценариев регистрации:
 * RLS изоляция, код подтверждения, конкурентные регистрации.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@DisplayName("RegistrationController Advanced Integration Tests")
class RegistrationControllerAdvancedIntegrationTest extends SharedServicesTestContainer {

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

    @Nested
    @DisplayName("RLS Isolation")
    class RlsIsolation {

        @Test
        @DisplayName("регистрации другого tenant не видны")
        void getEventRegistrations_DifferentTenant_ReturnsNotFound() throws Exception {
            UUID otherTenantId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/events/" + testEvent.getId() + "/registrations")
                    .with(jwt(jwtTokenProvider, otherUserId, null, otherTenantId, Set.of("USER", "ORGANIZER"))))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Confirmation Code")
    class ConfirmationCode {

        @Test
        @DisplayName("генерирует уникальный 8-символьный код")
        void create_GeneratesUniqueCode() throws Exception {
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
                .andExpect(jsonPath("$.confirmationCode").isNotEmpty())
                .andExpect(jsonPath("$.confirmationCode").value(
                    Matchers.matchesPattern("[A-Z0-9]{8}")
                ));
        }
    }

    @Nested
    @DisplayName("Concurrent Registration (Overselling Prevention)")
    class ConcurrentRegistration {

        @Test
        @DisplayName("предотвращает overselling при параллельных регистрациях")
        void create_ConcurrentRequests_PreventsOverselling() throws Exception {
            // given: только 2 билета доступно
            testTicketType.updateQuantity(2);
            ticketTypeRepository.save(testTicketType);

            int numberOfConcurrentRequests = 5;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);

            // when: 5 пользователей пытаются зарегистрироваться одновременно
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numberOfConcurrentRequests; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        latch.await();

                        UUID concurrentUserId = UUID.randomUUID();
                        String email = FAKER.internet().emailAddress();

                        CreateRegistrationRequest request = new CreateRegistrationRequest(
                            testTicketType.getId(),
                            FAKER.name().firstName(),
                            FAKER.name().lastName(),
                            email,
                            null
                        );

                        var result = mockMvc.perform(
                            post("/api/v1/events/" + testEvent.getId() + "/registrations")
                                .with(jwt(jwtTokenProvider, concurrentUserId, null, tenantId, Set.of("USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                        ).andReturn();

                        int status = result.getResponse().getStatus();
                        if (status == 201) {
                            successCount.incrementAndGet();
                        } else if (status == 409) {
                            conflictCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Игнорируем исключения в тесте
                    }
                }));
            }

            // Запускаем все потоки одновременно
            latch.countDown();

            // Ждём завершения всех запросов
            for (var future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Игнорируем
                }
            }
            executor.shutdown();

            // then: максимум 2 регистрации должны быть успешными
            assertThat(successCount.get())
                .as("Количество успешных регистраций не должно превышать доступное количество билетов")
                .isLessThanOrEqualTo(2);

            // Проверяем что soldCount соответствует количеству успешных регистраций
            TicketType updatedTicketType = ticketTypeRepository.findById(testTicketType.getId()).orElseThrow();
            assertThat(updatedTicketType.getSoldCount())
                .as("soldCount должен соответствовать количеству успешных регистраций")
                .isEqualTo(successCount.get());

            // Проверяем что нет overselling
            assertThat(updatedTicketType.getSoldCount())
                .as("soldCount не должен превышать quantity")
                .isLessThanOrEqualTo(updatedTicketType.getQuantity());
        }
    }
}
