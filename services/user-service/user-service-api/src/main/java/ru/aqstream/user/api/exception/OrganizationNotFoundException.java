package ru.aqstream.user.api.exception;

import java.util.UUID;
import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение при ненайденной организации.
 */
public class OrganizationNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для ненайденной организации по ID.
     *
     * @param id идентификатор организации
     */
    public OrganizationNotFoundException(UUID id) {
        super("Organization", id);
    }

    /**
     * Создаёт исключение для ненайденной организации по slug.
     *
     * @param slug URL-slug организации
     */
    public OrganizationNotFoundException(String slug) {
        super("Organization", slug);
    }
}
