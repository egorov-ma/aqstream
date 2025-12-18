package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение: email уже подтверждён.
 * HTTP статус: 400 Bad Request
 *
 * <p>Выбрасывается при попытке повторной верификации уже подтверждённого email.</p>
 */
public class EmailAlreadyVerifiedException extends AqStreamException {

    private static final String CODE = "email_already_verified";
    private static final String MESSAGE = "Email уже подтверждён";

    public EmailAlreadyVerifiedException() {
        super(CODE, MESSAGE);
    }
}
