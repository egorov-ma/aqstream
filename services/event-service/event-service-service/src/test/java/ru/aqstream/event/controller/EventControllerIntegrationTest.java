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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.context.annotation.Import;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.event.listener.OrganizationEventListener;
import ru.aqstream.user.client.UserClient;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.api.dto.CreateEventRequest;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.LocationType;
import ru.aqstream.event.api.dto.UpdateEventRequest;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;

@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@DisplayName("EventController Integration Tests")
class EventControllerIntegrationTest extends SharedServicesTestContainer {

    private static final Faker FAKER = new Faker();
    private static final String BASE_URL = "/api/v1/events";

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

    private UUID tenantId;
    private UUID userId;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        // Очищаем таблицу
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
    }

    /**
     * Создаёт JWT аутентификацию для текущего пользователя и tenant.
     */
    private RequestPostProcessor userAuth() {
        return jwt(jwtTokenProvider, userId, null, tenantId, Set.of("USER"));
    }

    @Nested
    @DisplayName("POST /api/v1/events")
    class Create {

        @Test
        @DisplayName("создаёт событие с валидными данными")
        void create_ValidRequest_ReturnsCreated() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                FAKER.book().title(),
                "Описание события",
                Instant.now().plus(14, ChronoUnit.DAYS),
                Instant.now().plus(14, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                "Europe/Moscow",
                LocationType.ONLINE,
                null,
                "https://zoom.us/j/123456",
                100,
                null,
                null,
                false,
                null,
                null
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(request.title()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.locationType").value("ONLINE"));
        }

        @Test
        @DisplayName("возвращает 400 для события без обязательных полей")
        void create_MissingRequiredFields_ReturnsBadRequest() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/{id}")
    class GetById {

        @Test
        @DisplayName("возвращает событие по ID")
        void getById_ExistingEvent_ReturnsEvent() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEvent.getId().toString()))
                .andExpect(jsonPath("$.title").value(testEvent.getTitle()));
        }

        @Test
        @DisplayName("возвращает 404 для несуществующего события")
        void getById_NonExistingEvent_ReturnsNotFound() throws Exception {
            UUID nonExistingId = UUID.randomUUID();

            mockMvc.perform(get(BASE_URL + "/" + nonExistingId)
                    .with(userAuth()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/events/{id}")
    class Update {

        @Test
        @DisplayName("обновляет название события")
        void update_ValidRequest_ReturnsUpdatedEvent() throws Exception {
            String newTitle = "Обновлённое название";
            UpdateEventRequest request = new UpdateEventRequest(
                newTitle, null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );

            mockMvc.perform(put(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(newTitle));

            // Проверяем что обновилось в БД
            Event updated = eventRepository.findById(testEvent.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo(newTitle);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/events/{id}")
    class Delete {

        @Test
        @DisplayName("удаляет событие (soft delete)")
        void delete_ExistingEvent_ReturnsNoContent() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth()))
                .andExpect(status().isNoContent());

            // Событие должно быть soft deleted
            // (не будет найдено из-за @SQLRestriction)
            assertThat(eventRepository.findById(testEvent.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/events/{id}/publish")
    class Publish {

        @Test
        @DisplayName("публикует событие в статусе DRAFT")
        void publish_DraftEvent_ReturnsPublishedEvent() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + testEvent.getId() + "/publish")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

            Event updated = eventRepository.findById(testEvent.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/events/{id}/cancel")
    class Cancel {

        @Test
        @DisplayName("отменяет событие")
        void cancel_ExistingEvent_ReturnsCancelledEvent() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + testEvent.getId() + "/cancel")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

            Event updated = eventRepository.findById(testEvent.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(EventStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("RLS Isolation")
    class RlsIsolation {

        @Test
        @DisplayName("события другого tenant не видны")
        void getById_DifferentTenant_ReturnsNotFound() throws Exception {
            // given: создаём событие для первого tenant
            UUID otherTenantId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            // when: запрашиваем событие с другого tenant
            mockMvc.perform(get(BASE_URL + "/" + testEvent.getId())
                    .with(jwt(jwtTokenProvider, otherUserId, null, otherTenantId, Set.of("USER"))))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("список событий фильтруется по tenant")
        void findAll_DifferentTenant_ReturnsEmptyList() throws Exception {
            // given: событие существует для tenantId
            UUID otherTenantId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            // when: запрашиваем список с другого tenant
            mockMvc.perform(get(BASE_URL)
                    .with(jwt(jwtTokenProvider, otherUserId, null, otherTenantId, Set.of("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }
}
