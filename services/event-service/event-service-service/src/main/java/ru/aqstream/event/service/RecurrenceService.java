package ru.aqstream.event.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aqstream.event.api.dto.RecurrenceFrequency;
import ru.aqstream.event.db.entity.RecurrenceRule;

/**
 * Сервис для работы с повторяющимися событиями.
 * Генерирует даты экземпляров на основе правила повторения.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurrenceService {

    private static final int MAX_OCCURRENCES = 365; // Максимум экземпляров за раз

    /**
     * Генерирует даты экземпляров события на основе правила повторения.
     *
     * @param rule     правило повторения
     * @param startDate начальная дата события
     * @param timezone  часовой пояс
     * @param limit    максимальное количество дат для генерации
     * @return список дат экземпляров
     */
    public List<LocalDate> generateOccurrences(RecurrenceRule rule, Instant startDate,
                                                String timezone, int limit) {
        List<LocalDate> occurrences = new ArrayList<>();
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDate currentDate = startDate.atZone(zoneId).toLocalDate();

        int count = 0;
        int maxCount = Math.min(limit, MAX_OCCURRENCES);

        // Учитываем occurrenceCount если задан
        if (rule.getOccurrenceCount() != null) {
            maxCount = Math.min(maxCount, rule.getOccurrenceCount());
        }

        while (count < maxCount) {
            // Проверяем endsAt
            if (rule.getEndsAt() != null) {
                LocalDate endDate = rule.getEndsAt().atZone(zoneId).toLocalDate();
                if (currentDate.isAfter(endDate)) {
                    break;
                }
            }

            // Проверяем исключённые даты
            if (!rule.isDateExcluded(currentDate)) {
                // Для WEEKLY проверяем день недели
                if (rule.getFrequency() == RecurrenceFrequency.WEEKLY) {
                    if (isDayMatching(currentDate, rule.getByDay())) {
                        occurrences.add(currentDate);
                        count++;
                    }
                } else {
                    occurrences.add(currentDate);
                    count++;
                }
            }

            // Вычисляем следующую дату
            currentDate = calculateNextDate(currentDate, rule);
        }

        return occurrences;
    }

    /**
     * Вычисляет следующую дату на основе частоты и интервала.
     */
    private LocalDate calculateNextDate(LocalDate current, RecurrenceRule rule) {
        int interval = rule.getIntervalValue();

        return switch (rule.getFrequency()) {
            case DAILY -> current.plusDays(interval);
            case WEEKLY -> {
                // Для WEEKLY с byDay — шаг 1 день, иначе — интервал недель
                if (rule.getByDay() != null && !rule.getByDay().isBlank()) {
                    yield current.plusDays(1);
                }
                yield current.plusWeeks(interval);
            }
            case MONTHLY -> {
                LocalDate next = current.plusMonths(interval);
                // Если задан конкретный день месяца
                if (rule.getByMonthDay() != null) {
                    int day = Math.min(rule.getByMonthDay(), next.lengthOfMonth());
                    yield next.withDayOfMonth(day);
                }
                yield next;
            }
            case YEARLY -> current.plusYears(interval);
        };
    }

    /**
     * Проверяет, соответствует ли день недели правилу byDay.
     *
     * @param date  дата для проверки
     * @param byDay строка с днями недели (например, "MO,WE,FR")
     * @return true если день недели входит в список
     */
    private boolean isDayMatching(LocalDate date, String byDay) {
        if (byDay == null || byDay.isBlank()) {
            return true;
        }

        String dayOfWeek = switch (date.getDayOfWeek()) {
            case MONDAY -> "MO";
            case TUESDAY -> "TU";
            case WEDNESDAY -> "WE";
            case THURSDAY -> "TH";
            case FRIDAY -> "FR";
            case SATURDAY -> "SA";
            case SUNDAY -> "SU";
        };

        Set<String> days = Set.of(byDay.toUpperCase().split(","));
        return days.contains(dayOfWeek);
    }

    /**
     * Вычисляет дату/время экземпляра события.
     *
     * @param templateStartsAt дата/время начала шаблона
     * @param instanceDate     дата экземпляра
     * @param timezone         часовой пояс
     * @return дата/время начала экземпляра
     */
    public Instant calculateInstanceStartTime(Instant templateStartsAt, LocalDate instanceDate,
                                               String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);

        // Получаем время из шаблона
        var templateTime = templateStartsAt.atZone(zoneId).toLocalTime();

        // Комбинируем с датой экземпляра
        return instanceDate.atTime(templateTime).atZone(zoneId).toInstant();
    }

    /**
     * Вычисляет дату/время окончания экземпляра события.
     *
     * @param templateStartsAt дата/время начала шаблона
     * @param templateEndsAt   дата/время окончания шаблона (может быть null)
     * @param instanceDate     дата экземпляра
     * @param timezone         часовой пояс
     * @return дата/время окончания экземпляра или null
     */
    public Instant calculateInstanceEndTime(Instant templateStartsAt, Instant templateEndsAt,
                                             LocalDate instanceDate, String timezone) {
        if (templateEndsAt == null) {
            return null;
        }

        // Вычисляем продолжительность шаблона
        long durationMinutes = ChronoUnit.MINUTES.between(templateStartsAt, templateEndsAt);

        // Применяем к времени начала экземпляра
        Instant instanceStart = calculateInstanceStartTime(templateStartsAt, instanceDate, timezone);
        return instanceStart.plus(durationMinutes, ChronoUnit.MINUTES);
    }
}
