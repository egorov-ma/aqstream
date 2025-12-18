package ru.aqstream.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.aqstream.user.api.dto.OrganizationInviteDto;
import ru.aqstream.user.db.entity.OrganizationInvite;

/**
 * Маппер для преобразования OrganizationInvite entity в DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationInviteMapper {

    /**
     * Формат Telegram deeplink.
     */
    String TELEGRAM_DEEPLINK_FORMAT = "https://t.me/AqStreamBot?start=invite_%s";

    /**
     * Преобразует OrganizationInvite entity в DTO.
     *
     * @param invite сущность приглашения
     * @return DTO приглашения
     */
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "invitedById", source = "invitedById")
    @Mapping(target = "invitedByName", expression = "java(formatInvitedByName(invite))")
    @Mapping(target = "telegramDeeplink", expression = "java(formatTelegramDeeplink(invite))")
    @Mapping(target = "usedById", source = "usedById")
    OrganizationInviteDto toDto(OrganizationInvite invite);

    /**
     * Форматирует имя пригласившего.
     *
     * @param invite приглашение
     * @return полное имя пригласившего
     */
    default String formatInvitedByName(OrganizationInvite invite) {
        if (invite.getInvitedBy() == null) {
            return null;
        }
        String firstName = invite.getInvitedBy().getFirstName();
        String lastName = invite.getInvitedBy().getLastName();
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }

    /**
     * Форматирует Telegram deeplink.
     *
     * @param invite приглашение
     * @return Telegram deeplink
     */
    default String formatTelegramDeeplink(OrganizationInvite invite) {
        return String.format(TELEGRAM_DEEPLINK_FORMAT, invite.getInviteCode());
    }
}
