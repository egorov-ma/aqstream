package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.InternalServerException;

/**
 * Исключение при невозможности сгенерировать уникальный инвайт-код.
 * Крайне редкая ситуация — возникает только при исчерпании попыток генерации.
 * HTTP статус: 500 Internal Server Error.
 */
public class InviteCodeGenerationException extends InternalServerException {

    private static final int MAX_ATTEMPTS = 5;
    private static final String CODE = "invite_code_generation_failed";

    /**
     * Создаёт исключение о невозможности генерации кода.
     */
    public InviteCodeGenerationException() {
        super(
            CODE,
            "Не удалось сгенерировать уникальный инвайт-код после " + MAX_ATTEMPTS + " попыток"
        );
    }
}
