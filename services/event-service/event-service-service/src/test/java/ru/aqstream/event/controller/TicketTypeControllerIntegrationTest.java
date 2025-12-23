package ru.aqstream.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

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
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.api.dto.CreateTicketTypeRequest;
import ru.aqstream.event.api.dto.UpdateTicketTypeRequest;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@DisplayName("TicketTypeController Integration Tests")
class TicketTypeControllerIntegrationTest extends SharedServicesTestContainer {

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

    private UUID tenantId;
    private UUID userId;
    private Event testEvent;
    private TicketType testTicketType;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();

        // Генерируем userId и tenant_id
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        // Создаём тестовое событие
        testEvent = Event.create(
            FAKER.book().title(),
            "test-event-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        testEvent = eventRepository.save(testEvent);

        // Создаём тестовый тип билета
        testTicketType = TicketType.create(testEvent, FAKER.commerce().productName());
        testTicketType.setDescription("Тестовое описание");
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
     * Создаёт base URL для типов билетов события.
     */
    private String baseUrl() {
        return "/api/v1/events/" + testEvent.getId() + "/ticket-types";
    }

    @Nested
    @DisplayName("POST /api/v1/events/{eventId}/ticket-types")
    class Create {

        @Test
        @DisplayName("создаёт тип билета с валидными данными")
        void create_ValidRequest_ReturnsCreated() throws Exception {
            String name = FAKER.commerce().productName();
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                name,
                "Описание типа билета",
                50,
                null,
                null,
                null
            );

            mockMvc.perform(post(baseUrl())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value("Описание типа билета"))
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.priceCents").value(0))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.isSoldOut").value(false));
        }

        @Test
        @DisplayName("создаёт тип билета с unlimited количеством")
        void create_UnlimitedQuantity_ReturnsCreated() throws Exception {
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                FAKER.commerce().productName(),
                null,
                null, // unlimited
                null,
                null,
                null
            );

            mockMvc.perform(post(baseUrl())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").isEmpty())
                .andExpect(jsonPath("$.available").isEmpty());
        }

        @Test
        @DisplayName("возвращает 400 для типа билета без названия")
        void create_MissingName_ReturnsBadRequest() throws Exception {
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                null, null, null, null, null, null
            );

            mockMvc.perform(post(baseUrl())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего события")
        void create_NonExistingEvent_ReturnsNotFound() throws Exception {
            UUID nonExistingEventId = UUID.randomUUID();
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                FAKER.commerce().productName(), null, null, null, null, null
            );

            mockMvc.perform(post("/api/v1/events/" + nonExistingEventId + "/ticket-types")
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("возвращает 409 для отменённого события")
        void create_CancelledEvent_ReturnsConflict() throws Exception {
            // given: отменяем событие
            testEvent.cancel();
            eventRepository.save(testEvent);

            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                FAKER.commerce().productName(), null, null, null, null, null
            );

            // when/then
            mockMvc.perform(post(baseUrl())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/{eventId}/ticket-types")
    class FindAll {

        @Test
        @DisplayName("возвращает все типы билетов события")
        void findAll_ExistingEvent_ReturnsTicketTypes() throws Exception {
            mockMvc.perform(get(baseUrl())
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(testTicketType.getId().toString()))
                .andExpect(jsonPath("$[0].name").value(testTicketType.getName()));
        }

        @Test
        @DisplayName("возвращает пустой список если нет типов билетов")
        void findAll_NoTicketTypes_ReturnsEmptyList() throws Exception {
            ticketTypeRepository.deleteAll();

            mockMvc.perform(get(baseUrl())
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/{eventId}/ticket-types/{ticketTypeId}")
    class GetById {

        @Test
        @DisplayName("возвращает тип билета по ID")
        void getById_ExistingTicketType_ReturnsTicketType() throws Exception {
            mockMvc.perform(get(baseUrl() + "/" + testTicketType.getId())
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testTicketType.getId().toString()))
                .andExpect(jsonPath("$.name").value(testTicketType.getName()))
                .andExpect(jsonPath("$.eventId").value(testEvent.getId().toString()));
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего типа билета")
        void getById_NonExistingTicketType_ReturnsNotFound() throws Exception {
            UUID nonExistingId = UUID.randomUUID();

            mockMvc.perform(get(baseUrl() + "/" + nonExistingId)
                    .with(userAuth()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/events/{eventId}/ticket-types/{ticketTypeId}")
    class Update {

        @Test
        @DisplayName("обновляет название типа билета")
        void update_ValidName_ReturnsUpdatedTicketType() throws Exception {
            String newName = "Обновлённое название";
            UpdateTicketTypeRequest request = new UpdateTicketTypeRequest(
                newName, null, null, null, null, null, null
            );

            mockMvc.perform(put(baseUrl() + "/" + testTicketType.getId())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));

            // Проверяем что обновилось в БД
            TicketType updated = ticketTypeRepository.findById(testTicketType.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("обновляет количество билетов")
        void update_ValidQuantity_ReturnsUpdatedTicketType() throws Exception {
            UpdateTicketTypeRequest request = new UpdateTicketTypeRequest(
                null, null, 200, null, null, null, null
            );

            mockMvc.perform(put(baseUrl() + "/" + testTicketType.getId())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(200));
        }

        @Test
        @DisplayName("деактивирует тип билета")
        void update_IsActiveFalse_DeactivatesTicketType() throws Exception {
            UpdateTicketTypeRequest request = new UpdateTicketTypeRequest(
                null, null, null, null, null, null, false
            );

            mockMvc.perform(put(baseUrl() + "/" + testTicketType.getId())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/events/{eventId}/ticket-types/{ticketTypeId}")
    class Delete {

        @Test
        @DisplayName("удаляет тип билета без регистраций")
        void delete_NoRegistrations_ReturnsNoContent() throws Exception {
            mockMvc.perform(delete(baseUrl() + "/" + testTicketType.getId())
                    .with(userAuth()))
                .andExpect(status().isNoContent());

            // Проверяем что удалено
            assertThat(ticketTypeRepository.findById(testTicketType.getId())).isEmpty();
        }

        @Test
        @DisplayName("возвращает 409 при удалении типа билета с продажами")
        void delete_HasSoldTickets_ReturnsConflict() throws Exception {
            // given: добавляем проданный билет
            testTicketType.incrementSoldCount();
            ticketTypeRepository.save(testTicketType);

            // when/then
            mockMvc.perform(delete(baseUrl() + "/" + testTicketType.getId())
                    .with(userAuth()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ticket_type_has_registrations"));

            // Тип билета не должен быть удалён
            assertThat(ticketTypeRepository.findById(testTicketType.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/events/{eventId}/ticket-types/{ticketTypeId}/deactivate")
    class Deactivate {

        @Test
        @DisplayName("деактивирует тип билета")
        void deactivate_ActiveTicketType_ReturnsDeactivatedTicketType() throws Exception {
            mockMvc.perform(post(baseUrl() + "/" + testTicketType.getId() + "/deactivate")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

            // Проверяем что обновилось в БД
            TicketType updated = ticketTypeRepository.findById(testTicketType.getId()).orElseThrow();
            assertThat(updated.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("RLS Isolation")
    class RlsIsolation {

        @Test
        @DisplayName("типы билетов другого tenant не видны")
        void findAll_DifferentTenant_ReturnsNotFound() throws Exception {
            UUID otherTenantId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            // Запрос к событию другого tenant должен вернуть 404
            mockMvc.perform(get(baseUrl())
                    .with(jwt(jwtTokenProvider, otherUserId, null, otherTenantId, Set.of("USER"))))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Бизнес-логика")
    class BusinessLogic {

        @Test
        @DisplayName("available вычисляется правильно")
        void create_WithQuantity_CalculatesAvailable() throws Exception {
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                FAKER.commerce().productName(),
                null,
                50,
                null,
                null,
                null
            );

            mockMvc.perform(post(baseUrl())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.soldCount").value(0))
                .andExpect(jsonPath("$.reservedCount").value(0))
                .andExpect(jsonPath("$.available").value(50))
                .andExpect(jsonPath("$.isSoldOut").value(false));
        }

        @Test
        @DisplayName("isSoldOut=true когда все билеты проданы")
        void update_AllSold_IsSoldOutTrue() throws Exception {
            // given: устанавливаем quantity=1 и продаём билет
            testTicketType.updateQuantity(1);
            testTicketType.incrementSoldCount();
            ticketTypeRepository.save(testTicketType);

            // when/then
            mockMvc.perform(get(baseUrl() + "/" + testTicketType.getId())
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.soldCount").value(1))
                .andExpect(jsonPath("$.available").value(0))
                .andExpect(jsonPath("$.isSoldOut").value(true));
        }
    }

    @Nested
    @DisplayName("Аутентификация")
    class Authentication {

        @Test
        @DisplayName("возвращает 401 без токена")
        void findAll_NoAuth_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get(baseUrl()))
                .andExpect(status().isUnauthorized());
        }
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
