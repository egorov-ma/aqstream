package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при использовании истёкшего приглашения.
 */
public class OrganizationInviteExpiredException extends ConflictException {

    /**
     * Создаёт исключение для истёкшего приглашения.
     */
    public OrganizationInviteExpiredException() {
        super("invite_expired", "Приглашение истекло");
    }
}
