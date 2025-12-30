package ru.aqstream.event.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.DashboardStatsDto;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;

/**
 * Сервис статистики для dashboard организатора.
 * Агрегирует данные по событиям и регистрациям.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private static final int STATS_PERIOD_DAYS = 30;
    private static final int UPCOMING_EVENTS_LIMIT = 5;

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EventMapper eventMapper;

    /**
     * Возвращает статистику для dashboard организатора.
     *
     * @return статистика dashboard
     */
    @Transactional(readOnly = true)
    public DashboardStatsDto getStats() {
        UUID tenantId = TenantContext.getTenantId();
        Instant now = Instant.now();
        Instant periodStart = now.minus(Duration.ofDays(STATS_PERIOD_DAYS));

        log.debug("Получение статистики dashboard: tenantId={}", tenantId);

        // Количество активных (опубликованных) событий
        long activeEventsCount = eventRepository.countByTenantIdAndStatus(tenantId, EventStatus.PUBLISHED);

        // Количество регистраций за период
        long totalRegistrations = registrationRepository.countByTenantIdAndCreatedAtAfter(tenantId, periodStart);

        // Количество check-in за период
        long checkedInCount = registrationRepository.countCheckedInByTenantIdAfter(tenantId, periodStart);

        // Ближайшие события
        List<Event> upcomingEvents = eventRepository.findUpcomingByTenantId(
            tenantId,
            now,
            PageRequest.of(0, UPCOMING_EVENTS_LIMIT)
        );
        List<EventDto> upcomingEventDtos = upcomingEvents.stream()
            .map(eventMapper::toDto)
            .toList();

        log.info("Статистика dashboard: tenantId={}, activeEvents={}, registrations={}, checkIns={}",
            tenantId, activeEventsCount, totalRegistrations, checkedInCount);

        return DashboardStatsDto.of(
            (int) activeEventsCount,
            totalRegistrations,
            checkedInCount,
            upcomingEventDtos
        );
    }
}
