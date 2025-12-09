package ru.aqstream.common.security;

import java.util.Set;
import java.util.UUID;

/**
 * Данные аутентифицированного пользователя из JWT токена.
 *
 * @param userId   идентификатор пользователя
 * @param email    email пользователя
 * @param tenantId идентификатор организации
 * @param roles    роли пользователя (например, "ADMIN", "ORGANIZER", "USER")
 */
public record UserPrincipal(
    UUID userId,
    String email,
    UUID tenantId,
    Set<String> roles
) {

    /**
     * Проверяет, имеет ли пользователь указанную роль.
     *
     * @param role роль для проверки
     * @return true если пользователь имеет роль
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Проверяет, имеет ли пользователь хотя бы одну из указанных ролей.
     *
     * @param requiredRoles роли для проверки
     * @return true если пользователь имеет хотя бы одну роль
     */
    public boolean hasAnyRole(String... requiredRoles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String role : requiredRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, является ли пользователь администратором.
     *
     * @return true если пользователь — администратор
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Проверяет, является ли пользователь организатором.
     *
     * @return true если пользователь — организатор
     */
    public boolean isOrganizer() {
        return hasAnyRole("ADMIN", "ORGANIZER");
    }
}
