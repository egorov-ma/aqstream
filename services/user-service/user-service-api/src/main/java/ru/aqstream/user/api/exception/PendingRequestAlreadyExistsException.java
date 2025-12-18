package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение: у пользователя уже есть активный запрос на создание организации.
 * HTTP статус: 409 Conflict
 */
public class PendingRequestAlreadyExistsException extends ConflictException {

    private static final String CODE = "pending_request_already_exists";

    public PendingRequestAlreadyExistsException() {
        super(CODE, "У вас уже есть активный запрос на создание организации");
    }
}
