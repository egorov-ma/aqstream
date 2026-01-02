package ru.aqstream.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.dto.UserTelegramInfoDto;
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
    @Mapping(source = "admin", target = "isAdmin")
    UserDto toDto(User user);

    /**
     * Преобразует User entity в UserTelegramInfoDto.
     *
     * @param user сущность пользователя
     * @return DTO с Telegram информацией
     */
    @Mapping(source = "id", target = "userId")
    UserTelegramInfoDto toTelegramInfoDto(User user);
}
