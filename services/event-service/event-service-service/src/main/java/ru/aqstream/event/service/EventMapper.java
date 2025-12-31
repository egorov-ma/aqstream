package ru.aqstream.event.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.RecurrenceRuleDto;
import ru.aqstream.event.db.entity.Event;

/**
 * Маппер для преобразования Event entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    /**
     * Преобразует Event entity в DTO.
     * RecurrenceRule маппится отдельно через параметр.
     *
     * @param event          сущность события
     * @param recurrenceRule правило повторения (может быть null)
     * @return DTO события
     */
    @Mapping(target = "isPublic", source = "event.public")
    @Mapping(target = "recurrenceRule", source = "recurrenceRule")
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "endsAt", source = "event.endsAt")
    @Mapping(target = "createdAt", source = "event.createdAt")
    @Mapping(target = "updatedAt", source = "event.updatedAt")
    EventDto toDto(Event event, RecurrenceRuleDto recurrenceRule);

    /**
     * Преобразует Event entity в DTO без правила повторения.
     *
     * @param event сущность события
     * @return DTO события
     */
    default EventDto toDto(Event event) {
        return toDto(event, null);
    }
}
