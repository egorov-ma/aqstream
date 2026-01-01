package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.aqstream.event.api.dto.RecurrenceFrequency;
import ru.aqstream.event.db.entity.RecurrenceRule;

@DisplayName("RecurrenceService")
class RecurrenceServiceTest {

    private static final String TIMEZONE = "Europe/Moscow";
    private static final ZoneId ZONE_ID = ZoneId.of(TIMEZONE);

    private RecurrenceService recurrenceService;

    @BeforeEach
    void setUp() {
        recurrenceService = new RecurrenceService();
    }

    @Nested
    @DisplayName("generateOccurrences")
    class GenerateOccurrences {

        @Test
        @DisplayName("генерирует даты для DAILY повторения")
        void generateOccurrences_Daily_ReturnsCorrectDates() {
            // Given
            RecurrenceRule rule = createRule(RecurrenceFrequency.DAILY, 1);
            Instant startDate = LocalDate.of(2025, 1, 1).atStartOfDay(ZONE_ID).toInstant();

            // When
            List<LocalDate> occurrences = recurrenceService.generateOccurrences(rule, startDate, TIMEZONE, 5);

            // Then
            assertThat(occurrences).hasSize(5);
            assertThat(occurrences.get(0)).isEqualTo(LocalDate.of(2025, 1, 1));
            assertThat(occurrences.get(1)).isEqualTo(LocalDate.of(2025, 1, 2));
            assertThat(occurrences.get(4)).isEqualTo(LocalDate.of(2025, 1, 5));
        }

        @Test
        @DisplayName("генерирует даты с интервалом 2 дня")
        void generateOccurrences_DailyInterval2_ReturnsCorrectDates() {
            // Given
            RecurrenceRule rule = createRule(RecurrenceFrequency.DAILY, 2);
            Instant startDate = LocalDate.of(2025, 1, 1).atStartOfDay(ZONE_ID).toInstant();

            // When
            List<LocalDate> occurrences = recurrenceService.generateOccurrences(rule, startDate, TIMEZONE, 3);

            // Then
            assertThat(occurrences).hasSize(3);
            assertThat(occurrences.get(0)).isEqualTo(LocalDate.of(2025, 1, 1));
            assertThat(occurrences.get(1)).isEqualTo(LocalDate.of(2025, 1, 3));
            assertThat(occurrences.get(2)).isEqualTo(LocalDate.of(2025, 1, 5));
        }

        @Test
        @DisplayName("генерирует даты для WEEKLY повторения")
        void generateOccurrences_Weekly_ReturnsCorrectDates() {
            // Given
            RecurrenceRule rule = createRule(RecurrenceFrequency.WEEKLY, 1);
            Instant startDate = LocalDate.of(2025, 1, 6).atStartOfDay(ZONE_ID).toInstant(); // Понедельник

            // When
            List<LocalDate> occurrences = recurrenceService.generateOccurrences(rule, startDate, TIMEZONE, 4);

            // Then
            assertThat(occurrences).hasSize(4);
            assertThat(occurrences.get(0)).isEqualTo(LocalDate.of(2025, 1, 6));
            assertThat(occurrences.get(1)).isEqualTo(LocalDate.of(2025, 1, 13));
            assertThat(occurrences.get(2)).isEqualTo(LocalDate.of(2025, 1, 20));
        }

        @Test
        @DisplayName("генерирует даты для MONTHLY повторения")
        void generateOccurrences_Monthly_ReturnsCorrectDates() {
            // Given
            RecurrenceRule rule = createRule(RecurrenceFrequency.MONTHLY, 1);
            Instant startDate = LocalDate.of(2025, 1, 15).atStartOfDay(ZONE_ID).toInstant();

            // When
            List<LocalDate> occurrences = recurrenceService.generateOccurrences(rule, startDate, TIMEZONE, 3);

            // Then
            assertThat(occurrences).hasSize(3);
            assertThat(occurrences.get(0)).isEqualTo(LocalDate.of(2025, 1, 15));
            assertThat(occurrences.get(1)).isEqualTo(LocalDate.of(2025, 2, 15));
            assertThat(occurrences.get(2)).isEqualTo(LocalDate.of(2025, 3, 15));
        }

