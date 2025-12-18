package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при попытке удалить владельца организации.
 */
public class CannotRemoveOwnerException extends ConflictException {

    /**
     * Создаёт исключение для попытки удаления владельца.
     */
    public CannotRemoveOwnerException() {
        super("cannot_remove_owner", "Невозможно удалить владельца организации");
    }
}
