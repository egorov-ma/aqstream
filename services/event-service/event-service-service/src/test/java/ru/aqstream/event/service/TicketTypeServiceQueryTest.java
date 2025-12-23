package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.TicketTypeDto;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

/**
 * Тесты для query методов и бизнес-логики TicketTypeService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketTypeService Query & Business Logic")
class TicketTypeServiceQueryTest {

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
    @DisplayName("findAllByEventId()")
    class FindAllByEventId {

        @Test
        @DisplayName("Возвращает все типы билетов события")
        void findAllByEventId_ExistingEvent_ReturnsAllTicketTypes() {
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            List<TicketTypeDto> result = service.findAllByEventId(eventId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ticketTypeId);
        }

        @Test
        @DisplayName("Возвращает пустой список если нет типов билетов")
        void findAllByEventId_NoTicketTypes_ReturnsEmptyList() {
            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdOrderBySortOrderAsc(eventId))
                .thenReturn(List.of());

            List<TicketTypeDto> result = service.findAllByEventId(eventId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveByEventId()")
    class FindActiveByEventId {

        @Test
        @DisplayName("Возвращает только активные типы билетов с открытыми продажами")
        void findActiveByEventId_ActiveAndSalesOpen_ReturnsFiltered() {
            testTicketType.updateSalesPeriod(
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
            );

            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            List<TicketTypeDto> result = service.findActiveByEventId(eventId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
        }

        @Test
        @DisplayName("Фильтрует типы билетов с закрытыми продажами")
        void findActiveByEventId_SalesClosed_ReturnsEmpty() {
            testTicketType.updateSalesPeriod(
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(5, ChronoUnit.DAYS)
            );

            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));

            List<TicketTypeDto> result = service.findActiveByEventId(eventId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает типы билетов без ограничения периода продаж")
        void findActiveByEventId_NoSalesPeriod_ReturnsTicketType() {
            testTicketType.updateSalesPeriod(null, null);

            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            List<TicketTypeDto> result = service.findActiveByEventId(eventId);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findPublicByEventSlug()")
    class FindPublicByEventSlug {

        @Test
        @DisplayName("Возвращает типы билетов публичного события")
        void findPublicByEventSlug_PublicEvent_ReturnsTicketTypes() {
            String slug = "test-event";
            testTicketType.updateSalesPeriod(null, null);

            when(eventRepository.findPublicBySlug(slug)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));
            when(ticketTypeMapper.toDto(testTicketType)).thenReturn(testTicketTypeDto);

            List<TicketTypeDto> result = service.findPublicByEventSlug(slug);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Выбрасывает EventNotFoundException если событие не публичное")
        void findPublicByEventSlug_NotPublic_ThrowsException() {
            String slug = "private-event";

            when(eventRepository.findPublicBySlug(slug)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findPublicByEventSlug(slug))
                .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("Фильтрует типы билетов с закрытыми продажами")
        void findPublicByEventSlug_SalesClosed_ReturnsEmpty() {
            String slug = "test-event";
            testTicketType.updateSalesPeriod(
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(5, ChronoUnit.DAYS)
            );

            when(eventRepository.findPublicBySlug(slug)).thenReturn(Optional.of(testEvent));
            when(ticketTypeRepository.findByEventIdAndActiveIsTrueOrderBySortOrderAsc(eventId))
                .thenReturn(List.of(testTicketType));

            List<TicketTypeDto> result = service.findPublicByEventSlug(slug);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("TicketType бизнес-логика")
    class TicketTypeBusinessLogic {

        @Test
        @DisplayName("isSoldOut возвращает true когда билеты распроданы")
        void isSoldOut_AllTicketsSold_ReturnsTrue() {
            testTicketType.updateQuantity(2);
            testTicketType.incrementSoldCount();
            testTicketType.incrementSoldCount();

            assertThat(testTicketType.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("isSoldOut возвращает false для unlimited билетов")
        void isSoldOut_UnlimitedQuantity_ReturnsFalse() {
            testTicketType.updateQuantity(null);
            testTicketType.incrementSoldCount();

            assertThat(testTicketType.isSoldOut()).isFalse();
        }

        @Test
        @DisplayName("getAvailable вычисляет правильно")
        void getAvailable_MixedCounts_CalculatesCorrectly() {
            testTicketType.updateQuantity(10);
            testTicketType.incrementSoldCount();
            testTicketType.incrementSoldCount();
            testTicketType.incrementReservedCount();

            assertThat(testTicketType.getAvailable()).isEqualTo(7);
        }

        @Test
        @DisplayName("getAvailable возвращает null для unlimited")
        void getAvailable_UnlimitedQuantity_ReturnsNull() {
            testTicketType.updateQuantity(null);

            assertThat(testTicketType.getAvailable()).isNull();
        }

        @Test
        @DisplayName("isSalesOpen проверяет период продаж")
        void isSalesOpen_WithinPeriod_ReturnsTrue() {
            testTicketType.updateSalesPeriod(
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
            );

            assertThat(testTicketType.isSalesOpen()).isTrue();
        }

        @Test
        @DisplayName("isSalesOpen возвращает false для деактивированного типа")
        void isSalesOpen_InactiveTicketType_ReturnsFalse() {
            testTicketType.deactivate();

            assertThat(testTicketType.isSalesOpen()).isFalse();
        }
    }
}
