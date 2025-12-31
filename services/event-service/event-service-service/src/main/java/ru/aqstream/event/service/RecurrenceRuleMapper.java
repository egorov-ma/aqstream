package ru.aqstream.event.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.event.api.dto.CreateRecurrenceRuleRequest;
import ru.aqstream.event.api.dto.RecurrenceRuleDto;
import ru.aqstream.event.db.entity.RecurrenceRule;

/**
 * Маппер для преобразования RecurrenceRule entity в DTO и обратно.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecurrenceRuleMapper {

    /**
     * Преобразует RecurrenceRule entity в DTO.
     * Entity имеет поле intervalValue, DTO — interval.
     *
     * @param rule сущность правила повторения
     * @return DTO правила повторения
     */
    @Mapping(target = "interval", source = "intervalValue")
    RecurrenceRuleDto toDto(RecurrenceRule rule);

    /**
     * Создаёт новую сущность RecurrenceRule из запроса.
     * Использует фабричный метод RecurrenceRule.create().
     *
     * @param request запрос на создание правила повторения
     * @return новая сущность (без id и tenantId)
     */
    default RecurrenceRule toEntity(CreateRecurrenceRuleRequest request) {
        if (request == null) {
            return null;
        }

        RecurrenceRule rule = RecurrenceRule.create(
            request.frequency(),
            request.intervalOrDefault()
        );

        rule.setEndsAt(request.endsAt());
        rule.setOccurrenceCount(request.occurrenceCount());
        rule.setByDay(request.byDay());
        rule.setByMonthDay(request.byMonthDay());

        if (request.excludedDates() != null) {
            request.excludedDates().forEach(dateStr -> {
                rule.excludeDate(java.time.LocalDate.parse(dateStr));
            });
        }

        return rule;
    }
}
