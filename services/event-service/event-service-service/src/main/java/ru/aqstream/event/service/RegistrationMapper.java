package ru.aqstream.event.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.db.entity.Registration;

/**
 * Маппер для преобразования Registration entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RegistrationMapper {

    /**
     * Преобразует Registration entity в DTO.
     *
     * @param registration сущность регистрации
     * @return DTO регистрации
     */
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "eventSlug", source = "event.slug")
    @Mapping(target = "eventStartsAt", source = "event.startsAt")
    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    RegistrationDto toDto(Registration registration);
}
