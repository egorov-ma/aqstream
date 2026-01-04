package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import org.springframework.data.domain.Pageable;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.DashboardStatsDto;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.LocationType;
import ru.aqstream.event.api.dto.ParticipantsVisibility;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventMapper eventMapper;

    private DashboardService service;

    private static final Faker FAKER = new Faker();

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service = new DashboardService(eventRepository, registrationRepository, eventMapper);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("возвращает статистику с данными")
        void getStats_WithData_ReturnsStats() {
            // given
            long activeEvents = 5;
            long totalRegistrations = 100;
            long checkedIn = 75;

            Event upcomingEvent = createTestEvent();
            EventDto upcomingEventDto = createTestEventDto();

            when(eventRepository.countByTenantIdAndStatus(tenantId, EventStatus.PUBLISHED))
                .thenReturn(activeEvents);
            when(registrationRepository.countByTenantIdAndCreatedAtAfter(eq(tenantId), any(Instant.class)))
                .thenReturn(totalRegistrations);
            when(registrationRepository.countCheckedInByTenantIdAfter(eq(tenantId), any(Instant.class)))
                .thenReturn(checkedIn);
            when(eventRepository.findUpcomingByTenantId(eq(tenantId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(upcomingEvent));
            when(eventMapper.toDto(upcomingEvent))
                .thenReturn(upcomingEventDto);

            // when
            DashboardStatsDto result = service.getStats();

            // then
            assertThat(result.activeEventsCount()).isEqualTo(5);
            assertThat(result.totalRegistrations()).isEqualTo(100);
            assertThat(result.checkedInCount()).isEqualTo(75);
            assertThat(result.attendanceRate()).isEqualTo(75.0);
            assertThat(result.upcomingEvents()).hasSize(1);
            assertThat(result.upcomingEvents().get(0)).isEqualTo(upcomingEventDto);

            verify(eventRepository).countByTenantIdAndStatus(tenantId, EventStatus.PUBLISHED);
            verify(registrationRepository).countByTenantIdAndCreatedAtAfter(eq(tenantId), any(Instant.class));
            verify(registrationRepository).countCheckedInByTenantIdAfter(eq(tenantId), any(Instant.class));
        }

        @Test
        @DisplayName("возвращает нулевую статистику для новой организации")
        void getStats_EmptyData_ReturnsZeros() {
            // given
            when(eventRepository.countByTenantIdAndStatus(tenantId, EventStatus.PUBLISHED))
                .thenReturn(0L);
            when(registrationRepository.countByTenantIdAndCreatedAtAfter(eq(tenantId), any(Instant.class)))
                .thenReturn(0L);
            when(registrationRepository.countCheckedInByTenantIdAfter(eq(tenantId), any(Instant.class)))
                .thenReturn(0L);
            when(eventRepository.findUpcomingByTenantId(eq(tenantId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

            // when
            DashboardStatsDto result = service.getStats();

            // then
            assertThat(result.activeEventsCount()).isZero();
            assertThat(result.totalRegistrations()).isZero();
            assertThat(result.checkedInCount()).isZero();
            assertThat(result.attendanceRate()).isZero();
            assertThat(result.upcomingEvents()).isEmpty();
        }

        @Test
        @DisplayName("вычисляет attendance rate корректно")
        void getStats_CalculatesAttendanceRate_Correctly() {
            // given
            when(eventRepository.countByTenantIdAndStatus(tenantId, EventStatus.PUBLISHED))
                .thenReturn(1L);
            when(registrationRepository.countByTenantIdAndCreatedAtAfter(eq(tenantId), any(Instant.class)))
                .thenReturn(50L);
            when(registrationRepository.countCheckedInByTenantIdAfter(eq(tenantId), any(Instant.class)))
                .thenReturn(33L);
            when(eventRepository.findUpcomingByTenantId(eq(tenantId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

            // when
            DashboardStatsDto result = service.getStats();

            // then
            // 33/50 = 0.66 = 66%
            assertThat(result.attendanceRate()).isEqualTo(66.0);
        }
    }

    private Event createTestEvent() {
        String title = FAKER.book().title();
        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        Event event = Event.create(title, "test-slug", startsAt, "Europe/Moscow");

        // Устанавливаем id через reflection
        try {
            var idField = event.getClass().getSuperclass().getSuperclass().getSuperclass()
                .getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    private EventDto createTestEventDto() {
        return new EventDto(
            UUID.randomUUID(),
            tenantId,
            "Test Organization", // organizerName
            FAKER.book().title(),
            "test-slug",
            FAKER.lorem().paragraph(),
            EventStatus.PUBLISHED,
            Instant.now().plus(7, ChronoUnit.DAYS),
            Instant.now().plus(8, ChronoUnit.DAYS),
            "Europe/Moscow",
            LocationType.OFFLINE,
            FAKER.address().fullAddress(),
            null, // onlineUrl
            100, // maxCapacity
            null, // registrationOpensAt
            null, // registrationClosesAt
            true, // isPublic
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
}
