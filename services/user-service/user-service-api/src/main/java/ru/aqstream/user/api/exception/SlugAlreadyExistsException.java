package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение: slug уже используется.
 * HTTP статус: 409 Conflict
 */
public class SlugAlreadyExistsException extends ConflictException {

    private static final String CODE = "slug_already_exists";

    public SlugAlreadyExistsException(String slug) {
        super(CODE, "Slug уже используется", Map.of("slug", slug));
    }
}
