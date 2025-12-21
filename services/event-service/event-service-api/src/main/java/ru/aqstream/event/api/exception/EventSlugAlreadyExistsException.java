package ru.aqstream.event.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при попытке создать событие с уже существующим slug.
 * Преобразуется в HTTP 409 Conflict.
 */
public class EventSlugAlreadyExistsException extends ConflictException {

    /**
     * Создаёт исключение для дублирующегося slug.
     *
     * @param slug URL-slug события
     */
    public EventSlugAlreadyExistsException(String slug) {
        super(
            "event_slug_already_exists",
            "Событие с таким slug уже существует",
            Map.of("slug", slug)
        );
    }
}
