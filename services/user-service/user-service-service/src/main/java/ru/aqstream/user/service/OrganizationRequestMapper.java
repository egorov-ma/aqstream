package ru.aqstream.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.user.api.dto.OrganizationRequestDto;
import ru.aqstream.user.db.entity.OrganizationRequest;

/**
 * Маппер для преобразования OrganizationRequest entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationRequestMapper {

    /**
     * Преобразует OrganizationRequest entity в DTO.
     *
     * @param request сущность запроса
     * @return DTO запроса
     */
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "userName", expression = "java(formatUserName(request))")
    @Mapping(target = "reviewedById", source = "reviewedById")
    OrganizationRequestDto toDto(OrganizationRequest request);

    /**
     * Форматирует имя пользователя.
     *
     * @param request запрос
     * @return полное имя пользователя
     */
    default String formatUserName(OrganizationRequest request) {
        if (request.getUser() == null) {
            return null;
        }
        String firstName = request.getUser().getFirstName();
        String lastName = request.getUser().getLastName();
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
}