        @Test
        @DisplayName("генерирует даты для YEARLY повторения")
        void generateOccurrences_Yearly_ReturnsCorrectDates() {
            // Given
            RecurrenceRule rule = createRule(RecurrenceFrequency.YEARLY, 1);
            Instant startDate = LocalDate.of(2025, 3, 1).atStartOfDay(ZONE_ID).toInstant();

            // When
            List<LocalDate> occurrences = recurrenceService.generateOccurrences(rule, startDate, TIMEZONE, 3);

            // Then
            assertThat(occurrences).hasSize(3);
            assertThat(occurrences.get(0)).isEqualTo(LocalDate.of(2025, 3, 1));
            assertThat(occurrences.get(1)).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(occurrences.get(2)).isEqualTo(LocalDate.of(2027, 3, 1));
        }

        @Test
        @DisplayName("ограничивает количество дат по occurrenceCount")
        void generateOccurrences_WithOccurrenceCount_LimitsResults() {
            // Given
            RecurrenceRule rule = createRule(RecurrenceFrequency.DAILY, 1);
            rule.setOccurrenceCount(3);
            Instant startDate = LocalDate.of(2025, 1, 1).atStartOfDay(ZONE_ID).toInstant();

            // When
            List<LocalDate> occurrences = recurrenceService.generateOccurrences(rule, startDate, TIMEZONE, 100);

            // Then
            assertThat(occurrences).hasSize(3);
        }

        @Test
        @DisplayName("ограничивает даты по endsAt")
        void generateOccurrences_WithEndsAt_StopsAtEndDate() {
            // Given
            RecurrenceRule rule = createRule(RecurrenceFrequency.DAILY, 1);
            rule.setEndsAt(LocalDate.of(2025, 1, 3).atStartOfDay(ZONE_ID).toInstant());
            Instant startDate = LocalDate.of(2025, 1, 1).atStartOfDay(ZONE_ID).toInstant();

            // When
            List<LocalDate> occurrences = recurrenceService.generateOccurrences(rule, startDate, TIMEZONE, 100);

            // Then
            assertThat(occurrences).hasSize(3);
            assertThat(occurrences).containsExactly(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 3)
            );
        }
    }

    @Nested
    @DisplayName("calculateInstanceStartTime")
    class CalculateInstanceStartTime {

        @Test
        @DisplayName("сохраняет время начала шаблона")
        void calculateInstanceStartTime_PreservesTemplateTime() {
            // Given
            Instant templateStart = Instant.parse("2025-01-01T10:30:00Z");
            LocalDate instanceDate = LocalDate.of(2025, 1, 15);

            // When
            Instant result = recurrenceService.calculateInstanceStartTime(
                templateStart, instanceDate, "UTC");

            // Then
            assertThat(result).isEqualTo(Instant.parse("2025-01-15T10:30:00Z"));
        }
    }

    @Nested
    @DisplayName("calculateInstanceEndTime")
    class CalculateInstanceEndTime {

        @Test
        @DisplayName("вычисляет время окончания на основе продолжительности")
        void calculateInstanceEndTime_CalculatesCorrectDuration() {
            // Given
            Instant templateStart = Instant.parse("2025-01-01T10:00:00Z");
            Instant templateEnd = Instant.parse("2025-01-01T12:30:00Z"); // 2.5 часа
            LocalDate instanceDate = LocalDate.of(2025, 1, 15);

            // When
            Instant result = recurrenceService.calculateInstanceEndTime(
                templateStart, templateEnd, instanceDate, "UTC");

            // Then
            assertThat(result).isEqualTo(Instant.parse("2025-01-15T12:30:00Z"));
        }

        @Test
        @DisplayName("возвращает null если templateEndsAt null")
        void calculateInstanceEndTime_NullTemplate_ReturnsNull() {
            // Given
            Instant templateStart = Instant.parse("2025-01-01T10:00:00Z");
            LocalDate instanceDate = LocalDate.of(2025, 1, 15);

            // When
            Instant result = recurrenceService.calculateInstanceEndTime(
                templateStart, null, instanceDate, "UTC");

            // Then
            assertThat(result).isNull();
        }
    }

    /**
     * Создаёт тестовое правило повторения.
     */
    private RecurrenceRule createRule(RecurrenceFrequency frequency, int interval) {
        return RecurrenceRule.create(frequency, interval);
    }
}
