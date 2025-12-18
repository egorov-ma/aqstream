package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение: неверный или истёкший токен верификации.
 * HTTP статус: 400 Bad Request
 *
 * <p>Используется для токенов подтверждения email и сброса пароля.
 * Сообщение намеренно не раскрывает причину (неверный, истёкший или уже использован)
 * для защиты от атак перебора.</p>
 */
public class InvalidVerificationTokenException extends AqStreamException {

    private static final String CODE = "invalid_verification_token";
    private static final String MESSAGE = "Недействительная или истёкшая ссылка";

    public InvalidVerificationTokenException() {
        super(CODE, MESSAGE);
    }
}
