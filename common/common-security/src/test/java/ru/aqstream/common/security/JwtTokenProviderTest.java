package ru.aqstream.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit тесты для JwtTokenProvider.
 */
class JwtTokenProviderTest {

    // Тестовый секрет минимум 256 бит (32 байта) для HS256
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-provider-minimum-256-bits";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
            TEST_SECRET,
            Duration.ofMinutes(15),
            Duration.ofDays(7)
        );
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("генерирует валидный JWT токен")
        void generateAccessToken_ValidPrincipal_ReturnsToken() {
            // given
            UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                "test@example.com",
                UUID.randomUUID(),
                Set.of("USER", "ORGANIZER")
            );

            // when
            String token = tokenProvider.generateAccessToken(principal);

            // then
            assertNotNull(token);
            assertTrue(token.split("\\.").length == 3, "JWT должен состоять из 3 частей");
        }

        @Test
        @DisplayName("токен содержит корректные claims")
        void generateAccessToken_ValidPrincipal_ContainsCorrectClaims() {
            // given
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(
                userId,
                "test@example.com",
                tenantId,
                Set.of("ADMIN")
            );

            // when
            String token = tokenProvider.generateAccessToken(principal);
            UserPrincipal extracted = tokenProvider.validateAndGetPrincipal(token);

            // then
            assertEquals(userId, extracted.userId());
            assertEquals("test@example.com", extracted.email());
            assertEquals(tenantId, extracted.tenantId());
            assertTrue(extracted.roles().contains("ADMIN"));
        }
    }

    @Nested
    @DisplayName("validateAndGetPrincipal")
    class ValidateAndGetPrincipal {

        @Test
        @DisplayName("успешно валидирует корректный токен")
        void validateAndGetPrincipal_ValidToken_ReturnsPrincipal() {
            // given
            UserPrincipal original = new UserPrincipal(
                UUID.randomUUID(),
                "user@example.com",
                UUID.randomUUID(),
                Set.of("USER")
            );
            String token = tokenProvider.generateAccessToken(original);

            // when
            UserPrincipal result = tokenProvider.validateAndGetPrincipal(token);

            // then
            assertEquals(original.userId(), result.userId());
            assertEquals(original.email(), result.email());
            assertEquals(original.tenantId(), result.tenantId());
            assertEquals(original.roles(), result.roles());
        }

        @Test
        @DisplayName("выбрасывает исключение для невалидного токена")
        void validateAndGetPrincipal_InvalidToken_ThrowsException() {
            // given
            String invalidToken = "invalid.jwt.token";

            // when/then
            JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> tokenProvider.validateAndGetPrincipal(invalidToken)
            );
            assertEquals("Невалидный токен", exception.getMessage());
        }

        @Test
        @DisplayName("выбрасывает исключение для токена с некорректной подписью")
        void validateAndGetPrincipal_WrongSignature_ThrowsException() {
            // given — токен подписан другим ключом
            JwtTokenProvider otherProvider = new JwtTokenProvider(
                "another-secret-key-for-jwt-different-key-256-bits",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
            );
            UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                "test@example.com",
                UUID.randomUUID(),
                Set.of("USER")
            );
            String token = otherProvider.generateAccessToken(principal);

            // when/then
            assertThrows(
                JwtAuthenticationException.class,
                () -> tokenProvider.validateAndGetPrincipal(token)
            );
        }

        @Test
        @DisplayName("выбрасывает исключение для refresh токена")
        void validateAndGetPrincipal_RefreshToken_ThrowsException() {
            // given
            UUID userId = UUID.randomUUID();
            String refreshToken = tokenProvider.generateRefreshToken(userId);

            // when/then
            JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> tokenProvider.validateAndGetPrincipal(refreshToken)
            );
            assertEquals("Неверный тип токена", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("генерирует валидный refresh токен")
        void generateRefreshToken_ValidUserId_ReturnsToken() {
            // given
            UUID userId = UUID.randomUUID();

            // when
            String token = tokenProvider.generateRefreshToken(userId);

            // then
            assertNotNull(token);
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("refresh токен содержит userId")
        void generateRefreshToken_ValidUserId_ContainsUserId() {
            // given
            UUID userId = UUID.randomUUID();

            // when
            String token = tokenProvider.generateRefreshToken(userId);
            UUID extractedUserId = tokenProvider.validateRefreshToken(token);

            // then
            assertEquals(userId, extractedUserId);
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshToken {

        @Test
        @DisplayName("успешно валидирует корректный refresh токен")
        void validateRefreshToken_ValidToken_ReturnsUserId() {
            // given
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateRefreshToken(userId);

            // when
            UUID result = tokenProvider.validateRefreshToken(token);

            // then
            assertEquals(userId, result);
        }

        @Test
        @DisplayName("выбрасывает исключение для access токена")
        void validateRefreshToken_AccessToken_ThrowsException() {
            // given
            UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                "test@example.com",
                UUID.randomUUID(),
                Set.of("USER")
            );
            String accessToken = tokenProvider.generateAccessToken(principal);

            // when/then
            JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> tokenProvider.validateRefreshToken(accessToken)
            );
            assertEquals("Неверный тип токена", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("extractUserIdUnsafe")
    class ExtractUserIdUnsafe {

        @Test
        @DisplayName("извлекает userId из валидного токена")
        void extractUserIdUnsafe_ValidToken_ReturnsUserId() {
            // given
            UUID userId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(
                userId,
                "test@example.com",
                UUID.randomUUID(),
                Set.of("USER")
            );
            String token = tokenProvider.generateAccessToken(principal);

            // when
            UUID result = tokenProvider.extractUserIdUnsafe(token);

            // then
            assertEquals(userId, result);
        }

        @Test
        @DisplayName("возвращает null для некорректного токена")
        void extractUserIdUnsafe_InvalidToken_ReturnsNull() {
            // given
            String invalidToken = "not-a-jwt";

            // when
            UUID result = tokenProvider.extractUserIdUnsafe(invalidToken);

            // then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Token Expiration")
    class TokenExpiration {

        @Test
        @DisplayName("истекший токен выбрасывает исключение")
        void validateAndGetPrincipal_ExpiredToken_ThrowsException() {
            // given — создаём провайдер с очень коротким временем жизни
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                TEST_SECRET,
                Duration.ofMillis(1), // 1 миллисекунда
                Duration.ofMillis(1)
            );
            UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                "test@example.com",
                UUID.randomUUID(),
                Set.of("USER")
            );
            String token = shortLivedProvider.generateAccessToken(principal);

            // Ждём истечения токена
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when/then
            JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> shortLivedProvider.validateAndGetPrincipal(token)
            );
            assertEquals("Токен истёк", exception.getMessage());
        }
    }
}
