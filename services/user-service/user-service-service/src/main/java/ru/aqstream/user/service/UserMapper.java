package ru.aqstream.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.db.entity.User;

/**
 * Маппер для преобразования User entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * Преобразует User entity в UserDto.
     *
     * @param user сущность пользователя
     * @return DTO пользователя
     */
    UserDto toDto(User user);
}
