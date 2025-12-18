package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на обновление организации.
 *
 * @param name        новое название организации (обязательно, 2-255 символов)
 * @param description новое описание организации (опционально, до 2000 символов)
 * @param logoUrl     URL логотипа (опционально, до 500 символов)
 */
public record UpdateOrganizationRequest(

    @NotBlank(message = "Название организации обязательно")
    @Size(min = 2, max = 255, message = "Название должно быть от 2 до 255 символов")
    String name,

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    String description,

    @Size(max = 500, message = "URL логотипа не должен превышать 500 символов")
    String logoUrl
) {
}
