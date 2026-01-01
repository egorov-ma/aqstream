package ru.aqstream.gateway.filter;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.aqstream.gateway.GatewayHeaders;
import ru.aqstream.gateway.security.JwtTokenValidator;
import ru.aqstream.gateway.security.JwtValidationException;

/**
 * Фильтр аутентификации JWT токенов для Gateway.
 * Валидирует токен и добавляет заголовки с данными пользователя для downstream сервисов.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/verify-email",
        "/api/v1/auth/telegram",
        "/api/v1/events/public",
        "/api/v1/public",
        "/api/v1/webhooks",
        "/api/v1/system",
        "/actuator"
    );

    private final JwtTokenValidator tokenValidator;

    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Пропускаем публичные endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            log.debug("Отсутствует токен авторизации для пути: {}", path);
            return unauthorized(exchange);
        }

        try {
            JwtTokenValidator.TokenInfo tokenInfo = tokenValidator.validate(token);

            // Добавляем headers для downstream сервисов
            ServerHttpRequest request = exchange.getRequest().mutate()
                .header(GatewayHeaders.USER_ID, tokenInfo.userId().toString())
                .header(GatewayHeaders.TENANT_ID, tokenInfo.tenantId().toString())
                .header(GatewayHeaders.USER_ROLES, String.join(",", tokenInfo.roles()))
                .build();

            log.trace("Аутентификация успешна: userId={}, tenantId={}",
                tokenInfo.userId(), tokenInfo.tenantId());

            return chain.filter(exchange.mutate().request(request).build());

        } catch (JwtValidationException e) {
            log.debug("Ошибка валидации JWT для пути {}: {}", path, e.getMessage());
            return unauthorized(exchange);
        }
    }

    @Override
    public int getOrder() {
        // Выполняется одним из первых фильтров
        return -100;
    }

    /**
     * Извлекает токен из заголовка Authorization.
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Проверяет, является ли путь публичным.
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Возвращает 401 Unauthorized ответ.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
