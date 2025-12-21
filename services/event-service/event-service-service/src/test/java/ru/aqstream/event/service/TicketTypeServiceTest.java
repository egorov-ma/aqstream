package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
        Event event = Event.create("Test Event", "test-event", startsAt, "Europe/Moscow");
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

    @Nested
    @DisplayName("findAllByEventId()")
    class FindAllByEventId {

        @Test
        @DisplayName("Возвращает все типы билетов события")
        void findAllByEventId_ExistingEvent_ReturnsAllTicketTypes() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            List<TicketTypeDto> result = service.findAllByEventId(eventId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ticketTypeId);
        }

        @Test
        @DisplayName("Возвращает пустой список если нет типов билетов")
        void findAllByEventId_NoTicketTypes_ReturnsEmptyList() {
            // given
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdOrderBySortOrderAsc(eventId))
                .thenReturn(List.of());

            // when
            List<TicketTypeDto> result = service.findAllByEventId(eventId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveByEventId()")
    class FindActiveByEventId {

        @Test
        @DisplayName("Возвращает только активные типы билетов с открытыми продажами")
        void findActiveByEventId_ActiveAndSalesOpen_ReturnsFiltered() {
            // given: устанавливаем период продаж чтобы isSalesOpen() вернул true
            testTicketType.updateSalesPeriod(
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
            );

            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            List<TicketTypeDto> result = service.findActiveByEventId(eventId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
        }

        @Test
        @DisplayName("Фильтрует типы билетов с закрытыми продажами")
        void findActiveByEventId_SalesClosed_ReturnsEmpty() {
            // given: устанавливаем период продаж в прошлом чтобы isSalesOpen() вернул false
            testTicketType.updateSalesPeriod(
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(5, ChronoUnit.DAYS)  // Продажи закончились
            );

            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));

            // when
            List<TicketTypeDto> result = service.findActiveByEventId(eventId);

            // then: тип билета отфильтрован, так как продажи закрыты
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает типы билетов без ограничения периода продаж")
        void findActiveByEventId_NoSalesPeriod_ReturnsTicketType() {
            // given: нет периода продаж — продажи открыты
            testTicketType.updateSalesPeriod(null, null);

            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            List<TicketTypeDto> result = service.findActiveByEventId(eventId);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findPublicByEventSlug()")
    class FindPublicByEventSlug {

        @Test
        @DisplayName("Возвращает типы билетов публичного события")
        void findPublicByEventSlug_PublicEvent_ReturnsTicketTypes() {
            // given
            String slug = "test-event";
            testTicketType.updateSalesPeriod(null, null);  // Без ограничений

            when(eventRepository.findPublicBySlug(slug)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            // when
            List<TicketTypeDto> result = service.findPublicByEventSlug(slug);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Выбрасывает EventNotFoundException если событие не публичное")
        void findPublicByEventSlug_NotPublic_ThrowsException() {
            // given
            String slug = "private-event";

            when(eventRepository.findPublicBySlug(slug)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.findPublicByEventSlug(slug))
                .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("Фильтрует типы билетов с закрытыми продажами")
        void findPublicByEventSlug_SalesClosed_ReturnsEmpty() {
            // given
            String slug = "test-event";
            testTicketType.updateSalesPeriod(
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(5, ChronoUnit.DAYS)  // Продажи закончились
            );

            when(eventRepository.findPublicBySlug(slug)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));

            // when
            List<TicketTypeDto> result = service.findPublicByEventSlug(slug);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("TicketType бизнес-логика")
    class TicketTypeBusinessLogic {

        @Test
        @DisplayName("isSoldOut возвращает true когда билеты распроданы")
        void isSoldOut_AllTicketsSold_ReturnsTrue() {
            // given
            testTicketType.updateQuantity(2);
            testTicketType.incrementSoldCount();
            testTicketType.incrementSoldCount();

            // when/then
            assertThat(testTicketType.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("isSoldOut возвращает false для unlimited билетов")
        void isSoldOut_UnlimitedQuantity_ReturnsFalse() {
            // given
            testTicketType.updateQuantity(null);
            testTicketType.incrementSoldCount();

            // when/then
            assertThat(testTicketType.isSoldOut()).isFalse();
        }

        @Test
        @DisplayName("getAvailable вычисляет правильно")
        void getAvailable_MixedCounts_CalculatesCorrectly() {
            // given
            testTicketType.updateQuantity(10);
            testTicketType.incrementSoldCount();
            testTicketType.incrementSoldCount();
            testTicketType.incrementReservedCount();

            // when/then
            assertThat(testTicketType.getAvailable()).isEqualTo(7); // 10 - 2 - 1
        }

        @Test
        @DisplayName("getAvailable возвращает null для unlimited")
        void getAvailable_UnlimitedQuantity_ReturnsNull() {
            // given
            testTicketType.updateQuantity(null);

            // when/then
            assertThat(testTicketType.getAvailable()).isNull();
        }

        @Test
        @DisplayName("isSalesOpen проверяет период продаж")
        void isSalesOpen_WithinPeriod_ReturnsTrue() {
            // given
            testTicketType.updateSalesPeriod(
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
            );

            // when/then
            assertThat(testTicketType.isSalesOpen()).isTrue();
        }

        @Test
        @DisplayName("isSalesOpen возвращает false для деактивированного типа")
        void isSalesOpen_InactiveTicketType_ReturnsFalse() {
            // given
            testTicketType.deactivate();

            // when/then
            assertThat(testTicketType.isSalesOpen()).isFalse();
        }
    }
}
