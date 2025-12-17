package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение: неверные учётные данные (email или пароль).
 * HTTP статус: 401 Unauthorized
 *
 * <p>Сообщение намеренно не указывает, что именно неверно (email или пароль),
 * чтобы не раскрывать информацию о существовании аккаунтов.</p>
 */
public class InvalidCredentialsException extends AqStreamException {

    private static final String CODE = "invalid_credentials";
    private static final String MESSAGE = "Неверный email или пароль";

    public InvalidCredentialsException() {
        super(CODE, MESSAGE);
    }
}
