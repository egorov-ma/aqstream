package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import ru.aqstream.event.api.dto.CreateTicketTypeRequest;
import ru.aqstream.event.api.dto.TicketTypeDto;
import ru.aqstream.event.api.dto.UpdateTicketTypeRequest;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.api.exception.EventNotEditableException;
import ru.aqstream.event.api.exception.TicketTypeHasRegistrationsException;
import ru.aqstream.event.api.exception.TicketTypeNotFoundException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketTypeService")
class TicketTypeServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeMapper ticketTypeMapper;

    private TicketTypeService service;

    private static final Faker FAKER = new Faker();

    private UUID tenantId;
    private UUID eventId;
    private UUID ticketTypeId;
    private String testName;
    private Event testEvent;
    private TicketType testTicketType;
    private TicketTypeDto testTicketTypeDto;

    @BeforeEach
    void setUp() {
        service = new TicketTypeService(ticketTypeRepository, eventRepository, ticketTypeMapper);

        tenantId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        testName = FAKER.commerce().productName();

        testEvent = createTestEvent();
        testTicketType = createTestTicketType();
        testTicketTypeDto = createTestTicketTypeDto();

        // Устанавливаем tenant context
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Event createTestEvent() {
        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        String slug = "event-" + UUID.randomUUID().toString().substring(0, 8);
        Event event = Event.create(FAKER.book().title(), slug, startsAt, "Europe/Moscow");
        setEntityId(event, eventId);
        return event;
    }

    private TicketType createTestTicketType() {
        TicketType ticketType = TicketType.create(testEvent, testName);
        setEntityId(ticketType, ticketTypeId);
        return ticketType;
    }

    private TicketTypeDto createTestTicketTypeDto() {
        return new TicketTypeDto(
            ticketTypeId,
            eventId,
            testName,
            null,
            0,
            "RUB",
            null,
            0,
            0,
            null,
            null,
            null,
            0,
            true,
            false,
            Instant.now(),
            Instant.now()
        );
    }

    private void setEntityId(Object entity, UUID id) {
        try {
            var current = entity.getClass();
            while (current != null && current != Object.class) {
                try {
                    var idField = current.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(entity, id);
                    return;
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Создаёт тип билета для события")
        void create_ValidRequest_ReturnsTicketType() {
            // given
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                testName, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findMaxSortOrderByEventId(eventId)).thenReturn(null);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            TicketTypeDto result = service.create(eventId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(testName);

            ArgumentCaptor<TicketType> ticketTypeCaptor = ArgumentCaptor.forClass(TicketType.class);
            verify(ticketTypeRepository).save(ticketTypeCaptor.capture());

            TicketType savedTicketType = ticketTypeCaptor.getValue();
            assertThat(savedTicketType.getName()).isEqualTo(testName);
            assertThat(savedTicketType.getPriceCents()).isEqualTo(0); // Phase 2: бесплатные
            assertThat(savedTicketType.isActive()).isTrue();
        }

        @Test
        @DisplayName("Устанавливает sortOrder автоматически")
        void create_NoSortOrder_SetsSortOrderAutomatically() {
            // given
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                testName, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findMaxSortOrderByEventId(eventId)).thenReturn(5);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            service.create(eventId, request);

            // then
            ArgumentCaptor<TicketType> ticketTypeCaptor = ArgumentCaptor.forClass(TicketType.class);
            verify(ticketTypeRepository).save(ticketTypeCaptor.capture());

            TicketType savedTicketType = ticketTypeCaptor.getValue();
            assertThat(savedTicketType.getSortOrder()).isEqualTo(6);
        }

        @Test
        @DisplayName("Выбрасывает EventNotFoundException если событие не найдено")
        void create_EventNotFound_ThrowsException() {
            // given
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                testName, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request))
                .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("Выбрасывает EventNotEditableException если событие отменено")
        void create_CancelledEvent_ThrowsException() {
            // given
            testEvent.cancel();
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                testName, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request))
                .isInstanceOf(EventNotEditableException.class);
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Возвращает тип билета по ID")
        void getById_ExistingTicketType_ReturnsTicketType() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            TicketTypeDto result = service.getById(eventId, ticketTypeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(ticketTypeId);
        }

        @Test
        @DisplayName("Выбрасывает TicketTypeNotFoundException если тип билета не найден")
        void getById_NonExistingTicketType_ThrowsException() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.getById(eventId, ticketTypeId))
                .isInstanceOf(TicketTypeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Обновляет название типа билета")
        void update_ValidName_UpdatesName() {
            // given
            String newName = FAKER.commerce().productName();
            UpdateTicketTypeRequest request = new UpdateTicketTypeRequest(
                newName, null, null, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            service.update(eventId, ticketTypeId, request);

            // then
            verify(ticketTypeRepository).save(any(TicketType.class));
            assertThat(testTicketType.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("Обновляет количество билетов")
        void update_ValidQuantity_UpdatesQuantity() {
            // given
            UpdateTicketTypeRequest request = new UpdateTicketTypeRequest(
                null, null, 100, null, null, null, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            service.update(eventId, ticketTypeId, request);

            // then
            assertThat(testTicketType.getQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("Деактивирует тип билета через isActive=false")
        void update_IsActiveFalse_DeactivatesTicketType() {
            // given
            UpdateTicketTypeRequest request = new UpdateTicketTypeRequest(
                null, null, null, null, null, null, false
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            service.update(eventId, ticketTypeId, request);

            // then
            assertThat(testTicketType.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Удаляет тип билета без регистраций")
        void delete_NoRegistrations_DeletesSuccessfully() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));

            // when
            service.delete(eventId, ticketTypeId);

            // then
            verify(ticketTypeRepository).delete(testTicketType);
        }

        @Test
        @DisplayName("Выбрасывает TicketTypeHasRegistrationsException если есть проданные билеты")
        void delete_HasSoldTickets_ThrowsException() {
            // given
            testTicketType.incrementSoldCount();

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));

            // when/then
            assertThatThrownBy(() -> service.delete(eventId, ticketTypeId))
                .isInstanceOf(TicketTypeHasRegistrationsException.class);

            verify(ticketTypeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Выбрасывает TicketTypeHasRegistrationsException если есть зарезервированные билеты")
        void delete_HasReservedTickets_ThrowsException() {
            // given
            testTicketType.incrementReservedCount();

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));

            // when/then
            assertThatThrownBy(() -> service.delete(eventId, ticketTypeId))
                .isInstanceOf(TicketTypeHasRegistrationsException.class);

            verify(ticketTypeRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("Деактивирует тип билета")
        void deactivate_ActiveTicketType_DeactivatesSuccessfully() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            service.deactivate(eventId, ticketTypeId);

            // then
            assertThat(testTicketType.isActive()).isFalse();
            verify(ticketTypeRepository).save(testTicketType);
        }
    }

}
