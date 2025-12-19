package ru.aqstream.user.api.exception;

import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение при попытке удалить создателя группы из участников.
 * Преобразуется в HTTP 409 Conflict.
 */
public class CannotRemoveGroupCreatorException extends ConflictException {

    /**
     * Создаёт исключение для невозможности удаления создателя.
     */
    public CannotRemoveGroupCreatorException() {
        super(
            "cannot_remove_group_creator",
            "Нельзя удалить создателя группы из участников"
        );
    }
}
