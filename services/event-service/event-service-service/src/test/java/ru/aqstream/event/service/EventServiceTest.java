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
import ru.aqstream.event.api.exception.InvalidEventStatusTransitionException;
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
            eventAuditService
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
            testTitle,
            "test-slug",
            null,
            EventStatus.DRAFT,
            testStartsAt,
            null,
            "Europe/Moscow",
            LocationType.ONLINE,
            null,
            null,
            null,
            null,
            null,
            false,
            ParticipantsVisibility.CLOSED,
            null,
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
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            service.publish(eventId);

            // then
            assertThat(testEvent.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Выбрасывает исключение при публикации события с датой в прошлом")
        void publish_EventInPast_ThrowsException() {
            // given
            Event pastEvent = Event.create(testTitle, "test-slug",
                Instant.now().minus(1, ChronoUnit.DAYS), "Europe/Moscow");
            try {
                var idField = pastEvent.getClass().getSuperclass().getSuperclass().getSuperclass()
                    .getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(pastEvent, eventId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(pastEvent));

            // when/then
            assertThatThrownBy(() -> service.publish(eventId))
                .isInstanceOf(EventInPastException.class);
        }

        @Test
        @DisplayName("Выбрасывает исключение при публикации опубликованного события")
        void publish_AlreadyPublishedEvent_ThrowsException() {
            // given
            testEvent.publish(); // переводим в PUBLISHED

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));

            // when/then
            assertThatThrownBy(() -> service.publish(eventId))
                .isInstanceOf(InvalidEventStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("unpublish()")
    class Unpublish {

        @Test
        @DisplayName("Снимает с публикации опубликованное событие")
        void unpublish_PublishedEvent_ReturnsToD() {
            // given
            testEvent.publish(); // сначала публикуем

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            service.unpublish(eventId);

            // then
            assertThat(testEvent.getStatus()).isEqualTo(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("Выбрасывает исключение для события не в статусе PUBLISHED")
        void unpublish_DraftEvent_ThrowsException() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));

            // when/then
            assertThatThrownBy(() -> service.unpublish(eventId))
                .isInstanceOf(InvalidEventStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("Отменяет событие в статусе DRAFT")
        void cancel_DraftEvent_CancelsSuccessfully() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            service.cancel(eventId);

            // then
            assertThat(testEvent.getStatus()).isEqualTo(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("Отменяет опубликованное событие")
        void cancel_PublishedEvent_CancelsSuccessfully() {
            // given
            testEvent.publish();

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            service.cancel(eventId);

            // then
            assertThat(testEvent.getStatus()).isEqualTo(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("Выбрасывает исключение для завершённого события")
        void cancel_CompletedEvent_ThrowsException() {
            // given
            testEvent.publish();
            testEvent.complete();

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));

            // when/then
            assertThatThrownBy(() -> service.cancel(eventId))
                .isInstanceOf(InvalidEventStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("Завершает опубликованное событие")
        void complete_PublishedEvent_CompletesSuccessfully() {
            // given
            testEvent.publish();

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            service.complete(eventId);

            // then
            assertThat(testEvent.getStatus()).isEqualTo(EventStatus.COMPLETED);
        }

        @Test
        @DisplayName("Выбрасывает исключение для черновика")
        void complete_DraftEvent_ThrowsException() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));

            // when/then
            assertThatThrownBy(() -> service.complete(eventId))
                .isInstanceOf(InvalidEventStatusTransitionException.class);
        }
    }
}
