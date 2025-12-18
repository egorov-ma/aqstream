package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Запрос на создание организации.
 *
 * @param name        название организации
 * @param slug        URL-slug (lowercase, цифры, дефис)
 * @param description описание организации (опционально)
 */
public record CreateOrganizationRequestRequest(
    @NotBlank(message = "Название обязательно")
    @Size(min = 2, max = 255, message = "Название должно содержать от 2 до 255 символов")
    String name,

    @NotBlank(message = "Slug обязателен")
    @Size(min = 3, max = 50, message = "Slug должен содержать от 3 до 50 символов")
    @Pattern(
        regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$",
        message = "Slug может содержать только строчные латинские буквы, цифры и дефис. "
            + "Должен начинаться и заканчиваться буквой или цифрой"
    )
    String slug,

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    String description
) {
}
