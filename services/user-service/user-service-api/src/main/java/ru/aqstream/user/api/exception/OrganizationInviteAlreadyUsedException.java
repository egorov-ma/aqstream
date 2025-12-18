package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при использовании уже использованного приглашения.
 */
public class OrganizationInviteAlreadyUsedException extends ConflictException {

    /**
     * Создаёт исключение для уже использованного приглашения.
     */
    public OrganizationInviteAlreadyUsedException() {
        super("invite_already_used", "Приглашение уже использовано");
    }
}
