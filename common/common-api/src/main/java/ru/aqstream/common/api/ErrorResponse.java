package ru.aqstream.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Стандартный формат ответа об ошибке для REST API.
 *
 * @param code    уникальный код ошибки (например, "event_not_found", "validation_error")
 * @param message человекочитаемое сообщение об ошибке (на русском)
 * @param details дополнительные детали ошибки (может быть null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code,
    String message,
    Map<String, Object> details
) {

    /**
     * Создаёт ErrorResponse без дополнительных деталей.
     *
     * @param code    код ошибки
     * @param message сообщение об ошибке
     */
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    /**
     * Создаёт ErrorResponse с одной деталью.
     *
     * @param code       код ошибки
     * @param message    сообщение об ошибке
     * @param detailKey  ключ детали
     * @param detailValue значение детали
     */
    public static ErrorResponse of(String code, String message, String detailKey, Object detailValue) {
        return new ErrorResponse(code, message, Map.of(detailKey, detailValue));
    }
}
