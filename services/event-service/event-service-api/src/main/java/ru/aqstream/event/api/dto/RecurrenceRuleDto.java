package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO правила повторения события.
 *
 * @param id              идентификатор правила
 * @param frequency       частота повторения (DAILY, WEEKLY, MONTHLY, YEARLY)
 * @param interval        интервал повторения (каждые N дней/недель/месяцев)
 * @param endsAt          дата окончания серии (опционально)
 * @param occurrenceCount количество повторений (опционально, альтернатива endsAt)
 * @param byDay           дни недели для WEEKLY (например, "MO,WE,FR")
 * @param byMonthDay      день месяца для MONTHLY (1-31)
 * @param excludedDates   исключённые даты
 * @param createdAt       дата создания
 * @param updatedAt       дата обновления
 */
public record RecurrenceRuleDto(
    UUID id,
    RecurrenceFrequency frequency,
    int interval,
    Instant endsAt,
    Integer occurrenceCount,
    String byDay,
    Integer byMonthDay,
    List<LocalDate> excludedDates,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Создаёт правило для ежедневного повторения.
     *
     * @param interval интервал в днях
     * @param endsAt   дата окончания
     * @return правило
     */
    public static RecurrenceRuleDto daily(int interval, Instant endsAt) {
        return new RecurrenceRuleDto(
            null, RecurrenceFrequency.DAILY, interval, endsAt, null,
            null, null, null, null, null
        );
    }

    /**
     * Создаёт правило для еженедельного повторения.
     *
     * @param interval интервал в неделях
     * @param byDay    дни недели (например, "MO,WE,FR")
     * @param endsAt   дата окончания
     * @return правило
     */
    public static RecurrenceRuleDto weekly(int interval, String byDay, Instant endsAt) {
        return new RecurrenceRuleDto(
            null, RecurrenceFrequency.WEEKLY, interval, endsAt, null,
            byDay, null, null, null, null
        );
    }

    /**
     * Создаёт правило для ежемесячного повторения.
     *
     * @param interval   интервал в месяцах
     * @param byMonthDay день месяца
     * @param endsAt     дата окончания
     * @return правило
     */
    public static RecurrenceRuleDto monthly(int interval, int byMonthDay, Instant endsAt) {
        return new RecurrenceRuleDto(
            null, RecurrenceFrequency.MONTHLY, interval, endsAt, null,
            null, byMonthDay, null, null, null
        );
    }
}
