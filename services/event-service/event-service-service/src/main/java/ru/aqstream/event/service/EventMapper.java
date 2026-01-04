package ru.aqstream.event.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.RecurrenceRuleDto;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.db.entity.Event;

/**
 * Маппер для преобразования Event entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    /**
     * Преобразует Event entity в DTO с названием организатора и правилом повторения.
     *
     * @param event          сущность события
     * @param organizerName  название организации (из user-service)
     * @param recurrenceRule правило повторения (может быть null)
     * @return DTO события
     */
    @Mapping(target = "isPublic", source = "event.public")
    @Mapping(target = "organizerName", source = "organizerName")
    @Mapping(target = "recurrenceRule", source = "recurrenceRule")
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "endsAt", source = "event.endsAt")
    @Mapping(target = "createdAt", source = "event.createdAt")
    @Mapping(target = "updatedAt", source = "event.updatedAt")
    EventDto toDto(Event event, String organizerName, RecurrenceRuleDto recurrenceRule);

    /**
     * Преобразует Event entity в DTO без названия организатора.
     *
     * @param event          сущность события
     * @param recurrenceRule правило повторения (может быть null)
     * @return DTO события
     */
    default EventDto toDto(Event event, RecurrenceRuleDto recurrenceRule) {
        return toDto(event, null, recurrenceRule);
    }

    /**
     * Преобразует Event entity в DTO без правила повторения и названия организатора.
     *
     * @param event сущность события
     * @return DTO события
     */
    default EventDto toDto(Event event) {
        return toDto(event, null, null);
    }

    /**
     * Преобразует Event entity в DTO с регистрацией пользователя.
     *
     * @param event            сущность события
     * @param organizerName    название организации (из user-service)
     * @param recurrenceRule   правило повторения (может быть null)
     * @param userRegistration регистрация текущего пользователя (может быть null)
     * @return DTO события с информацией о регистрации
     */
    default EventDto toDtoWithRegistration(
        Event event,
        String organizerName,
        RecurrenceRuleDto recurrenceRule,
        RegistrationDto userRegistration
    ) {
        EventDto base = toDto(event, organizerName, recurrenceRule);
        return new EventDto(
            base.id(),
            base.tenantId(),
            base.organizerName(),
            base.title(),
            base.slug(),
            base.description(),
            base.status(),
            base.startsAt(),
            base.endsAt(),
            base.timezone(),
            base.locationType(),
            base.locationAddress(),
            base.onlineUrl(),
            base.maxCapacity(),
            base.registrationOpensAt(),
            base.registrationClosesAt(),
            base.isPublic(),
            base.participantsVisibility(),
            base.groupId(),
            base.registrationFormConfig(),
            base.cancelReason(),
            base.cancelledAt(),
            base.recurrenceRule(),
            base.parentEventId(),
            base.instanceDate(),
            base.createdAt(),
            base.updatedAt(),
            userRegistration
        );
    }
}
