package ru.aqstream.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import ru.aqstream.gateway.GatewayHeaders;
import ru.aqstream.gateway.security.JwtTokenValidator;

/**
 * Конфигурация rate limiting для Gateway.
 * Определяет KeyResolver для идентификации клиентов.
 *
 * <p>ВАЖНО: Rate limiting в Spring Cloud Gateway работает ДО кастомных GlobalFilter.
 * RequestRateLimiter имеет порядок 0, а наш JwtAuthenticationFilter = -100.
 * Однако RequestRateLimiter выполняется в filter chain после GlobalFilter'ов,
 * поэтому X-User-Id header уже будет установлен.</p>
 */
@Configuration
public class RateLimitConfig {

    private final JwtTokenValidator tokenValidator;

    public RateLimitConfig(JwtTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    /**
     * KeyResolver для rate limiting.
     * Извлекает userId напрямую из JWT токена, если он есть.
     * Для анонимных пользователей использует IP адрес.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Сначала пробуем получить userId из уже установленного header
            String userId = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.USER_ID);
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }

            // Fallback: пробуем извлечь напрямую из токена
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    JwtTokenValidator.TokenInfo tokenInfo = tokenValidator.validate(token);
                    return Mono.just("user:" + tokenInfo.userId().toString());
                } catch (Exception e) {
                    // Невалидный токен — используем IP
                }
            }

            // Для анонимных пользователей используем IP
            String ip = extractClientIp(exchange);
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Извлекает IP клиента с учётом proxy headers.
     */
    private String extractClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Берём первый IP из списка (оригинальный клиент)
            return xForwardedFor.split(",")[0].trim();
        }

        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }
}
