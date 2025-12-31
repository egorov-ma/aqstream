package ru.aqstream.event.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.aqstream.common.data.TenantAwareEntity;
import ru.aqstream.event.api.dto.RecurrenceFrequency;

/**
 * Правило повторения для серии событий.
 * Определяет, как и когда создаются новые экземпляры повторяющегося события.
 */
@Entity
@Table(name = "recurrence_rules", schema = "event_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurrenceRule extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    @Column(name = "interval_value", nullable = false)
    private int intervalValue = 1;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    /**
     * Дни недели для WEEKLY (например, "MO,WE,FR").
     * Формат: ISO 8601 двухбуквенные коды через запятую.
     */
    @Column(name = "by_day", length = 50)
    private String byDay;

    /**
     * День месяца для MONTHLY (1-31).
     */
    @Column(name = "by_month_day")
    private Integer byMonthDay;

    /**
     * Исключённые даты (отменённые экземпляры серии).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "excluded_dates", columnDefinition = "jsonb")
    private List<LocalDate> excludedDates;

    // === Фабричные методы ===

    /**
     * Создаёт правило повторения.
     *
     * @param frequency частота повторения
     * @param interval  интервал (каждые N периодов)
     * @return правило
     */
    public static RecurrenceRule create(RecurrenceFrequency frequency, int interval) {
        RecurrenceRule rule = new RecurrenceRule();
        rule.setFrequency(frequency);
        rule.setIntervalValue(interval);
        return rule;
    }

    /**
     * Создаёт ежедневное правило.
     *
     * @param interval интервал в днях
     * @return правило
     */
    public static RecurrenceRule daily(int interval) {
        return create(RecurrenceFrequency.DAILY, interval);
    }

    /**
     * Создаёт еженедельное правило.
     *
     * @param interval интервал в неделях
     * @param byDay    дни недели
     * @return правило
     */
    public static RecurrenceRule weekly(int interval, String byDay) {
        RecurrenceRule rule = create(RecurrenceFrequency.WEEKLY, interval);
        rule.setByDay(byDay);
        return rule;
    }

    /**
     * Создаёт ежемесячное правило.
     *
     * @param interval   интервал в месяцах
     * @param byMonthDay день месяца
     * @return правило
     */
    public static RecurrenceRule monthly(int interval, int byMonthDay) {
        RecurrenceRule rule = create(RecurrenceFrequency.MONTHLY, interval);
        rule.setByMonthDay(byMonthDay);
        return rule;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, истекло ли правило.
     *
     * @return true если правило больше не активно
     */
    public boolean isExpired() {
        if (endsAt != null && Instant.now().isAfter(endsAt)) {
            return true;
        }
        return false;
    }

    /**
     * Добавляет дату в список исключений.
     *
     * @param date дата для исключения
     */
    public void excludeDate(LocalDate date) {
        if (excludedDates == null) {
            excludedDates = new java.util.ArrayList<>();
        }
        if (!excludedDates.contains(date)) {
            excludedDates.add(date);
        }
    }

    /**
     * Проверяет, исключена ли дата.
     *
     * @param date дата для проверки
     * @return true если дата исключена
     */
    public boolean isDateExcluded(LocalDate date) {
        return excludedDates != null && excludedDates.contains(date);
    }
}
