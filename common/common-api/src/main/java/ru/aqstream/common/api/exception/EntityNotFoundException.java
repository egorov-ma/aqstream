package ru.aqstream.common.api.exception;

import java.util.Map;
import java.util.UUID;

/**
 * Исключение для случаев, когда сущность не найдена.
 * Преобразуется в HTTP 404 Not Found.
 */
public class EntityNotFoundException extends AqStreamException {

    private static final String DEFAULT_CODE = "entity_not_found";

    /**
     * Создаёт исключение для ненайденной сущности.
     *
     * @param entityType тип сущности (например, "Event", "User")
     * @param id         идентификатор сущности
     */
    public EntityNotFoundException(String entityType, UUID id) {
        super(
            formatCode(entityType),
            formatMessage(entityType),
            Map.of(formatIdKey(entityType), id.toString())
        );
    }

    /**
     * Создаёт исключение для ненайденной сущности по строковому идентификатору.
     *
     * @param entityType тип сущности
     * @param identifier идентификатор (например, slug, email)
     */
    public EntityNotFoundException(String entityType, String identifier) {
        super(
            formatCode(entityType),
            formatMessage(entityType),
            Map.of("identifier", identifier)
        );
    }

    /**
     * Создаёт исключение с произвольным кодом и сообщением.
     *
     * @param code    код ошибки
     * @param message сообщение об ошибке
     */
    public EntityNotFoundException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    private static String formatCode(String entityType) {
        return entityType.toLowerCase() + "_not_found";
    }

    private static String formatMessage(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "event" -> "Событие не найдено";
            case "user" -> "Пользователь не найден";
            case "organization" -> "Организация не найдена";
            case "registration" -> "Регистрация не найдена";
            case "payment" -> "Платёж не найден";
            case "ticket" -> "Билет не найден";
            default -> entityType + " не найден";
        };
    }

    private static String formatIdKey(String entityType) {
        return entityType.substring(0, 1).toLowerCase() + entityType.substring(1) + "Id";
    }
}
