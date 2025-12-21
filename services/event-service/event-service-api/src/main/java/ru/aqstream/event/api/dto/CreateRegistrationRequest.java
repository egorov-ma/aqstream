package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Запрос на создание регистрации на событие.
 *
 * @param ticketTypeId идентификатор типа билета (обязательно)
 * @param firstName    имя участника (обязательно)
 * @param lastName     фамилия участника (обязательно)
 * @param email        email участника (обязательно)
 * @param customFields дополнительные поля формы (опционально)
 */
public record CreateRegistrationRequest(
    @NotNull(message = "Тип билета обязателен")
    UUID ticketTypeId,

    @NotBlank(message = "Имя обязательно")
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    String firstName,

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    String lastName,

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не должен превышать 255 символов")
    String email,

    Map<String, Object> customFields
) {

    /**
     * Возвращает дополнительные поля с дефолтным значением.
     *
     * @return дополнительные поля или пустая Map
     */
    public Map<String, Object> customFieldsOrDefault() {
        return customFields != null ? customFields : Map.of();
    }
}
