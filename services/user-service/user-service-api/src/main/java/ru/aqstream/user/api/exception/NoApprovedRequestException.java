package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при отсутствии одобренного запроса на создание организации.
 */
public class NoApprovedRequestException extends ConflictException {

    /**
     * Создаёт исключение для отсутствия одобренного запроса.
     */
    public NoApprovedRequestException() {
        super(
            "no_approved_request",
            "Нет одобренного запроса на создание организации. "
                + "Сначала отправьте запрос и дождитесь одобрения"
        );
    }
}
