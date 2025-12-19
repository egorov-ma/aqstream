package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на обновление группы.
 *
 * @param name        название группы (обязательно, 2-100 символов)
 * @param description описание группы (опционально, до 2000 символов)
 */
public record UpdateGroupRequest(

    @NotBlank(message = "Название группы обязательно")
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    String name,

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    String description
) {
}
