package ru.aqstream.event.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Конфигурация формы регистрации на событие.
 * Определяет кастомные поля, которые будут запрошены у участника.
 *
 * @param customFields список кастомных полей формы (максимум 20)
 */
public record RegistrationFormConfig(
    @Valid
    @Size(max = 20, message = "Слишком много кастомных полей (максимум 20)")
    List<CustomFieldConfig> customFields
) {

    /**
     * Проверяет, есть ли кастомные поля.
     *
     * @return true если есть хотя бы одно кастомное поле
     */
    public boolean hasCustomFields() {
        return customFields != null && !customFields.isEmpty();
    }

    /**
     * Возвращает кастомные поля или пустой список.
     *
     * @return список кастомных полей
     */
    public List<CustomFieldConfig> customFieldsOrEmpty() {
        return customFields != null ? customFields : List.of();
    }
}
