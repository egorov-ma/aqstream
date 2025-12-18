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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.user.api.exception.EmailAlreadyVerifiedException;
import ru.aqstream.user.api.exception.InvalidVerificationTokenException;
import ru.aqstream.user.api.exception.TooManyVerificationRequestsException;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.entity.VerificationToken;
import ru.aqstream.user.db.entity.VerificationToken.TokenType;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.db.repository.VerificationTokenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationService")
class VerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordService passwordService;

    private VerificationService verificationService;

    private static final Faker FAKER = new Faker();
    private static final String TEST_PASSWORD_HASH = "$2a$12$hashedpassword";

    private String testEmail;
    private String testFirstName;
    private String testLastName;

    @BeforeEach
    void setUp() {
        testEmail = FAKER.internet().emailAddress();
        testFirstName = FAKER.name().firstName();
        testLastName = FAKER.name().lastName();

        verificationService = new VerificationService(
            userRepository,
            verificationTokenRepository,
            refreshTokenRepository,
            passwordService
        );

        ReflectionTestUtils.setField(verificationService, "frontendUrl", "http://localhost:3000");
    }

    @Nested
    @DisplayName("createEmailVerificationToken")
    class CreateEmailVerificationToken {

        @Test
        @DisplayName("создаёт токен верификации для пользователя")
        void createEmailVerificationToken_ValidUser_CreatesToken() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = verificationService.createEmailVerificationToken(user);

            // Assert
            assertThat(token).isNotNull().hasSize(64);

            // Проверяем сохранение токена
            ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
            verify(verificationTokenRepository).save(tokenCaptor.capture());

            VerificationToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getUser()).isEqualTo(user);
            assertThat(savedToken.getType()).isEqualTo(TokenType.EMAIL_VERIFICATION);
            assertThat(savedToken.getToken()).isEqualTo(token);
            assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("инвалидирует старые токены при создании нового")
        void createEmailVerificationToken_ExistingTokens_InvalidatesOld() {
            // Arrange
            User user = createTestUser();

            // Act
            verificationService.createEmailVerificationToken(user);

            // Assert
            verify(verificationTokenRepository).invalidateAllByUserIdAndType(
                eq(user.getId()),
                eq(TokenType.EMAIL_VERIFICATION),
                any(Instant.class)
            );
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("успешно подтверждает email по валидному токену")
        void verifyEmail_ValidToken_VerifiesEmail() {
            // Arrange
            User user = createTestUser();
            VerificationToken token = createTestVerificationToken(user, TokenType.EMAIL_VERIFICATION);

            when(verificationTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

            // Act
            verificationService.verifyEmail(token.getToken());

            // Assert
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(token.isUsed()).isTrue();

            verify(userRepository).save(user);
            verify(verificationTokenRepository).save(token);
        }

        @Test
        @DisplayName("выбрасывает исключение если токен не найден")
        void verifyEmail_TokenNotFound_ThrowsException() {
            // Arrange
            String unknownToken = UUID.randomUUID().toString().replace("-", "") + "12345678901234567890123456789012";

            when(verificationTokenRepository.findByToken(unknownToken))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> verificationService.verifyEmail(unknownToken))
                .isInstanceOf(InvalidVerificationTokenException.class);
        }

        @Test
        @DisplayName("выбрасывает исключение если токен уже использован")
        void verifyEmail_UsedToken_ThrowsException() {
            // Arrange
            User user = createTestUser();
            VerificationToken token = createTestVerificationToken(user, TokenType.EMAIL_VERIFICATION);
            token.markAsUsed();

            when(verificationTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

            // Act & Assert
            assertThatThrownBy(() -> verificationService.verifyEmail(token.getToken()))
                .isInstanceOf(InvalidVerificationTokenException.class);
        }

        @Test
        @DisplayName("выбрасывает исключение если токен истёк")
        void verifyEmail_ExpiredToken_ThrowsException() {
            // Arrange
            User user = createTestUser();
            VerificationToken token = createExpiredVerificationToken(user, TokenType.EMAIL_VERIFICATION);

            when(verificationTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

            // Act & Assert
            assertThatThrownBy(() -> verificationService.verifyEmail(token.getToken()))
                .isInstanceOf(InvalidVerificationTokenException.class);
        }

        @Test
        @DisplayName("выбрасывает исключение если тип токена неверный")
        void verifyEmail_WrongTokenType_ThrowsException() {
            // Arrange
            User user = createTestUser();
            VerificationToken token = createTestVerificationToken(user, TokenType.PASSWORD_RESET);

            when(verificationTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

            // Act & Assert
            assertThatThrownBy(() -> verificationService.verifyEmail(token.getToken()))
                .isInstanceOf(InvalidVerificationTokenException.class);
        }
    }

    @Nested
    @DisplayName("resendVerificationEmail")
    class ResendVerificationEmail {

        @Test
        @DisplayName("не выбрасывает исключение для несуществующего email")
        void resendVerificationEmail_NonExistentEmail_SilentlySucceeds() {
            // Arrange
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

            // Act & Assert - не должно выбрасывать исключение
            verificationService.resendVerificationEmail(testEmail);

            // Токен не должен создаваться
            verify(verificationTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("выбрасывает исключение если email уже подтверждён")
        void resendVerificationEmail_AlreadyVerified_ThrowsException() {
            // Arrange
            User user = createTestUser();
            user.verifyEmail(); // Подтверждаем email

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            // Act & Assert
            assertThatThrownBy(() -> verificationService.resendVerificationEmail(user.getEmail()))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
        }

        @Test
        @DisplayName("выбрасывает исключение при превышении rate limit")
        void resendVerificationEmail_RateLimitExceeded_ThrowsException() {
            // Arrange
            User user = createTestUser();

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(verificationTokenRepository.countByUserIdAndTypeSince(
                eq(user.getId()),
                eq(TokenType.EMAIL_VERIFICATION),
                any(Instant.class)
            )).thenReturn((long) VerificationService.MAX_REQUESTS_PER_HOUR);

            // Act & Assert
            assertThatThrownBy(() -> verificationService.resendVerificationEmail(user.getEmail()))
                .isInstanceOf(TooManyVerificationRequestsException.class);
        }
    }

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordReset {

        @Test
        @DisplayName("создаёт токен сброса пароля для существующего пользователя")
        void requestPasswordReset_ExistingUser_CreatesToken() {
            // Arrange
            User user = createTestUser();

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(verificationTokenRepository.countByUserIdAndTypeSince(
                any(), any(), any()
            )).thenReturn(0L);

            // Act
            verificationService.requestPasswordReset(user.getEmail());

            // Assert
            ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
            verify(verificationTokenRepository).save(tokenCaptor.capture());

            VerificationToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getType()).isEqualTo(TokenType.PASSWORD_RESET);
        }

        @Test
        @DisplayName("не выбрасывает исключение для несуществующего email")
        void requestPasswordReset_NonExistentEmail_SilentlySucceeds() {
            // Arrange
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

            // Act & Assert - не должно выбрасывать исключение
            verificationService.requestPasswordReset(testEmail);

            // Токен не должен создаваться
            verify(verificationTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("выбрасывает исключение при превышении rate limit")
        void requestPasswordReset_RateLimitExceeded_ThrowsException() {
            // Arrange
            User user = createTestUser();

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(verificationTokenRepository.countByUserIdAndTypeSince(
                eq(user.getId()),
                eq(TokenType.PASSWORD_RESET),
                any(Instant.class)
            )).thenReturn((long) VerificationService.MAX_REQUESTS_PER_HOUR);

            // Act & Assert
            assertThatThrownBy(() -> verificationService.requestPasswordReset(user.getEmail()))
                .isInstanceOf(TooManyVerificationRequestsException.class);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("успешно сбрасывает пароль по валидному токену")
        void resetPassword_ValidToken_ResetsPassword() {
            // Arrange
            User user = createTestUser();
            VerificationToken token = createTestVerificationToken(user, TokenType.PASSWORD_RESET);
            String newPassword = FAKER.internet().password(8, 20, true, false, true);

            when(verificationTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));
            when(passwordService.hash(newPassword)).thenReturn("new-password-hash");
            when(refreshTokenRepository.revokeAllByUserId(any(), any())).thenReturn(3);

            // Act
            verificationService.resetPassword(token.getToken(), newPassword);

            // Assert
            assertThat(token.isUsed()).isTrue();
            assertThat(user.getPasswordHash()).isEqualTo("new-password-hash");

            verify(passwordService).validate(newPassword);
            verify(userRepository).save(user);
            verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()), any(Instant.class));
        }

        @Test
        @DisplayName("выбрасывает исключение если токен не найден")
        void resetPassword_TokenNotFound_ThrowsException() {
            // Arrange
            String unknownToken = UUID.randomUUID().toString().replace("-", "") + "12345678901234567890123456789012";
            String newPassword = FAKER.internet().password(8, 20, true, false, true);

            when(verificationTokenRepository.findByToken(unknownToken))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> verificationService.resetPassword(unknownToken, newPassword))
                .isInstanceOf(InvalidVerificationTokenException.class);
        }

        @Test
        @DisplayName("выбрасывает исключение если токен уже использован")
        void resetPassword_UsedToken_ThrowsException() {
            // Arrange
            User user = createTestUser();
            VerificationToken token = createTestVerificationToken(user, TokenType.PASSWORD_RESET);
            token.markAsUsed();
            String newPassword = FAKER.internet().password(8, 20, true, false, true);

            when(verificationTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

            // Act & Assert
            assertThatThrownBy(() -> verificationService.resetPassword(token.getToken(), newPassword))
                .isInstanceOf(InvalidVerificationTokenException.class);
        }

        @Test
        @DisplayName("выбрасывает исключение если тип токена неверный")
        void resetPassword_WrongTokenType_ThrowsException() {
            // Arrange
            User user = createTestUser();
            VerificationToken token = createTestVerificationToken(user, TokenType.EMAIL_VERIFICATION);
            String newPassword = FAKER.internet().password(8, 20, true, false, true);

            when(verificationTokenRepository.findByToken(token.getToken()))
                .thenReturn(Optional.of(token));

            // Act & Assert
            assertThatThrownBy(() -> verificationService.resetPassword(token.getToken(), newPassword))
                .isInstanceOf(InvalidVerificationTokenException.class);
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("удаляет истёкшие и использованные токены")
        void cleanupExpiredTokens_ExpiredAndUsedTokens_DeletesBoth() {
            // Arrange
            when(verificationTokenRepository.deleteExpiredBefore(any())).thenReturn(5);
            when(verificationTokenRepository.deleteUsedBefore(any())).thenReturn(3);

            // Act
            int deleted = verificationService.cleanupExpiredTokens();

            // Assert
            assertThat(deleted).isEqualTo(8);
            verify(verificationTokenRepository).deleteExpiredBefore(any());
            verify(verificationTokenRepository).deleteUsedBefore(any());
        }
    }

    // === Вспомогательные методы ===

    private User createTestUser() {
        User user = User.createWithEmail(testEmail, TEST_PASSWORD_HASH, testFirstName, testLastName);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private VerificationToken createTestVerificationToken(User user, TokenType type) {
        String tokenValue = UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        VerificationToken token;
        if (type == TokenType.EMAIL_VERIFICATION) {
            token = VerificationToken.createEmailVerification(user, tokenValue);
        } else {
            token = VerificationToken.createPasswordReset(user, tokenValue);
        }
        ReflectionTestUtils.setField(token, "id", UUID.randomUUID());
        return token;
    }

    private VerificationToken createExpiredVerificationToken(User user, TokenType type) {
        VerificationToken token = createTestVerificationToken(user, type);
        // Устанавливаем время истечения в прошлом
        ReflectionTestUtils.setField(token, "expiresAt", Instant.now().minusSeconds(3600));
        return token;
    }
}
