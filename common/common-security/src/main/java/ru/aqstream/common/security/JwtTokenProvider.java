package ru.aqstream.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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
 * Провайдер для работы с JWT токенами.
 * Генерирует access и refresh токены, валидирует и извлекает данные пользователя.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiration:15m}") Duration accessTokenExpiration,
        @Value("${jwt.refresh-token-expiration:7d}") Duration refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Генерирует access token для пользователя.
     *
     * @param principal данные пользователя
     * @return JWT access token
     */
    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiration);

        return Jwts.builder()
            .subject(principal.userId().toString())
            .claim(CLAIM_USER_ID, principal.userId().toString())
            .claim(CLAIM_EMAIL, principal.email())
            .claim(CLAIM_TENANT_ID, principal.tenantId().toString())
            .claim(CLAIM_ROLES, principal.roles())
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    /**
     * Генерирует refresh token для пользователя.
     *
     * @param userId идентификатор пользователя
     * @return JWT refresh token
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpiration);

        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_USER_ID, userId.toString())
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    /**
     * Валидирует токен и извлекает данные пользователя.
     *
     * @param token JWT токен
     * @return данные пользователя
     * @throws JwtAuthenticationException если токен невалиден или истёк
     */
    public UserPrincipal validateAndGetPrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
                throw new JwtAuthenticationException("Неверный тип токена");
            }

            UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
            String email = claims.get(CLAIM_EMAIL, String.class);
            UUID tenantId = UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));

            @SuppressWarnings("unchecked")
            List<String> rolesList = claims.get(CLAIM_ROLES, List.class);
            Set<String> roles = rolesList != null ? new HashSet<>(rolesList) : Set.of();

            return new UserPrincipal(userId, email, tenantId, roles);

        } catch (ExpiredJwtException e) {
            log.debug("JWT токен истёк: {}", e.getMessage());
            throw new JwtAuthenticationException("Токен истёк");
        } catch (JwtException e) {
            log.debug("Невалидный JWT токен: {}", e.getMessage());
            throw new JwtAuthenticationException("Невалидный токен");
        } catch (IllegalArgumentException e) {
            log.debug("Ошибка парсинга JWT токена: {}", e.getMessage());
            throw new JwtAuthenticationException("Некорректный формат токена");
        }
    }

    /**
     * Валидирует refresh token и извлекает userId.
     *
     * @param token refresh token
     * @return идентификатор пользователя
     * @throws JwtAuthenticationException если токен невалиден
     */
    public UUID validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
                throw new JwtAuthenticationException("Неверный тип токена");
            }

            return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));

        } catch (ExpiredJwtException e) {
            log.debug("Refresh токен истёк: {}", e.getMessage());
            throw new JwtAuthenticationException("Refresh токен истёк");
        } catch (JwtException e) {
            log.debug("Невалидный refresh токен: {}", e.getMessage());
            throw new JwtAuthenticationException("Невалидный refresh токен");
        }
    }

    /**
     * Извлекает userId из токена без валидации подписи.
     * Используется для логирования и аудита.
     *
     * <p>ВАЖНО: Этот метод НЕ валидирует подпись токена!
     * Используйте только для логирования, не для авторизации.</p>
     *
     * @param token JWT токен
     * @return userId или null если не удалось извлечь
     */
    public UUID extractUserIdUnsafe(String token) {
        try {
            // Используем jjwt для парсинга без валидации подписи
            // Разбиваем токен и декодируем payload часть
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // Декодируем payload (вторая часть JWT)
            byte[] decodedPayload = java.util.Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(decodedPayload, StandardCharsets.UTF_8);

            // Парсим JSON с помощью простого поиска (Jackson недоступен напрямую)
            // Формат: {"userId":"uuid-value",...}
            String searchKey = "\"" + CLAIM_USER_ID + "\":\"";
            int startIndex = payloadJson.indexOf(searchKey);
            if (startIndex < 0) {
                return null;
            }
            startIndex += searchKey.length();
            int endIndex = payloadJson.indexOf("\"", startIndex);
            if (endIndex < 0) {
                return null;
            }

            String userIdStr = payloadJson.substring(startIndex, endIndex);
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            log.trace("Не удалось извлечь userId из токена: {}", e.getMessage());
            return null;
        }
    }
}
