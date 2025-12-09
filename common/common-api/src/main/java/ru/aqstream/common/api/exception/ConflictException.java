package ru.aqstream.common.api.exception;

import java.util.Map;

/**
 * Исключение для конфликтов состояния (например, дублирование, некорректный переход состояния).
 * Преобразуется в HTTP 409 Conflict.
 */
public class ConflictException extends AqStreamException {

    private static final String DEFAULT_CODE = "conflict";

    /**
     * Создаёт исключение с сообщением о конфликте.
     *
     * @param message описание конфликта (на русском)
     */
    public ConflictException(String message) {
        super(DEFAULT_CODE, message);
    }

    /**
     * Создаёт исключение с кодом и сообщением.
     *
     * @param code    специфичный код ошибки
     * @param message описание конфликта
     */
    public ConflictException(String code, String message) {
        super(code, message);
    }

    /**
     * Создаёт исключение с деталями.
     *
     * @param code    код ошибки
     * @param message сообщение
     * @param details детали конфликта
     */
    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    /**
     * Создаёт исключение для дублирования сущности.
     *
     * @param entityType тип сущности
     * @param field      поле, по которому обнаружен дубликат
     * @param value      значение поля
     */
    public static ConflictException duplicate(String entityType, String field, String value) {
        return new ConflictException(
            entityType.toLowerCase() + "_already_exists",
            entityType + " с таким " + field + " уже существует",
            Map.of(field, value)
        );
    }

    /**
     * Создаёт исключение для некорректного перехода состояния.
     *
     * @param entityType    тип сущности
     * @param currentState  текущее состояние
     * @param targetState   целевое состояние
     */
    public static ConflictException invalidStateTransition(
        String entityType,
        String currentState,
        String targetState
    ) {
        return new ConflictException(
            "invalid_state_transition",
            "Невозможно перевести " + entityType.toLowerCase() + " из состояния "
                + currentState + " в " + targetState,
            Map.of("currentState", currentState, "targetState", targetState)
        );
    }
}
