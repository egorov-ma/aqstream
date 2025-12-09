package ru.aqstream.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Утилитный класс для работы с контекстом безопасности.
 * Предоставляет удобные методы для получения текущего пользователя.
 */
public final class SecurityContext {

    private SecurityContext() {
        // Утилитный класс
    }

    /**
     * Возвращает текущего аутентифицированного пользователя.
     *
     * @return данные пользователя
     * @throws IllegalStateException если пользователь не аутентифицирован
     */
    public static UserPrincipal getCurrentUser() {
        return getCurrentUserOptional()
            .orElseThrow(() -> new IllegalStateException(
                "Пользователь не аутентифицирован. " +
                "Проверьте, что endpoint защищён Spring Security."
            ));
    }

    /**
     * Возвращает текущего пользователя как Optional.
     *
     * @return Optional с данными пользователя или пустой Optional
     */
    public static Optional<UserPrincipal> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal);
        }

        return Optional.empty();
    }

    /**
     * Проверяет, аутентифицирован ли текущий пользователь.
     *
     * @return true если пользователь аутентифицирован
     */
    public static boolean isAuthenticated() {
        return getCurrentUserOptional().isPresent();
    }

    /**
     * Проверяет, имеет ли текущий пользователь указанную роль.
     *
     * @param role роль для проверки
     * @return true если пользователь имеет роль
     */
    public static boolean hasRole(String role) {
        return getCurrentUserOptional()
            .map(user -> user.hasRole(role))
            .orElse(false);
    }

    /**
     * Проверяет, является ли текущий пользователь администратором.
     *
     * @return true если пользователь — администратор
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
