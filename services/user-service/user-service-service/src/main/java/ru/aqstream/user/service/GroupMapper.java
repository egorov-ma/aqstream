package ru.aqstream.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.user.api.dto.GroupDto;
import ru.aqstream.user.db.entity.Group;

/**
 * Маппер для преобразования Group entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GroupMapper {

    /**
     * Преобразует Group entity в DTO.
     * Примечание: memberCount устанавливается вручную в сервисе.
     *
     * @param group сущность группы
     * @return DTO группы
     */
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "createdById", source = "createdById")
    @Mapping(target = "createdByName", expression = "java(formatCreatedByName(group))")
    @Mapping(target = "memberCount", ignore = true)
    GroupDto toDto(Group group);

    /**
     * Форматирует имя создателя.
     *
     * @param group группа
     * @return полное имя создателя
     */
    default String formatCreatedByName(Group group) {
        if (group.getCreatedBy() == null) {
            return null;
        }
        String firstName = group.getCreatedBy().getFirstName();
        String lastName = group.getCreatedBy().getLastName();
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
}
