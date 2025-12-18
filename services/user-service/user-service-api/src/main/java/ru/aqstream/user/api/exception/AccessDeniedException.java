package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение: доступ запрещён.
 * HTTP статус: 403 Forbidden
 */
public class AccessDeniedException extends AqStreamException {

    private static final String CODE = "access_denied";

    public AccessDeniedException() {
        super(CODE, "Доступ запрещён");
    }

    public AccessDeniedException(String message) {
        super(CODE, message);
    }
}
