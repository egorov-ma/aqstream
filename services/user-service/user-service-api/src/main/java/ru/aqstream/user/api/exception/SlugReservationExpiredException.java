package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при истечении срока резервации slug.
 * Slug резервируется на 7 дней после одобрения запроса.
 * HTTP статус: 409 Conflict
 */
public class SlugReservationExpiredException extends ConflictException {

    private static final String CODE = "slug_reservation_expired";

    public SlugReservationExpiredException(String slug) {
        super(
            CODE,
            "Срок резервации slug истёк. Подайте новый запрос на создание организации",
            Map.of("slug", slug)
        );
    }
}
