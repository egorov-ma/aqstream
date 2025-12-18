package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение при недостаточных правах на операцию в организации.
 * Преобразуется в HTTP 403 Forbidden.
 */
public class InsufficientOrganizationPermissionsException extends AqStreamException {

    /**
     * Создаёт исключение для недостаточных прав.
     *
     * @param action       действие, которое пользователь пытался выполнить
     * @param requiredRole минимально необходимая роль
     */
    public InsufficientOrganizationPermissionsException(String action, String requiredRole) {
        super(
            "insufficient_permissions",
            "Недостаточно прав для выполнения операции: " + action,
            Map.of("requiredRole", requiredRole)
        );
    }

    /**
     * Создаёт исключение для недостаточных прав.
     *
     * @param action действие, которое пользователь пытался выполнить
     */
    public InsufficientOrganizationPermissionsException(String action) {
        super(
            "insufficient_permissions",
            "Недостаточно прав для выполнения операции: " + action
        );
    }
}
