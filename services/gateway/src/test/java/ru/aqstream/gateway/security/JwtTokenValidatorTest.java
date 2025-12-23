package ru.aqstream.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты для JwtTokenValidator.
 */
class JwtTokenValidatorTest {

    private static final Faker FAKER = new Faker();
    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String EMAIL = FAKER.internet().emailAddress();
    private static final Set<String> ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    private JwtTokenValidator validator;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        validator = new JwtTokenValidator(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("validate_ValidAccessToken_ReturnsTokenInfo")
    void validate_ValidAccessToken_ReturnsTokenInfo() {
        // Arrange
        String token = createValidAccessToken();

        // Act
        JwtTokenValidator.TokenInfo tokenInfo = validator.validate(token);

        // Assert
        assertNotNull(tokenInfo);
        assertEquals(USER_ID, tokenInfo.userId());
        assertEquals(EMAIL, tokenInfo.email());
        assertEquals(TENANT_ID, tokenInfo.tenantId());
        assertTrue(tokenInfo.roles().containsAll(ROLES));
    }

    @Test
    @DisplayName("validate_ExpiredToken_ThrowsJwtValidationException")
    void validate_ExpiredToken_ThrowsJwtValidationException() {
        // Arrange
        String token = createExpiredToken();

        // Act & Assert
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> validator.validate(token)
        );
        assertEquals("Токен истёк", exception.getMessage());
    }

    @Test
    @DisplayName("validate_InvalidSignature_ThrowsJwtValidationException")
    void validate_InvalidSignature_ThrowsJwtValidationException() {
        // Arrange
        SecretKey wrongKey = Keys.hmacShaKeyFor(
            "wrong-secret-key-must-be-at-least-32-characters".getBytes(StandardCharsets.UTF_8)
        );
        String token = createTokenWithKey(wrongKey);

        // Act & Assert
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> validator.validate(token)
        );
        assertEquals("Невалидный токен", exception.getMessage());
    }

    @Test
    @DisplayName("validate_RefreshToken_ThrowsJwtValidationException")
    void validate_RefreshToken_ThrowsJwtValidationException() {
        // Arrange
        String token = createRefreshToken();

        // Act & Assert
        JwtValidationException exception = assertThrows(
            JwtValidationException.class,
            () -> validator.validate(token)
        );
        assertEquals("Неверный тип токена", exception.getMessage());
    }

    @Test
    @DisplayName("validate_MalformedToken_ThrowsJwtValidationException")
    void validate_MalformedToken_ThrowsJwtValidationException() {
        // Arrange
        String token = "not.a.valid.jwt.token";

        // Act & Assert
        assertThrows(JwtValidationException.class, () -> validator.validate(token));
    }

    @Test
    @DisplayName("validate_EmptyToken_ThrowsJwtValidationException")
    void validate_EmptyToken_ThrowsJwtValidationException() {
        // Arrange
        String token = "";

        // Act & Assert
        assertThrows(JwtValidationException.class, () -> validator.validate(token));
    }

    @Test
    @DisplayName("validate_TokenWithoutRoles_ReturnsEmptyRoles")
    void validate_TokenWithoutRoles_ReturnsEmptyRoles() {
        // Arrange
        String token = createTokenWithoutRoles();

        // Act
        JwtTokenValidator.TokenInfo tokenInfo = validator.validate(token);

        // Assert
        assertNotNull(tokenInfo);
        assertTrue(tokenInfo.roles().isEmpty());
    }

    /**
     * Создаёт валидный access token.
     */
    private String createValidAccessToken() {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(USER_ID.toString())
            .claim("userId", USER_ID.toString())
            .claim("email", EMAIL)
            .claim("tenantId", TENANT_ID.toString())
            .claim("roles", List.copyOf(ROLES))
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofHours(1))))
            .signWith(secretKey)
            .compact();
    }

    /**
     * Создаёт истёкший токен.
     */
    private String createExpiredToken() {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        return Jwts.builder()
            .subject(USER_ID.toString())
            .claim("userId", USER_ID.toString())
            .claim("email", EMAIL)
            .claim("tenantId", TENANT_ID.toString())
            .claim("roles", List.copyOf(ROLES))
            .claim("type", "access")
            .issuedAt(Date.from(past))
            .expiration(Date.from(past.plus(Duration.ofHours(1))))
            .signWith(secretKey)
            .compact();
    }

    /**
     * Создаёт токен с другим ключом подписи.
     */
    private String createTokenWithKey(SecretKey key) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(USER_ID.toString())
            .claim("userId", USER_ID.toString())
            .claim("email", EMAIL)
            .claim("tenantId", TENANT_ID.toString())
            .claim("roles", List.copyOf(ROLES))
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofHours(1))))
            .signWith(key)
            .compact();
    }

    /**
     * Создаёт refresh token.
     */
    private String createRefreshToken() {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(USER_ID.toString())
            .claim("userId", USER_ID.toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofDays(7))))
            .signWith(secretKey)
            .compact();
    }

    /**
     * Создаёт токен без ролей.
     */
    private String createTokenWithoutRoles() {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(USER_ID.toString())
            .claim("userId", USER_ID.toString())
            .claim("email", EMAIL)
            .claim("tenantId", TENANT_ID.toString())
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofHours(1))))
            .signWith(secretKey)
            .compact();
    }
}
