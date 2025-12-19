package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение при недостаточных правах на операцию с группой.
 * Преобразуется в HTTP 403 Forbidden.
 */
public class InsufficientGroupPermissionsException extends AqStreamException {

    /**
     * Создаёт исключение для недостаточных прав с указанием требуемой роли.
     *
     * @param action       действие, которое пользователь пытался выполнить
     * @param requiredRole минимально необходимая роль
     */
    public InsufficientGroupPermissionsException(String action, String requiredRole) {
        super(
            "insufficient_group_permissions",
            "Недостаточно прав для выполнения операции: " + action,
            Map.of("requiredRole", requiredRole)
        );
    }

    /**
     * Создаёт исключение для недостаточных прав.
     *
     * @param action действие, которое пользователь пытался выполнить
     */
    public InsufficientGroupPermissionsException(String action) {
        super(
            "insufficient_group_permissions",
            "Недостаточно прав для выполнения операции: " + action
        );
    }
}
