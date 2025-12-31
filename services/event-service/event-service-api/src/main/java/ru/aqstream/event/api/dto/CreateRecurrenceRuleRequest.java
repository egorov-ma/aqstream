package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Запрос на создание правила повторения.
 *
 * @param frequency       частота повторения (обязательно)
 * @param interval        интервал между повторениями (по умолчанию 1)
 * @param endsAt          дата окончания серии (опционально)
 * @param occurrenceCount количество повторений (опционально)
 * @param byDay           дни недели для WEEKLY (например, "MO,WE,FR")
 * @param byMonthDay      день месяца для MONTHLY (1-31)
 * @param excludedDates   исключённые даты (опционально)
 */
public record CreateRecurrenceRuleRequest(
    @NotNull(message = "Частота повторения обязательна")
    RecurrenceFrequency frequency,

    @Min(value = 1, message = "Интервал должен быть не менее 1")
    @Max(value = 99, message = "Интервал не должен превышать 99")
    Integer interval,

    Instant endsAt,

    @Min(value = 1, message = "Количество повторений должно быть не менее 1")
    @Max(value = 365, message = "Количество повторений не должно превышать 365")
    Integer occurrenceCount,

    @Size(max = 50, message = "Дни недели не должны превышать 50 символов")
    String byDay,

    @Min(value = 1, message = "День месяца должен быть не менее 1")
    @Max(value = 31, message = "День месяца не должен превышать 31")
    Integer byMonthDay,

    List<String> excludedDates
) {

    /**
     * Возвращает интервал с дефолтным значением.
     *
     * @return интервал
     */
    public int intervalOrDefault() {
        return interval != null ? interval : 1;
    }
}
