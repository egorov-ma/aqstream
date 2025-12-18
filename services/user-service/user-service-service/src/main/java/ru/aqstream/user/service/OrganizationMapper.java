package ru.aqstream.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.user.api.dto.OrganizationDto;
import ru.aqstream.user.db.entity.Organization;

/**
 * Маппер для преобразования Organization entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    /**
     * Преобразует Organization entity в DTO.
     *
     * @param organization сущность организации
     * @return DTO организации
     */
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "ownerName", expression = "java(formatOwnerName(organization))")
    OrganizationDto toDto(Organization organization);

    /**
     * Форматирует имя владельца.
     *
     * @param organization организация
     * @return полное имя владельца
     */
    default String formatOwnerName(Organization organization) {
        if (organization.getOwner() == null) {
            return null;
        }
        String firstName = organization.getOwner().getFirstName();
        String lastName = organization.getOwner().getLastName();
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
}
