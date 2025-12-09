package ru.aqstream.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Валидатор JWT токенов для Gateway.
 * Выделен отдельно от common-security, т.к. Gateway работает на WebFlux stack,
 * а common-security зависит от servlet stack.
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";

    private final SecretKey secretKey;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Валидирует токен и возвращает данные пользователя.
     *
     * @param token JWT токен
     * @return данные пользователя
     * @throws JwtValidationException если токен невалиден
     */
    public TokenInfo validate(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
                throw new JwtValidationException("Неверный тип токена");
            }

            UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
            String email = claims.get(CLAIM_EMAIL, String.class);
            UUID tenantId = UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));

            @SuppressWarnings("unchecked")
            List<String> rolesList = claims.get(CLAIM_ROLES, List.class);
            Set<String> roles = rolesList != null ? new HashSet<>(rolesList) : Set.of();

            return new TokenInfo(userId, email, tenantId, roles);

        } catch (ExpiredJwtException e) {
            log.debug("JWT токен истёк: {}", e.getMessage());
            throw new JwtValidationException("Токен истёк");
        } catch (JwtException e) {
            log.debug("Невалидный JWT токен: {}", e.getMessage());
            throw new JwtValidationException("Невалидный токен");
        } catch (IllegalArgumentException e) {
            log.debug("Ошибка парсинга JWT токена: {}", e.getMessage());
            throw new JwtValidationException("Некорректный формат токена");
        }
    }

    /**
     * Данные пользователя из токена.
     */
    public record TokenInfo(
        UUID userId,
        String email,
        UUID tenantId,
        Set<String> roles
    ) { }
}
