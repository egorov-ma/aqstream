package ru.aqstream.event.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.db.entity.Event;

/**
 * Маппер для преобразования Event entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    /**
     * Преобразует Event entity в DTO.
     *
     * @param event сущность события
     * @return DTO события
     */
    @Mapping(target = "isPublic", source = "public")
    EventDto toDto(Event event);
}
