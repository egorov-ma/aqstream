package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Запрос на создание регистрации на событие.
 *
 * @param ticketTypeId идентификатор типа билета (обязательно)
 * @param firstName    имя участника (опционально, если не передано — берётся из профиля)
 * @param lastName     фамилия участника (опционально, если не передано — берётся из профиля)
 * @param email        email участника (опционально, если не передано — берётся из профиля)
 * @param customFields дополнительные поля формы (опционально)
 */
public record CreateRegistrationRequest(
    @NotNull(message = "Тип билета обязателен")
    UUID ticketTypeId,

    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    String firstName,

    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    String lastName,

    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не должен превышать 255 символов")
    String email,

    Map<String, Object> customFields
) {

    /**
     * Проверяет, переданы ли все личные данные в запросе.
     *
     * @return true если firstName, lastName и email переданы и не пустые
     */
    public boolean hasPersonalInfo() {
        return firstName != null && !firstName.isBlank()
            && lastName != null && !lastName.isBlank()
            && email != null && !email.isBlank();
    }

    /**
     * Возвращает дополнительные поля с дефолтным значением.
     *
     * @return дополнительные поля или пустая Map
     */
    public Map<String, Object> customFieldsOrDefault() {
        return customFields != null ? customFields : Map.of();
    }
}
