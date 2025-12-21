package ru.aqstream.event.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.event.api.dto.TicketTypeDto;
import ru.aqstream.event.db.entity.TicketType;

/**
 * Маппер для преобразования TicketType entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TicketTypeMapper {

    /**
     * Преобразует TicketType entity в DTO.
     *
     * @param ticketType сущность типа билета
     * @return DTO типа билета
     */
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isSoldOut", source = "soldOut")
    TicketTypeDto toDto(TicketType ticketType);
}
