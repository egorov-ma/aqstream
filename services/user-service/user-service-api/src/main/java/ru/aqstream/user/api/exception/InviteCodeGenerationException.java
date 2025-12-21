package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение при невозможности сгенерировать уникальный инвайт-код.
 * Крайне редкая ситуация — возникает только при исчерпании попыток генерации.
 * Преобразуется в HTTP 500 Internal Server Error.
 */
public class InviteCodeGenerationException extends AqStreamException {

    private static final int MAX_ATTEMPTS = 5;

    /**
     * Создаёт исключение о невозможности генерации кода.
     */
    public InviteCodeGenerationException() {
        super(
            "invite_code_generation_failed",
            "Не удалось сгенерировать уникальный инвайт-код после " + MAX_ATTEMPTS + " попыток"
        );
    }
}
