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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.LocationType;
import ru.aqstream.event.api.dto.ParticipantsVisibility;
import ru.aqstream.event.api.exception.EventHasNoTicketTypesException;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RecurrenceRuleRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventLifecycleService")
class EventLifecycleServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RecurrenceRuleRepository recurrenceRuleRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private RecurrenceRuleMapper recurrenceRuleMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private EventAuditService eventAuditService;

    private EventLifecycleService service;

    private static final Faker FAKER = new Faker();

    private UUID tenantId;
    private UUID eventId;
    private String testTitle;
    private Instant testStartsAt;
    private Event testEvent;
    private EventDto testEventDto;

    @BeforeEach
    void setUp() {
        service = new EventLifecycleService(
            eventRepository,
            recurrenceRuleRepository,
            ticketTypeRepository,
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
        testEventDto = createTestEventDto(EventStatus.PUBLISHED);

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

    private EventDto createTestEventDto(EventStatus status) {
        return new EventDto(
            eventId,
            tenantId,
            "Test Organization",
            testTitle,
            "test-slug",
            null, // description
            status,
            testStartsAt,
            null, // endsAt
            "Europe/Moscow",
            LocationType.ONLINE,
            null, // locationAddress
            null, // onlineUrl
            null, // maxCapacity
            null, // registrationOpensAt
            null, // registrationClosesAt
            false,
            ParticipantsVisibility.CLOSED,
            null, // groupId
            null, // registrationFormConfig
            null, // cancelReason
            null, // cancelledAt
            null, // recurrenceRule
            null, // parentEventId
            null, // instanceDate
            Instant.now(),
            Instant.now(),
            null  // userRegistration
        );
    }

    @Nested
    @DisplayName("publish()")
    class Publish {

        @Test
        @DisplayName("Выбрасывает EventHasNoTicketTypesException если нет типов билетов")
        void publish_NoTicketTypes_ThrowsException() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId))
                .thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.existsByEventIdAndActiveIsTrue(eventId))
                .thenReturn(false);

            // when/then
            assertThatThrownBy(() -> service.publish(eventId))
                .isInstanceOf(EventHasNoTicketTypesException.class)
                .hasMessageContaining("хотя бы один тип билета");
        }

        @Test
        @DisplayName("Публикует событие с типами билетов")
        void publish_WithTicketTypes_PublishesSuccessfully() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId))
                .thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.existsByEventIdAndActiveIsTrue(eventId))
                .thenReturn(true);
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(testEventDto);

            // when
            EventDto result = service.publish(eventId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
            verify(eventRepository).save(testEvent);
            verify(eventPublisher).publish(any());
            verify(eventAuditService).logPublished(testEvent);
        }

        @Test
        @DisplayName("Выбрасывает EventNotFoundException если событие не найдено")
        void publish_EventNotFound_ThrowsException() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.publish(eventId))
                .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("unpublish()")
    class Unpublish {

        @Test
        @DisplayName("Снимает с публикации опубликованное событие")
        void unpublish_PublishedEvent_ReturnsEventInDraft() {
            // given
            testEvent.publish();
            EventDto draftDto = createTestEventDto(EventStatus.DRAFT);

            when(eventRepository.findByIdAndTenantId(eventId, tenantId))
                .thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(draftDto);

            // when
            EventDto result = service.unpublish(eventId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
            verify(eventAuditService).logUnpublished(testEvent);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("Отменяет событие с причиной")
        void cancel_WithReason_CancelsSuccessfully() {
            // given
            String reason = FAKER.lorem().sentence();
            EventDto cancelledDto = createTestEventDto(EventStatus.CANCELLED);

            when(eventRepository.findByIdAndTenantId(eventId, tenantId))
                .thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(cancelledDto);

            // when
            EventDto result = service.cancel(eventId, reason);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EventStatus.CANCELLED);
            verify(eventPublisher).publish(any());
            verify(eventAuditService).logCancelled(testEvent, reason);
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
            EventDto completedDto = createTestEventDto(EventStatus.COMPLETED);

            when(eventRepository.findByIdAndTenantId(eventId, tenantId))
                .thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(eventMapper.toDto(eq(testEvent), any())).thenReturn(completedDto);

            // when
            EventDto result = service.complete(eventId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(EventStatus.COMPLETED);
            verify(eventPublisher).publish(any());
            verify(eventAuditService).logCompleted(testEvent);
        }
    }
}
