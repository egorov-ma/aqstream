package ru.aqstream.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

/**
 * Интеграционные тесты для публичного API типов билетов.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@DisplayName("TicketTypeController Public API Integration Tests")
class TicketTypeControllerPublicIntegrationTest extends SharedServicesTestContainer {

    private static final Faker FAKER = new Faker();

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

    private UUID tenantId;
    private Event testEvent;
    private TicketType testTicketType;

    @BeforeEach
    void setUp() {
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();

        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        testEvent = Event.create(
            FAKER.book().title(),
            "test-event-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        testEvent = eventRepository.save(testEvent);

        testTicketType = TicketType.create(testEvent, FAKER.commerce().productName());
        testTicketType.setDescription("Тестовое описание");
        testTicketType.updateQuantity(100);
        testTicketType = ticketTypeRepository.save(testTicketType);
    }

    @Nested
    @DisplayName("GET /api/v1/public/events/{slug}/ticket-types")
    class PublicTicketTypes {

        @Test
        @DisplayName("возвращает типы билетов публичного события без авторизации")
        void getPublicTicketTypes_PublicEvent_ReturnsTicketTypes() throws Exception {
            // given: делаем событие публичным и опубликованным
            testEvent.publish();
            testEvent.updateVisibility(true, null);
            eventRepository.save(testEvent);

            // when/then
            mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug() + "/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(testTicketType.getName()));
        }

        @Test
        @DisplayName("возвращает 404 для непубличного события")
        void getPublicTicketTypes_NotPublic_ReturnsNotFound() throws Exception {
            // given: событие не публичное
            testEvent.publish();
            testEvent.updateVisibility(false, null);
            eventRepository.save(testEvent);

            // when/then
            mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug() + "/ticket-types"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего события")
        void getPublicTicketTypes_NotExists_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/public/events/non-existent-slug/ticket-types"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("фильтрует типы билетов с закрытыми продажами")
        void getPublicTicketTypes_SalesClosed_FiltersTicketTypes() throws Exception {
            // given: делаем событие публичным
            testEvent.publish();
            testEvent.updateVisibility(true, null);
            eventRepository.save(testEvent);

            // Устанавливаем период продаж в прошлом
            testTicketType.updateSalesPeriod(
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(5, ChronoUnit.DAYS)
            );
            ticketTypeRepository.save(testTicketType);

            // when/then: тип билета отфильтрован
            mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug() + "/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("возвращает типы билетов с открытыми продажами")
        void getPublicTicketTypes_SalesOpen_ReturnsTicketTypes() throws Exception {
            // given: делаем событие публичным
            testEvent.publish();
            testEvent.updateVisibility(true, null);
            eventRepository.save(testEvent);

            // Устанавливаем активный период продаж
            testTicketType.updateSalesPeriod(
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(7, ChronoUnit.DAYS)
            );
            ticketTypeRepository.save(testTicketType);

            // when/then
            mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug() + "/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("не возвращает неактивные типы билетов")
        void getPublicTicketTypes_InactiveType_FiltersInactive() throws Exception {
            // given: делаем событие публичным
            testEvent.publish();
            testEvent.updateVisibility(true, null);
            eventRepository.save(testEvent);

            // Деактивируем тип билета
            testTicketType.deactivate();
            ticketTypeRepository.save(testTicketType);

            // when/then
            mockMvc.perform(get("/api/v1/public/events/" + testEvent.getSlug() + "/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
