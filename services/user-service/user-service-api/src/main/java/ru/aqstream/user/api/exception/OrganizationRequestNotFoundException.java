package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение: запрос на создание организации не найден.
 * HTTP статус: 404 Not Found
 */
public class OrganizationRequestNotFoundException extends EntityNotFoundException {

    private static final String CODE = "organization_request_not_found";

    public OrganizationRequestNotFoundException(UUID requestId) {
        super(CODE, "Запрос на создание организации не найден", Map.of("requestId", requestId.toString()));
    }
}
