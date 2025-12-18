package ru.aqstream.user.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение: запрос на создание организации уже рассмотрен.
 * HTTP статус: 409 Conflict
 */
public class OrganizationRequestAlreadyReviewedException extends ConflictException {

    private static final String CODE = "organization_request_already_reviewed";

    public OrganizationRequestAlreadyReviewedException(UUID requestId, String status) {
        super(
            CODE,
            "Запрос уже рассмотрен",
            Map.of("requestId", requestId.toString(), "status", status)
        );
    }
}
