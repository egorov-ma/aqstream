package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на создание организации.
 *
 * <p>Slug берётся автоматически из одобренного OrganizationRequest.</p>
 *
 * @param name        название организации (обязательно, 2-255 символов)
 * @param description описание организации (опционально, до 2000 символов)
 */
public record CreateOrganizationRequest(

    @NotBlank(message = "Название организации обязательно")
    @Size(min = 2, max = 255, message = "Название должно быть от 2 до 255 символов")
    String name,

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    String description
) {
}
