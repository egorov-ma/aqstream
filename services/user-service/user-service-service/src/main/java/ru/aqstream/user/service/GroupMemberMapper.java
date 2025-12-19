package ru.aqstream.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.user.api.dto.GroupMemberDto;
import ru.aqstream.user.db.entity.GroupMember;

/**
 * Маппер для преобразования GroupMember entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GroupMemberMapper {

    /**
     * Преобразует GroupMember entity в DTO.
     *
     * @param member сущность участника группы
     * @return DTO участника группы
     */
    @Mapping(target = "groupId", source = "groupId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "userName", expression = "java(formatUserName(member))")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userAvatarUrl", source = "user.avatarUrl")
    @Mapping(target = "invitedById", source = "invitedById")
    @Mapping(target = "invitedByName", expression = "java(formatInvitedByName(member))")
    GroupMemberDto toDto(GroupMember member);

    /**
     * Форматирует имя пользователя.
     *
     * @param member участник группы
     * @return полное имя пользователя
     */
    default String formatUserName(GroupMember member) {
        if (member.getUser() == null) {
            return null;
        }
        String firstName = member.getUser().getFirstName();
        String lastName = member.getUser().getLastName();
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }

    /**
     * Форматирует имя пригласившего.
     *
     * @param member участник группы
     * @return полное имя пригласившего
     */
    default String formatInvitedByName(GroupMember member) {
        if (member.getInvitedBy() == null) {
            return null;
        }
        String firstName = member.getInvitedBy().getFirstName();
        String lastName = member.getInvitedBy().getLastName();
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
}
