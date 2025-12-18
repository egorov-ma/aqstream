package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при попытке создать организацию с занятым slug.
 */
public class OrganizationSlugAlreadyExistsException extends ConflictException {

    /**
     * Создаёт исключение для занятого slug.
     *
     * @param slug URL-slug
     */
    public OrganizationSlugAlreadyExistsException(String slug) {
        super(
            "organization_slug_already_exists",
            "Организация с таким slug уже существует: " + slug
        );
    }
}
