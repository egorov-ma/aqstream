package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение: email уже зарегистрирован в системе.
 * HTTP статус: 409 Conflict
 */
public class EmailAlreadyExistsException extends ConflictException {

    private static final String CODE = "email_already_exists";

    public EmailAlreadyExistsException() {
        super(CODE, "Пользователь с таким email уже существует");
    }

    public EmailAlreadyExistsException(String email) {
        super(CODE, "Пользователь с email " + maskEmail(email) + " уже существует");
    }

    /**
     * Маскирует email для безопасного отображения в сообщении.
     * Пример: test@example.com -> t***@example.com
     */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
