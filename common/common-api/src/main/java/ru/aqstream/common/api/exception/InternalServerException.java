package ru.aqstream.common.api.exception;

import java.util.Map;

/**
 * Исключение для внутренних ошибок сервера (технические сбои).
 * Преобразуется в HTTP 500 Internal Server Error.
 *
 * <p>Используется для ошибок, которые не являются бизнес-ошибками,
 * например: ошибки генерации файлов, проблемы с внешними сервисами и т.д.</p>
 *
 * <p>ВАЖНО: Не использовать для бизнес-ошибок! Для них есть
 * ValidationException, ConflictException и т.д.</p>
 */
public class InternalServerException extends AqStreamException {

    private static final String DEFAULT_CODE = "internal_error";

    /**
     * Создаёт исключение с сообщением.
     *
     * @param message описание ошибки (на русском)
     */
    public InternalServerException(String message) {
        super(DEFAULT_CODE, message);
    }

    /**
     * Создаёт исключение с кодом и сообщением.
     *
     * @param code    специфичный код ошибки
     * @param message описание ошибки
     */
    public InternalServerException(String code, String message) {
        super(code, message);
    }

    /**
     * Создаёт исключение с причиной.
     *
     * @param code    код ошибки
     * @param message сообщение
     * @param cause   причина исключения
     */
    public InternalServerException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Создаёт исключение с деталями.
     *
     * @param code    код ошибки
     * @param message сообщение
     * @param details детали ошибки
     */
    public InternalServerException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
