package ru.aqstream.common.api.exception;

import java.util.Map;

/**
 * Исключение для rate limiting.
 * Преобразуется в HTTP 429 Too Many Requests.
 *
 * <p>Используется когда:</p>
 * <ul>
 *     <li>Превышен лимит запросов</li>
 *     <li>Слишком частые попытки выполнить операцию</li>
 * </ul>
 */
public class TooManyRequestsException extends AqStreamException {

    private static final String DEFAULT_CODE = "too_many_requests";

    /**
     * Создаёт исключение с сообщением.
     *
     * @param message описание ошибки (на русском)
     */
    public TooManyRequestsException(String message) {
        super(DEFAULT_CODE, message);
    }

    /**
     * Создаёт исключение с кодом и сообщением.
     *
     * @param code    специфичный код ошибки
     * @param message описание ошибки
     */
    public TooManyRequestsException(String code, String message) {
        super(code, message);
    }

    /**
     * Создаёт исключение с деталями.
     *
     * @param code    код ошибки
     * @param message сообщение
     * @param details детали (например, retryAfter)
     */
    public TooManyRequestsException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
