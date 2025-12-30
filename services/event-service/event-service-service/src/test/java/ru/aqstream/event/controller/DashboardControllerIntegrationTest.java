package ru.aqstream.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.listener.OrganizationEventListener;
import ru.aqstream.user.client.UserClient;

/**
 * Интеграционные тесты для DashboardController.
 *
 * <p>Тестирует:
 * <ul>
 *   <li>Получение статистики для dashboard</li>
 *   <li>RLS изоляцию между tenant'ами</li>
 *   <li>Аутентификацию</li>
 * </ul>
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@DisplayName("DashboardController Integration Tests")
class DashboardControllerIntegrationTest extends SharedServicesTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String BASE_URL = "/api/v1/dashboard";

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
    private RegistrationRepository registrationRepository;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы
        registrationRepository.deleteAll();
        eventRepository.deleteAll();

        // Генерируем идентификаторы
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    /**
     * Создаёт JWT аутентификацию для текущего пользователя и tenant.
     */
    private RequestPostProcessor userAuth() {
        return jwt(jwtTokenProvider, userId, null, tenantId, Set.of("USER"));
    }

    /**
     * Создаёт опубликованное событие в будущем.
     */
    private Event createPublishedEvent() {
        Event event = Event.create(
            FAKER.book().title(),
            "event-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        event.publish();
        return eventRepository.save(event);
    }

    @Nested
    @DisplayName("GET /api/v1/dashboard/stats")
    class GetStats {

        @Test
        @DisplayName("возвращает статистику для dashboard")
        void getStats_Authenticated_ReturnsStats() throws Exception {
            // given: создаём опубликованное событие
            createPublishedEvent();

            // when/then
            mockMvc.perform(get(BASE_URL + "/stats")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEventsCount").value(1))
                .andExpect(jsonPath("$.totalRegistrations").isNumber())
                .andExpect(jsonPath("$.checkedInCount").isNumber())
                .andExpect(jsonPath("$.upcomingEvents").isArray());
        }

        @Test
        @DisplayName("возвращает пустую статистику для нового tenant")
        void getStats_EmptyTenant_ReturnsZeros() throws Exception {
            // given: нет событий для tenant

            // when/then
            mockMvc.perform(get(BASE_URL + "/stats")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEventsCount").value(0))
                .andExpect(jsonPath("$.totalRegistrations").value(0))
                .andExpect(jsonPath("$.checkedInCount").value(0))
                .andExpect(jsonPath("$.upcomingEvents").isEmpty());
        }

        @Test
        @DisplayName("возвращает 401 без аутентификации")
        void getStats_NotAuthenticated_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("RLS Isolation")
    class RlsIsolation {

        @Test
        @DisplayName("статистика изолирована по tenant")
        void getStats_DifferentTenant_ReturnsOwnStats() throws Exception {
            // given: создаём событие для текущего tenant
            createPublishedEvent();

            // when: запрашиваем статистику от другого tenant
            UUID otherTenantId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            mockMvc.perform(get(BASE_URL + "/stats")
                    .with(jwt(jwtTokenProvider, otherUserId, null, otherTenantId, Set.of("USER"))))
                .andExpect(status().isOk())
                // then: статистика пустая (события другого tenant не видны)
                .andExpect(jsonPath("$.activeEventsCount").value(0))
                .andExpect(jsonPath("$.upcomingEvents").isEmpty());
        }
    }
}
