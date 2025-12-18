package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.EntityNotFoundException;

/**
 * Исключение при ненайденном или недействительном приглашении.
 */
public class OrganizationInviteNotFoundException extends EntityNotFoundException {

    /**
     * Создаёт исключение для ненайденного или недействительного приглашения.
     *
     * @param inviteCode код приглашения
     */
    public OrganizationInviteNotFoundException(String inviteCode) {
        super("OrganizationInvite", inviteCode);
    }
}
