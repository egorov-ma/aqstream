package ru.aqstream.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на отклонение запроса на создание организации.
 *
 * @param comment причина отклонения
 */
public record RejectOrganizationRequestRequest(
    @NotBlank(message = "Причина отклонения обязательна")
    @Size(min = 10, max = 1000, message = "Причина должна содержать от 10 до 1000 символов")
    String comment
) {
}
