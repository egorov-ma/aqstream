package ru.aqstream.common.api.exception;

import java.util.Map;

/**
 * Исключение для ошибок валидации бизнес-правил.
 * Преобразуется в HTTP 400 Bad Request.
 */
public class ValidationException extends AqStreamException {

    private static final String DEFAULT_CODE = "validation_error";

    /**
     * Создаёт исключение с сообщением об ошибке валидации.
     *
     * @param message описание ошибки валидации (на русском)
     */
    public ValidationException(String message) {
        super(DEFAULT_CODE, message);
    }

    /**
     * Создаёт исключение с кастомным кодом и сообщением.
     * Используется в наследниках для переопределения кода ошибки.
     *
     * @param code    уникальный код ошибки
     * @param message описание ошибки валидации (на русском)
     */
    protected ValidationException(String code, String message) {
        super(code, message);
    }

    /**
     * Создаёт исключение с деталями по полям.
     *
     * @param message сообщение об ошибке
     * @param fieldErrors карта с ошибками по полям (поле -> сообщение)
     */
    public ValidationException(String message, Map<String, Object> fieldErrors) {
        super(DEFAULT_CODE, message, fieldErrors);
    }

    /**
     * Создаёт исключение с одной ошибкой поля.
     *
     * @param field   имя поля
     * @param message сообщение об ошибке
     */
    public static ValidationException forField(String field, String message) {
        return new ValidationException("Ошибка валидации", Map.of(field, message));
    }
}
