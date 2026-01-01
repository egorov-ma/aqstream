package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.CreateEventRequest;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.LocationType;
import ru.aqstream.event.api.dto.ParticipantsVisibility;
import ru.aqstream.event.api.dto.UpdateEventRequest;
import ru.aqstream.event.api.exception.EventInPastException;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.api.exception.EventNotEditableException;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RecurrenceRuleRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RecurrenceRuleRepository recurrenceRuleRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private RecurrenceRuleMapper recurrenceRuleMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private EventAuditService eventAuditService;

    @Mock
    private EventLifecycleService eventLifecycleService;

    @Mock
    private OrganizationNameResolver organizationNameResolver;

    private EventService service;

    private static final Faker FAKER = new Faker();

    private UUID tenantId;
    private UUID eventId;
    private String testTitle;
    private Instant testStartsAt;
    private Event testEvent;
    private EventDto testEventDto;

    @BeforeEach
    void setUp() {
        service = new EventService(
            eventRepository,
            recurrenceRuleRepository,
            eventMapper,
            recurrenceRuleMapper,
            eventPublisher,
            eventAuditService,
            eventLifecycleService,
            organizationNameResolver
        );

        tenantId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        testTitle = FAKER.book().title();
        testStartsAt = Instant.now().plus(7, ChronoUnit.DAYS);

        testEvent = createTestEvent();
        testEventDto = createTestEventDto();

        // Устанавливаем tenant context
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Event createTestEvent() {
        Event event = Event.create(testTitle, "test-slug", testStartsAt, "Europe/Moscow");
        // Используем reflection для установки id (в реальности устанавливается JPA)
        try {
            var idField = event.getClass().getSuperclass().getSuperclass().getSuperclass()
                .getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, eventId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    private EventDto createTestEventDto() {
        return new EventDto(
            eventId,
            tenantId,
            "Test Organization", // organizerName
            testTitle,
            "test-slug",
            null, // description
            EventStatus.DRAFT,
            testStartsAt,
            null, // endsAt
            "Europe/Moscow",
            LocationType.ONLINE,
            null, // locationAddress
            null, // onlineUrl
            null, // maxCapacity
            null, // registrationOpensAt
            null, // registrationClosesAt
            false, // isPublic
            ParticipantsVisibility.CLOSED,
            null, // groupId
            null, // registrationFormConfig
            null, // cancelReason
            null, // cancelledAt
            null, // recurrenceRule
            null, // parentEventId
            null, // instanceDate
            Instant.now(),
            Instant.now()
        );
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Создаёт событие в статусе DRAFT")
        void create_ValidRequest_ReturnsEventInDraftStatus() {
            // given
            CreateEventRequest request = new CreateEventRequest(
                testTitle, null, testStartsAt, null, null, null, null, null,
                null, null, null, null, null, null, null
            );

            when(eventRepository.existsBySlugAndTenantId(any(), eq(tenantId))).thenReturn(false);
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            EventDto result = service.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EventStatus.DRAFT);

            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(eventCaptor.capture());

            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getTitle()).isEqualTo(testTitle);
            assertThat(savedEvent.getStatus()).isEqualTo(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("Генерирует уникальный slug из title")
        void create_ValidTitle_GeneratesSlug() {
            // given
            CreateEventRequest request = new CreateEventRequest(
                "Конференция Spring 2025", null, testStartsAt, null, null, null, null, null,
                null, null, null, null, null, null, null
            );

            when(eventRepository.existsBySlugAndTenantId(any(), eq(tenantId))).thenReturn(false);
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            service.create(request);

            // then
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(eventCaptor.capture());

            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getSlug()).isNotBlank();
            assertThat(savedEvent.getSlug()).doesNotContain(" ");
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Возвращает событие по ID")
        void getById_ExistingEvent_ReturnsEvent() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            EventDto result = service.getById(eventId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("Выбрасывает EventNotFoundException если событие не найдено")
        void getById_NonExistingEvent_ThrowsException() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.getById(eventId))
                .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Обновляет название события")
        void update_ValidTitle_UpdatesTitle() {
            // given
            String newTitle = FAKER.book().title();
            UpdateEventRequest request = new UpdateEventRequest(
                newTitle, null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            service.update(eventId, request);

            // then
            verify(eventRepository).save(any(Event.class));
            assertThat(testEvent.getTitle()).isEqualTo(newTitle);
        }

        @Test
        @DisplayName("Выбрасывает исключение при редактировании отменённого события")
        void update_CancelledEvent_ThrowsException() {
            // given
            testEvent.cancel();
            UpdateEventRequest request = new UpdateEventRequest(
                "Новое название", null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));

            // when/then
            assertThatThrownBy(() -> service.update(eventId, request))
                .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("Выбрасывает исключение при редактировании завершённого события")
        void update_CompletedEvent_ThrowsException() {
            // given
            testEvent.publish();
            testEvent.complete();
            UpdateEventRequest request = new UpdateEventRequest(
                "Новое название", null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));

            // when/then
            assertThatThrownBy(() -> service.update(eventId, request))
                .isInstanceOf(EventNotEditableException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Выполняет soft delete события")
        void delete_ExistingEvent_SoftDeletes() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

            // when
            service.delete(eventId);

            // then
            assertThat(testEvent.isDeleted()).isTrue();
            verify(eventRepository).save(testEvent);
        }
    }

    @Nested
    @DisplayName("publish()")
    class Publish {

        @Test
        @DisplayName("Публикует событие в статусе DRAFT")
        void publish_DraftEvent_PublishesSuccessfully() {
            // given - lifecycle метод делегируется в EventLifecycleService
            when(eventLifecycleService.publish(eventId)).thenReturn(testEventDto);

            // when
            EventDto result = service.publish(eventId);

            // then - проверяем делегирование
            assertThat(result).isEqualTo(testEventDto);
            verify(eventLifecycleService).publish(eventId);
        }

        @Test
        @DisplayName("Пробрасывает исключение от EventLifecycleService")
        void publish_LifecycleServiceThrows_PropagatesException() {
            // given
            when(eventLifecycleService.publish(eventId))
                .thenThrow(new EventInPastException());

            // when/then
            assertThatThrownBy(() -> service.publish(eventId))
                .isInstanceOf(EventInPastException.class);
        }
    }

    @Nested
    @DisplayName("unpublish()")
    class Unpublish {

        @Test
        @DisplayName("Делегирует unpublish в EventLifecycleService")
        void unpublish_DelegatesSuccessfully() {
            // given
            when(eventLifecycleService.unpublish(eventId)).thenReturn(testEventDto);

            // when
            EventDto result = service.unpublish(eventId);

            // then
            assertThat(result).isEqualTo(testEventDto);
            verify(eventLifecycleService).unpublish(eventId);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("Делегирует cancel в EventLifecycleService")
        void cancel_DelegatesSuccessfully() {
            // given
            when(eventLifecycleService.cancel(eventId)).thenReturn(testEventDto);

            // when
            EventDto result = service.cancel(eventId);

            // then
            assertThat(result).isEqualTo(testEventDto);
            verify(eventLifecycleService).cancel(eventId);
        }

        @Test
        @DisplayName("Делегирует cancel с reason в EventLifecycleService")
        void cancel_WithReason_DelegatesSuccessfully() {
            // given
            String reason = "Отмена мероприятия";
            when(eventLifecycleService.cancel(eventId, reason)).thenReturn(testEventDto);

            // when
            EventDto result = service.cancel(eventId, reason);

            // then
            assertThat(result).isEqualTo(testEventDto);
            verify(eventLifecycleService).cancel(eventId, reason);
        }
    }

    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("Делегирует complete в EventLifecycleService")
        void complete_DelegatesSuccessfully() {
            // given
            when(eventLifecycleService.complete(eventId)).thenReturn(testEventDto);

            // when
            EventDto result = service.complete(eventId);

            // then
            assertThat(result).isEqualTo(testEventDto);
            verify(eventLifecycleService).complete(eventId);
        }
    }
}
