package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.UnauthorizedException;

/**
 * Исключение: неверный текущий пароль при смене пароля.
 * HTTP статус: 401 Unauthorized
 */
public class WrongPasswordException extends UnauthorizedException {

    private static final String CODE = "wrong_password";
    private static final String MESSAGE = "Неверный текущий пароль";

    public WrongPasswordException() {
        super(CODE, MESSAGE);
    }
}
