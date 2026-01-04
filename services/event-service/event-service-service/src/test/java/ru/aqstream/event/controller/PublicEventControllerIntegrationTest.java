package ru.aqstream.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.test.allure.AllureSteps;
import ru.aqstream.common.test.allure.TestLogger;
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;
import ru.aqstream.event.listener.OrganizationEventListener;
import ru.aqstream.user.client.UserClient;

import static io.qameta.allure.SeverityLevel.BLOCKER;

/**
 * Интеграционные тесты для PublicEventController.
 * Проверяет endpoint GET /api/v1/public/events/{slug} с опциональной аутентификацией.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@Feature(AllureFeatures.Features.EVENT_MANAGEMENT)
@Story(AllureFeatures.Stories.EVENT_CRUD)
@DisplayName("PublicEventController Integration Tests")
class PublicEventControllerIntegrationTest extends SharedServicesTestContainer {

    private static final Faker FAKER = new Faker();

    @Autowired
    private MockMvc mockMvc;

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
    private String testEventTitle;

    @BeforeEach
    void setUp() {
        AllureSteps.step("Очистка БД", () -> {
            registrationRepository.deleteAll();
            ticketTypeRepository.deleteAll();
            eventRepository.deleteAll();
        });

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testEventTitle = FAKER.book().title();
        TenantContext.setTenantId(tenantId);

        testEvent = AllureSteps.createTestEvent(testEventTitle, () -> {
            Event event = Event.create(
                testEventTitle,
                "test-event-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "Europe/Moscow"
            );
            event.setDescription(FAKER.lorem().sentence());
            event.publish();
            event.updateVisibility(true, null);
            return eventRepository.save(event);
        });

        testTicketType = AllureSteps.step("Создать тип билета", () -> {
            TicketType ticketType = TicketType.create(testEvent, "Стандартный");
            ticketType.updateQuantity(100);
            ticketType.activate();
            return ticketTypeRepository.save(ticketType);
        });

        TestLogger.info("Подготовка: eventId={}, slug={}, tenantId={}, userId={}",
            testEvent.getId(), testEvent.getSlug(), tenantId, userId);
    }

    /**
     * Генерирует уникальный код подтверждения для регистрации.
     */
    private String generateConfirmationCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(FAKER.random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Nested
    @DisplayName("GET /api/v1/public/events/{slug}")
    class GetBySlug {

        @Test
        @Severity(BLOCKER)
        @DisplayName("анонимный пользователь получает событие без userRegistration")
        void getBySlug_AnonymousUser_Returns200WithoutRegistration() throws Exception {
            TestLogger.info("Тестируем получение события анонимным пользователем: slug={}", testEvent.getSlug());

            AllureSteps.performRequest("GET", "/api/v1/public/events/" + testEvent.getSlug(), () -> {
                try {
                    return mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(testEvent.getId().toString()))
                        .andExpect(jsonPath("$.slug").value(testEvent.getSlug()))
                        .andExpect(jsonPath("$.title").value(testEvent.getTitle()))
                        .andExpect(jsonPath("$.userRegistration").doesNotExist());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TestLogger.info("Анонимный пользователь успешно получил событие без userRegistration");
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("авторизованный пользователь без регистрации получает событие без userRegistration")
        void getBySlug_AuthenticatedUserNoRegistration_Returns200WithoutUserRegistration() throws Exception {
            TestLogger.info("Тестируем получение события авторизованным пользователем без регистрации: userId={}, slug={}",
                userId, testEvent.getSlug());

            AllureSteps.performRequest("GET", "/api/v1/public/events/" + testEvent.getSlug(), () -> {
                try {
                    return mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug())
                            .with(jwt(jwtTokenProvider, userId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(testEvent.getId().toString()))
                        .andExpect(jsonPath("$.slug").value(testEvent.getSlug()))
                        .andExpect(jsonPath("$.userRegistration").doesNotExist());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TestLogger.info("Пользователь без регистрации успешно получил событие без userRegistration");
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("авторизованный пользователь с активной регистрацией получает событие с userRegistration")
        void getBySlug_AuthenticatedUserWithRegistration_Returns200WithUserRegistration() throws Exception {
            // Given: создаём регистрацию пользователя
            Registration registration = AllureSteps.step("Создать регистрацию пользователя", () -> {
                String confirmationCode = generateConfirmationCode();
                Registration reg = Registration.create(
                    testEvent,
                    testTicketType,
                    userId,
                    confirmationCode,
                    FAKER.name().firstName(),
                    FAKER.name().lastName(),
                    FAKER.internet().emailAddress()
                );
                // Статус уже CONFIRMED по умолчанию
                return registrationRepository.save(reg);
            });

            TestLogger.info("Создана регистрация: registrationId={}, userId={}, confirmationCode={}",
                registration.getId(), userId, registration.getConfirmationCode());

            // When/Then
            AllureSteps.performRequest("GET", "/api/v1/public/events/" + testEvent.getSlug(), () -> {
                try {
                    return mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug())
                            .with(jwt(jwtTokenProvider, userId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(testEvent.getId().toString()))
                        .andExpect(jsonPath("$.slug").value(testEvent.getSlug()))
                        .andExpect(jsonPath("$.userRegistration").exists())
                        .andExpect(jsonPath("$.userRegistration.id").value(registration.getId().toString()))
                        .andExpect(jsonPath("$.userRegistration.confirmationCode").value(registration.getConfirmationCode()))
                        .andExpect(jsonPath("$.userRegistration.status").value(RegistrationStatus.CONFIRMED.name()))
                        .andExpect(jsonPath("$.userRegistration.ticketTypeName").value(testTicketType.getName()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TestLogger.info("Пользователь с регистрацией успешно получил событие с userRegistration");
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("отменённая регистрация не включается в userRegistration")
        void getBySlug_CancelledRegistration_ReturnsWithoutUserRegistration() throws Exception {
            // Given: создаём отменённую регистрацию
            AllureSteps.step("Создать отменённую регистрацию", () -> {
                String confirmationCode = generateConfirmationCode();
                Registration reg = Registration.create(
                    testEvent,
                    testTicketType,
                    userId,
                    confirmationCode,
                    FAKER.name().firstName(),
                    FAKER.name().lastName(),
                    FAKER.internet().emailAddress()
                );
                // Статус уже CONFIRMED по умолчанию, отменяем
                reg.cancel();
                return registrationRepository.save(reg);
            });

            TestLogger.info("Создана отменённая регистрация для userId={}", userId);

            // When/Then
            AllureSteps.performRequest("GET", "/api/v1/public/events/" + testEvent.getSlug(), () -> {
                try {
                    return mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug())
                            .with(jwt(jwtTokenProvider, userId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.userRegistration").doesNotExist());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TestLogger.info("Отменённая регистрация корректно исключена из userRegistration");
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("возвращает 404 для несуществующего события")
        void getBySlug_NonExistent_Returns404() throws Exception {
            String nonExistentSlug = "non-existent-slug-" + UUID.randomUUID();
            TestLogger.info("Тестируем получение несуществующего события: slug={}", nonExistentSlug);

            AllureSteps.performRequest("GET", "/api/v1/public/events/" + nonExistentSlug, () -> {
                try {
                    return mockMvc.perform(get("/api/v1/public/events/" + nonExistentSlug))
                        .andExpect(status().isNotFound());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TestLogger.info("Корректно вернули 404 для несуществующего события");
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("возвращает 404 для непубличного события")
        void getBySlug_NotPublic_Returns404() throws Exception {
            // Given: делаем событие непубличным
            AllureSteps.step("Сделать событие непубличным", () -> {
                testEvent.updateVisibility(false, null);
                return eventRepository.save(testEvent);
            });

            TestLogger.info("Событие сделано непубличным: slug={}", testEvent.getSlug());

            // When/Then
            AllureSteps.performRequest("GET", "/api/v1/public/events/" + testEvent.getSlug(), () -> {
                try {
                    return mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug()))
                        .andExpect(status().isNotFound());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TestLogger.info("Корректно вернули 404 для непубличного события");
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("возвращает 404 для черновика события")
        void getBySlug_DraftEvent_Returns404() throws Exception {
            // Given: создаём событие в статусе DRAFT
            String draftTitle = FAKER.book().title();
            Event draftEvent = AllureSteps.createTestEvent(draftTitle, () -> {
                Event event = Event.create(
                    draftTitle,
                    "draft-event-" + UUID.randomUUID().toString().substring(0, 8),
                    Instant.now().plus(7, ChronoUnit.DAYS),
                    "Europe/Moscow"
                );
                event.updateVisibility(true, null);
                // НЕ публикуем событие - оставляем DRAFT
                return eventRepository.save(event);
            });

            TestLogger.info("Создано событие в статусе DRAFT: slug={}", draftEvent.getSlug());

            // When/Then
            AllureSteps.performRequest("GET", "/api/v1/public/events/" + draftEvent.getSlug(), () -> {
                try {
                    return mockMvc.perform(get("/api/v1/public/events/" + draftEvent.getSlug()))
                        .andExpect(status().isNotFound());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TestLogger.info("Корректно вернули 404 для черновика события");
        }
    }
}
