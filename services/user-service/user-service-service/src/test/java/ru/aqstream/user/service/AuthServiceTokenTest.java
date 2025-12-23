package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.user.api.exception.InvalidCredentialsException;
import ru.aqstream.user.db.entity.RefreshToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Тесты для операций с токенами AuthService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Token Operations")
class AuthServiceTokenTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private VerificationService verificationService;

    @Mock
    private EventPublisher eventPublisher;

    private AuthService authService;

    private static final Faker FAKER = new Faker();
    private static final String TEST_USER_AGENT = "Mozilla/5.0";
    private static final String TEST_IP = "127.0.0.1";

    private String testEmail;
    private String testFirstName;
    private String testLastName;

    @BeforeEach
    void setUp() {
        testEmail = FAKER.internet().emailAddress();
        testFirstName = FAKER.name().firstName();
        testLastName = FAKER.name().lastName();

        authService = new AuthService(
            userRepository,
            refreshTokenRepository,
            passwordService,
            jwtTokenProvider,
            userMapper,
            verificationService,
            eventPublisher
        );

        ReflectionTestUtils.setField(authService, "accessTokenExpiration",
            java.time.Duration.ofMinutes(15));
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration",
            java.time.Duration.ofDays(7));
    }

    private User createTestUser() {
        User user = User.createWithEmail(testEmail, "$2a$12$hash", testFirstName, testLastName);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private RefreshToken createTestRefreshToken(User user) {
        return RefreshToken.create(
            user,
            "test-token-hash",
            Instant.now().plus(java.time.Duration.ofDays(7)),
            TEST_USER_AGENT,
            TEST_IP
        );
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("отзывает все токены пользователя")
        void logout_ValidUser_RevokesAllTokens() {
            UUID userId = UUID.randomUUID();
            when(refreshTokenRepository.revokeAllByUserId(any(UUID.class), any(Instant.class)))
                .thenReturn(3);

            authService.logout(userId);

            verify(refreshTokenRepository).revokeAllByUserId(any(UUID.class), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("revokeToken")
    class RevokeToken {

        @Test
        @DisplayName("успешно отзывает токен")
        void revokeToken_ValidToken_RevokesToken() {
            String refreshTokenValue = "token.to.revoke";
            User user = createTestUser();
            RefreshToken storedToken = createTestRefreshToken(user);

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(storedToken));

            authService.revokeToken(refreshTokenValue);

            assertThat(storedToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(storedToken);
        }

        @Test
        @DisplayName("игнорирует несуществующий токен")
        void revokeToken_NonExistentToken_DoesNothing() {
            String refreshTokenValue = "non.existent.token";

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            authService.revokeToken(refreshTokenValue);

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("logoutAll")
    class LogoutAll {

        @Test
        @DisplayName("отзывает все токены пользователя по refresh token")
        void logoutAll_ValidToken_RevokesAllUserTokens() {
            String refreshTokenValue = "valid.refresh.token";
            UUID userId = UUID.randomUUID();

            when(jwtTokenProvider.validateRefreshToken(refreshTokenValue)).thenReturn(userId);
            when(refreshTokenRepository.revokeAllByUserId(any(UUID.class), any(Instant.class)))
                .thenReturn(3);

            authService.logoutAll(refreshTokenValue);

            verify(jwtTokenProvider).validateRefreshToken(refreshTokenValue);
            verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(Instant.class));
        }

        @Test
        @DisplayName("выбрасывает исключение если токен невалиден")
        void logoutAll_InvalidToken_ThrowsException() {
            String refreshTokenValue = "invalid.refresh.token";

            when(jwtTokenProvider.validateRefreshToken(refreshTokenValue))
                .thenThrow(new ru.aqstream.common.security.JwtAuthenticationException("Невалидный токен"));

            assertThatThrownBy(() -> authService.logoutAll(refreshTokenValue))
                .isInstanceOf(InvalidCredentialsException.class);
        }
    }
}
