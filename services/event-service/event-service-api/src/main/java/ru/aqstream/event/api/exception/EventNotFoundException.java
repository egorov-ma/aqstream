package ru.aqstream.event.api.exception;

import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение для случаев, когда событие не найдено.
 * Преобразуется в HTTP 404 Not Found.
 */
public class EventNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для ненайденного события по ID.
     *
     * @param eventId идентификатор события
     */
    public EventNotFoundException(UUID eventId) {
        super("Event", eventId);
    }

    /**
     * Создаёт исключение для ненайденного события по slug.
     *
     * @param slug URL-slug события
     */
    public EventNotFoundException(String slug) {
        super("Event", slug);
    }

    /**
     * Создаёт исключение для ненайденного события в организации.
     *
     * @param eventId  идентификатор события
     * @param tenantId идентификатор организации
     */
    public EventNotFoundException(UUID eventId, UUID tenantId) {
        super(
            "event_not_found",
            "Событие не найдено",
            Map.of("eventId", eventId.toString(), "tenantId", tenantId.toString())
        );
    }
}
