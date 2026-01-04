package ru.aqstream.common.test;

import io.qameta.allure.Step;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.UserPrincipal;

/**
 * Утилиты для тестирования с UserPrincipal и JWT.
 *
 * <p>Для полноценных интеграционных тестов используйте методы с JWT:</p>
 * <pre>
 * mockMvc.perform(post("/api/v1/resource")
 *     .with(SecurityTestUtils.jwt(jwtTokenProvider, userId))
 *     .contentType(MediaType.APPLICATION_JSON)
 *     .content(...))
 * </pre>
 *
 * <p>Для тестов с отключёнными фильтрами (addFilters=false) используйте userPrincipal:</p>
 * <pre>
 * mockMvc.perform(post("/api/v1/resource")
 *     .with(SecurityTestUtils.userPrincipal(userId))
 *     .contentType(MediaType.APPLICATION_JSON)
 *     .content(...))
 * </pre>
 */
public final class SecurityTestUtils {

    private static final UUID SYSTEM_TENANT_ID = new UUID(0L, 0L);
    private static final String BEARER_PREFIX = "Bearer ";

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

    /**
     * Создаёт RequestPostProcessor, устанавливающий TenantContext.
     * Используется для тестов сервисов с multi-tenancy.
     *
     * @param tenantId идентификатор организации
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor withTenant(UUID tenantId) {
        return request -> {
            ru.aqstream.common.security.TenantContext.setTenantId(tenantId);
            return request;
        };
    }

    // ========== JWT методы для полноценных интеграционных тестов ==========

    /**
     * Создаёт RequestPostProcessor с JWT токеном для пользователя.
     * Используется для полноценных интеграционных тестов без addFilters=false.
     *
     * @param jwtTokenProvider провайдер JWT токенов
     * @param userId           идентификатор пользователя
     * @return RequestPostProcessor
     */
    @Step("Создать JWT токен для пользователя: userId={userId}")
    public static RequestPostProcessor jwt(JwtTokenProvider jwtTokenProvider, UUID userId) {
        return jwt(jwtTokenProvider, userId, Set.of("USER"));
    }

    /**
     * Создаёт RequestPostProcessor с JWT токеном для пользователя с указанными ролями.
     *
     * @param jwtTokenProvider провайдер JWT токенов
     * @param userId           идентификатор пользователя
     * @param roles            роли пользователя
     * @return RequestPostProcessor
     */
    @Step("Создать JWT токен для пользователя: userId={userId}, roles={roles}")
    public static RequestPostProcessor jwt(JwtTokenProvider jwtTokenProvider, UUID userId, Set<String> roles) {
        return jwt(jwtTokenProvider, userId, null, SYSTEM_TENANT_ID, roles);
    }

    /**
     * Создаёт RequestPostProcessor с JWT токеном для администратора.
     *
     * @param jwtTokenProvider провайдер JWT токенов
     * @param userId           идентификатор администратора
     * @return RequestPostProcessor
     */
    @Step("Создать JWT токен для администратора: userId={userId}")
    public static RequestPostProcessor jwtAdmin(JwtTokenProvider jwtTokenProvider, UUID userId) {
        return jwt(jwtTokenProvider, userId, Set.of("USER", "ADMIN"));
    }

    /**
     * Создаёт RequestPostProcessor с JWT токеном с полной настройкой.
     *
     * @param jwtTokenProvider провайдер JWT токенов
     * @param userId           идентификатор пользователя
     * @param email            email пользователя
     * @param tenantId         идентификатор организации
     * @param roles            роли пользователя
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor jwt(
        JwtTokenProvider jwtTokenProvider,
        UUID userId,
        String email,
        UUID tenantId,
        Set<String> roles
    ) {
        UserPrincipal principal = new UserPrincipal(userId, email, tenantId, roles);
        String token = jwtTokenProvider.generateAccessToken(principal);

        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
            return request;
        };
    }

    /**
     * Создаёт RequestPostProcessor с готовым JWT токеном.
     * Используется когда токен уже получен (например, через API регистрации).
     *
     * @param token JWT access token
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor jwt(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
            return request;
        };
    }
}
