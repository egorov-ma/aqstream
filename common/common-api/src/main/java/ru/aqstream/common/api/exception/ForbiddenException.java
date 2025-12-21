package ru.aqstream.common.api.exception;

import java.util.Map;

/**
 * Исключение для ошибок авторизации (доступ запрещён).
 * Преобразуется в HTTP 403 Forbidden.
 *
 * <p>Используется когда:</p>
 * <ul>
 *     <li>Пользователь аутентифицирован, но не имеет прав</li>
 *     <li>Аккаунт заблокирован</li>
 *     <li>Ресурс недоступен для данной роли</li>
 * </ul>
 */
public class ForbiddenException extends AqStreamException {

    private static final String DEFAULT_CODE = "forbidden";

    /**
     * Создаёт исключение с сообщением.
     *
     * @param message описание ошибки (на русском)
     */
    public ForbiddenException(String message) {
        super(DEFAULT_CODE, message);
    }

    /**
     * Создаёт исключение с кодом и сообщением.
     *
     * @param code    специфичный код ошибки
     * @param message описание ошибки
     */
    public ForbiddenException(String code, String message) {
        super(code, message);
    }

    /**
     * Создаёт исключение с деталями.
     *
     * @param code    код ошибки
     * @param message сообщение
     * @param details детали ошибки
     */
    public ForbiddenException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
