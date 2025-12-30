package ru.aqstream.event.api.dto;

import java.util.List;

/**
 * DTO статистики для dashboard организатора.
 *
 * @param activeEventsCount     количество активных (опубликованных) событий
 * @param totalRegistrations    общее количество регистраций за последние 30 дней
 * @param checkedInCount        количество check-in за последние 30 дней
 * @param attendanceRate        средний процент посещаемости (0-100)
 * @param upcomingEvents        список ближайших событий (до 5)
 */
public record DashboardStatsDto(
    int activeEventsCount,
    long totalRegistrations,
    long checkedInCount,
    double attendanceRate,
    List<EventDto> upcomingEvents
) {

    /**
     * Создаёт DTO со статистикой.
     *
     * @param activeEventsCount  количество активных событий
     * @param totalRegistrations количество регистраций
     * @param checkedInCount     количество check-in
     * @param upcomingEvents     ближайшие события
     * @return DTO статистики
     */
    public static DashboardStatsDto of(
            int activeEventsCount,
            long totalRegistrations,
            long checkedInCount,
            List<EventDto> upcomingEvents
    ) {
        // Вычисляем attendance rate
        double rate = totalRegistrations > 0
            ? (double) checkedInCount / totalRegistrations * 100.0
            : 0.0;

        return new DashboardStatsDto(
            activeEventsCount,
            totalRegistrations,
            checkedInCount,
            Math.round(rate * 10.0) / 10.0, // Округляем до 1 знака после запятой
            upcomingEvents
        );
    }
}
