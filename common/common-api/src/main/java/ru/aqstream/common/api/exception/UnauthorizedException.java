package ru.aqstream.common.api.exception;

import java.util.Map;

/**
 * Исключение для ошибок аутентификации.
 * Преобразуется в HTTP 401 Unauthorized.
 *
 * <p>Используется когда:</p>
 * <ul>
 *     <li>Неверные учётные данные (email/пароль)</li>
 *     <li>Невалидный или истёкший токен</li>
 *     <li>Отсутствует аутентификация</li>
 * </ul>
 */
public class UnauthorizedException extends AqStreamException {

    private static final String DEFAULT_CODE = "unauthorized";

    /**
     * Создаёт исключение с сообщением.
     *
     * @param message описание ошибки (на русском)
     */
    public UnauthorizedException(String message) {
        super(DEFAULT_CODE, message);
    }

    /**
     * Создаёт исключение с кодом и сообщением.
     *
     * @param code    специфичный код ошибки
     * @param message описание ошибки
     */
    public UnauthorizedException(String code, String message) {
        super(code, message);
    }

    /**
     * Создаёт исключение с деталями.
     *
     * @param code    код ошибки
     * @param message сообщение
     * @param details детали ошибки
     */
    public UnauthorizedException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
