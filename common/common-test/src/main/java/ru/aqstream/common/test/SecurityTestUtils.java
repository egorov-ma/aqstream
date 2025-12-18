package ru.aqstream.common.test;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.aqstream.common.security.UserPrincipal;

/**
 * Утилиты для тестирования с UserPrincipal.
 *
 * <p>Использование:</p>
 * <pre>
 * mockMvc.perform(post("/api/v1/resource")
 *     .with(SecurityTestUtils.userPrincipal(userId))
 *     .contentType(MediaType.APPLICATION_JSON)
 *     .content(...))
 * </pre>
 */
public final class SecurityTestUtils {

    private static final UUID SYSTEM_TENANT_ID = new UUID(0L, 0L);

    private SecurityTestUtils() {
        // Утилитный класс
    }

    /**
     * Создаёт RequestPostProcessor для аутентифицированного пользователя.
     *
     * @param userId идентификатор пользователя
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor userPrincipal(UUID userId) {
        return userPrincipal(userId, Set.of("USER"));
    }

    /**
     * Создаёт RequestPostProcessor для пользователя с указанными ролями.
     *
     * @param userId идентификатор пользователя
     * @param roles  роли пользователя
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor userPrincipal(UUID userId, Set<String> roles) {
        return userPrincipal(userId, null, SYSTEM_TENANT_ID, roles);
    }

    /**
     * Создаёт RequestPostProcessor для администратора.
     *
     * @param userId идентификатор администратора
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor adminPrincipal(UUID userId) {
        return userPrincipal(userId, Set.of("USER", "ADMIN"));
    }

    /**
     * Создаёт RequestPostProcessor с полной настройкой UserPrincipal.
     *
     * @param userId   идентификатор пользователя
     * @param email    email пользователя
     * @param tenantId идентификатор организации
     * @param roles    роли пользователя
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor userPrincipal(
        UUID userId,
        String email,
        UUID tenantId,
        Set<String> roles
    ) {
        return request -> {
            UserPrincipal principal = new UserPrincipal(userId, email, tenantId, roles);

            Collection<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            return request;
        };
    }

    /**
     * Очищает SecurityContext.
     * Вызывать после тестов если нужно.
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
