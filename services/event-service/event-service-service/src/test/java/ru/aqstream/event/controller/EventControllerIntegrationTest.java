package ru.aqstream.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;
import static ru.aqstream.common.test.SecurityTestUtils.jwtAdmin;
import static io.qameta.allure.SeverityLevel.BLOCKER;
import static io.qameta.allure.SeverityLevel.CRITICAL;
import static io.qameta.allure.SeverityLevel.NORMAL;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.context.annotation.Import;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.event.listener.OrganizationEventListener;
import ru.aqstream.user.api.dto.OrganizationMembershipDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.client.UserClient;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.test.allure.AllureSteps;
import ru.aqstream.common.test.allure.TestLogger;
import ru.aqstream.common.web.GlobalExceptionHandler;
import ru.aqstream.event.api.dto.CreateEventRequest;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.LocationType;
import ru.aqstream.event.api.dto.UpdateEventRequest;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventAuditLogRepository;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

@IntegrationTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@Feature(AllureFeatures.Features.EVENT_MANAGEMENT)
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

    @Autowired
    private EventAuditLogRepository eventAuditLogRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    private UUID tenantId;
    private UUID userId;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы (порядок важен из-за FK)
        ticketTypeRepository.deleteAll();
        eventAuditLogRepository.deleteAll();
        eventRepository.deleteAll();

        // Генерируем userId и tenant_id
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        // Мок проверки членства — пользователь является OWNER организации
        when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
            .thenReturn(OrganizationMembershipDto.member(tenantId, userId, OrganizationRole.OWNER));

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
    @Story(AllureFeatures.Stories.EVENT_CRUD)
    @DisplayName("POST /api/v1/events")
    class Create {

        @Test
        @Severity(BLOCKER)
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
                null,
                null, // organizationId (null для обычных пользователей)
                null  // recurrenceRule
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
        @Severity(NORMAL)
        @DisplayName("возвращает 400 для события без обязательных полей")
        void create_MissingRequiredFields_ReturnsBadRequest() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.EVENT_CRUD)
    @DisplayName("GET /api/v1/events/{id}")
    class GetById {

        @Test
        @Severity(BLOCKER)
        @DisplayName("возвращает событие по ID")
        void getById_ExistingEvent_ReturnsEvent() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEvent.getId().toString()))
                .andExpect(jsonPath("$.title").value(testEvent.getTitle()));
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает 404 для несуществующего события")
        void getById_NonExistingEvent_ReturnsNotFound() throws Exception {
            UUID nonExistingId = UUID.randomUUID();

            mockMvc.perform(get(BASE_URL + "/" + nonExistingId)
                    .with(userAuth()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.EVENT_CRUD)
    @DisplayName("PUT /api/v1/events/{id}")
    class Update {

        @Test
        @Severity(CRITICAL)
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
    @Story(AllureFeatures.Stories.EVENT_CRUD)
    @DisplayName("DELETE /api/v1/events/{id}")
    class Delete {

        @Test
        @Severity(CRITICAL)
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
    @Story(AllureFeatures.Stories.EVENT_LIFECYCLE)
    @DisplayName("POST /api/v1/events/{id}/publish")
    class Publish {

        @Test
        @Severity(CRITICAL)
        @DisplayName("публикует событие с типами билетов")
        void publish_EventWithTicketTypes_ReturnsPublishedEvent() throws Exception {
            // Создаём тип билета для события
            TicketType ticketType = TicketType.create(testEvent, FAKER.commerce().productName());
            ticketTypeRepository.save(ticketType);

            mockMvc.perform(post(BASE_URL + "/" + testEvent.getId() + "/publish")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

            Event updated = eventRepository.findById(testEvent.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает 400 при попытке опубликовать событие без типов билетов")
        void publish_EventWithoutTicketTypes_ReturnsBadRequest() throws Exception {
            // Событие создано в setUp без типов билетов

            mockMvc.perform(post(BASE_URL + "/" + testEvent.getId() + "/publish")
                    .with(userAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("event_has_no_ticket_types"))
                .andExpect(jsonPath("$.message").value("Для публикации события необходимо добавить хотя бы один тип билета"));
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.EVENT_LIFECYCLE)
    @DisplayName("POST /api/v1/events/{id}/cancel")
    class Cancel {

        @Test
        @Severity(CRITICAL)
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
        @Severity(BLOCKER)
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
        @Severity(BLOCKER)
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

    @Nested
    @Story(AllureFeatures.Stories.EVENT_PERMISSIONS)
    @DisplayName("Access Control")
    class AccessControl {

        @Test
        @Severity(BLOCKER)
        @DisplayName("пользователь без tenantId получает 403")
        void create_UserWithoutTenant_Returns403() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                FAKER.book().title(),
                null,
                Instant.now().plus(14, ChronoUnit.DAYS),
                null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            // JWT без tenantId (null)
            mockMvc.perform(post(BASE_URL)
                    .with(jwt(jwtTokenProvider, userId, null, null, Set.of("USER")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("админ с organizationId создаёт событие")
        void create_AdminWithOrganizationId_ReturnsCreated() throws Exception {
            UUID targetOrganizationId = UUID.randomUUID();
            CreateEventRequest request = new CreateEventRequest(
                FAKER.book().title(),
                null,
                Instant.now().plus(14, ChronoUnit.DAYS),
                null, null, null, null, null, null, null, null, null, null, null,
                targetOrganizationId, // organizationId для админа
                null
            );

            mockMvc.perform(post(BASE_URL)
                    .with(jwtAdmin(jwtTokenProvider, userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("админ без organizationId и без tenantId получает 403")
        void create_AdminWithoutOrganization_Returns403() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                FAKER.book().title(),
                null,
                Instant.now().plus(14, ChronoUnit.DAYS),
                null, null, null, null, null, null, null, null, null, null, null,
                null, // без organizationId
                null
            );

            // Админ без tenantId в JWT и без organizationId в request
            mockMvc.perform(post(BASE_URL)
                    .with(jwt(jwtTokenProvider, userId, null, null, Set.of("USER", "ADMIN")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("запрос без аутентификации получает 401")
        void create_Unauthenticated_Returns401() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                FAKER.book().title(),
                null,
                Instant.now().plus(14, ChronoUnit.DAYS),
                null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("не-член организации получает 403")
        void create_NotMember_Returns403() throws Exception {
            // Мокаем что пользователь НЕ является членом организации
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.notMember(tenantId, userId));

            CreateEventRequest request = new CreateEventRequest(
                FAKER.book().title(),
                null,
                Instant.now().plus(14, ChronoUnit.DAYS),
                null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            mockMvc.perform(post(BASE_URL)
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("обычный пользователь не может просматривать список событий")
        void findAll_RegularUser_Returns403() throws Exception {
            // Мокаем что пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, null));

            mockMvc.perform(get(BASE_URL)
                    .with(userAuth()))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("обычный пользователь не может просматривать событие по ID")
        void getById_RegularUser_Returns403() throws Exception {
            // Мокаем что пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, null));

            mockMvc.perform(get(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth()))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("обычный пользователь не может обновлять событие")
        void update_RegularUser_Returns403() throws Exception {
            // Мокаем что пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, null));

            UpdateEventRequest request = new UpdateEventRequest(
                "Новое название", null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );

            mockMvc.perform(put(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("обычный пользователь не может удалять событие")
        void delete_RegularUser_Returns403() throws Exception {
            // Мокаем что пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, null));

            mockMvc.perform(delete(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth()))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("обычный пользователь не может публиковать событие")
        void publish_RegularUser_Returns403() throws Exception {
            // Мокаем что пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, null));

            mockMvc.perform(post(BASE_URL + "/" + testEvent.getId() + "/publish")
                    .with(userAuth()))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("обычный пользователь не может отменять событие")
        void cancel_RegularUser_Returns403() throws Exception {
            // Мокаем что пользователь член организации, но без роли OWNER/MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, null));

            mockMvc.perform(post(BASE_URL + "/" + testEvent.getId() + "/cancel")
                    .with(userAuth()))
                .andExpect(status().isForbidden());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("MODERATOR может обновлять событие")
        void update_Moderator_Returns200() throws Exception {
            // Мокаем что пользователь MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, OrganizationRole.MODERATOR));

            UpdateEventRequest request = new UpdateEventRequest(
                "Название от модератора", null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );

            mockMvc.perform(put(BASE_URL + "/" + testEvent.getId())
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Название от модератора"));
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("MODERATOR может публиковать событие")
        void publish_Moderator_Returns200() throws Exception {
            // Создаём тип билета для события
            TicketType ticketType = TicketType.create(testEvent, FAKER.commerce().productName());
            ticketTypeRepository.save(ticketType);

            // Мокаем что пользователь MODERATOR
            when(userClient.getMembershipRole(any(UUID.class), any(UUID.class)))
                .thenReturn(OrganizationMembershipDto.member(tenantId, userId, OrganizationRole.MODERATOR));

            mockMvc.perform(post(BASE_URL + "/" + testEvent.getId() + "/publish")
                    .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }
    }
}
