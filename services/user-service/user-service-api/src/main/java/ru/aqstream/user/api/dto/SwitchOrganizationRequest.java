package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Запрос на переключение на другую организацию.
 *
 * @param organizationId идентификатор организации для переключения
 */
public record SwitchOrganizationRequest(

    @NotNull(message = "Идентификатор организации обязателен")
    UUID organizationId
) {
}
