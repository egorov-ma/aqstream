package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Конфигурация кастомного поля формы регистрации.
 *
 * @param name     имя поля (латиница, camelCase, для передачи в API)
 * @param label    отображаемое название поля на русском
 * @param type     тип поля: text, email, tel, select
 * @param required обязательное ли поле
 * @param options  варианты для select (null для других типов)
 */
public record CustomFieldConfig(
    @NotBlank(message = "Имя поля обязательно")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,49}$", message = "Имя поля должно начинаться с буквы")
    @Size(max = 50, message = "Имя поля не должно превышать 50 символов")
    String name,

    @NotBlank(message = "Название поля обязательно")
    @Size(max = 200, message = "Название поля не должно превышать 200 символов")
    String label,

    @NotBlank(message = "Тип поля обязателен")
    @Pattern(regexp = "^(text|email|tel|select)$", message = "Недопустимый тип поля")
    String type,

    boolean required,

    @Size(max = 50, message = "Слишком много вариантов для выбора")
    List<@Size(max = 200, message = "Вариант не должен превышать 200 символов") String> options
) {

    /**
     * Проверяет, является ли поле типа select.
     *
     * @return true если тип select
     */
    public boolean isSelect() {
        return "select".equals(type);
    }
}
