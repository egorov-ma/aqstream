package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.BLOCKER;
import static io.qameta.allure.SeverityLevel.CRITICAL;
import static io.qameta.allure.SeverityLevel.NORMAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.test.allure.AllureSteps;
import ru.aqstream.common.test.allure.TestLogger;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.LoginRequest;
import ru.aqstream.user.api.dto.RefreshTokenRequest;
import ru.aqstream.user.api.dto.RegisterRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.exception.AccountLockedException;
import ru.aqstream.user.api.exception.EmailAlreadyExistsException;
import ru.aqstream.user.api.exception.InvalidCredentialsException;
import ru.aqstream.user.db.entity.RefreshToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

@UnitTest
@Feature(AllureFeatures.Features.USER_MANAGEMENT)
@DisplayName("AuthService")
class AuthServiceTest {

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

    // Мок-значения для токенов (возвращаются из мока, не нуждаются в Faker)
    private static final String TEST_PASSWORD_HASH = "$2a$12$hashedpassword";
    private static final String TEST_ACCESS_TOKEN = "access.token.jwt";
    private static final String TEST_REFRESH_TOKEN = "refresh.token.jwt";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";
    private static final String TEST_IP = "127.0.0.1";

    // Генерируемые тестовые данные
    private String testEmail;
    private String testPassword;
    private String testFirstName;
    private String testLastName;

    @BeforeEach
    void setUp() {
        // Генерируем свежие тестовые данные для каждого теста
        testEmail = FAKER.internet().emailAddress();
        testPassword = FAKER.internet().password(8, 20, true, false, true);
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

        // Устанавливаем значения @Value полей через reflection
        ReflectionTestUtils.setField(authService, "accessTokenExpiration",
            java.time.Duration.ofMinutes(15));
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration",
            java.time.Duration.ofDays(7));
    }

    @Nested
    @Story(AllureFeatures.Stories.AUTHENTICATION)
    @DisplayName("register")
    class Register {

        @Test
        @Severity(BLOCKER)
        @DisplayName("успешно регистрирует нового пользователя")
        void register_ValidRequest_ReturnsAuthResponse() {
            // Arrange
            TestLogger.debug("Создание тестовых данных для регистрации: email={}", testEmail);

            RegisterRequest request = AllureSteps.createTestUser(testEmail, () ->
                new RegisterRequest(testEmail, testPassword, testFirstName, testLastName)
            );

            User savedUser = createTestUser();
            UserDto userDto = createTestUserDto(savedUser);

            AllureSteps.setupMock("UserRepository и зависимости", () -> {
                when(userRepository.existsByEmail(testEmail)).thenReturn(false);
                when(passwordService.hash(testPassword)).thenReturn(TEST_PASSWORD_HASH);
                when(userRepository.save(any(User.class))).thenReturn(savedUser);
                when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class)))
                    .thenReturn(TEST_ACCESS_TOKEN);
                when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                    .thenReturn(TEST_REFRESH_TOKEN);
                when(userMapper.toDto(savedUser)).thenReturn(userDto);
            });

            // Act
            AuthResponse response = AllureSteps.callService("AuthService", "register", () ->
                authService.register(request, TEST_USER_AGENT, TEST_IP)
            );

            // Assert
            AllureSteps.verifyResponse(201, () -> {
                assertThat(response).isNotNull();
                assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
                assertThat(response.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
                assertThat(response.tokenType()).isEqualTo("Bearer");
                assertThat(response.user()).isEqualTo(userDto);
            });

            verify(passwordService).validate(testPassword);
            verify(userRepository).save(any(User.class));
            verify(refreshTokenRepository).save(any());

            TestLogger.info("Пользователь зарегистрирован: userId={}, email={}", savedUser.getId(), testEmail);
            TestLogger.attachJson("Register Request", request);
            TestLogger.attachJson("Auth Response", response);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение при дублировании email")
        void register_DuplicateEmail_ThrowsException() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                testEmail, testPassword, testFirstName, null
            );

            when(userRepository.existsByEmail(testEmail)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("сохраняет email в нижнем регистре")
        void register_MixedCaseEmail_NormalizesToLowerCase() {
            // Arrange
            String mixedCaseEmail = testFirstName + "@Example.COM";
            RegisterRequest request = new RegisterRequest(
                mixedCaseEmail, testPassword, testFirstName, null
            );

            User savedUser = createTestUser();
            UserDto userDto = createTestUserDto(savedUser);

            when(userRepository.existsByEmail(mixedCaseEmail)).thenReturn(false);
            when(passwordService.hash(testPassword)).thenReturn(TEST_PASSWORD_HASH);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(any())).thenReturn(userDto);

            // Act
            authService.register(request, TEST_USER_AGENT, TEST_IP);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEmail()).isEqualTo(mixedCaseEmail.toLowerCase());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.AUTHENTICATION)
    @DisplayName("login")
    class Login {

        @Test
        @Severity(BLOCKER)
        @DisplayName("успешный вход с корректными данными")
        void login_ValidCredentials_ReturnsAuthResponse() {
            // Arrange
            LoginRequest request = new LoginRequest(testEmail, testPassword);
            User user = createTestUser();
            UserDto userDto = createTestUserDto(user);

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));
            when(passwordService.matches(testPassword, user.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(user)).thenReturn(userDto);

            // Act
            AuthResponse response = authService.login(request, TEST_USER_AGENT, TEST_IP);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            verify(userRepository).save(user); // Обновление lastLoginAt
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("выбрасывает исключение при несуществующем email")
        void login_NonExistentEmail_ThrowsException() {
            // Arrange
            LoginRequest request = new LoginRequest(testEmail, testPassword);
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("выбрасывает исключение при неверном пароле")
        void login_WrongPassword_ThrowsException() {
            // Arrange
            String wrongPassword = FAKER.internet().password();
            LoginRequest request = new LoginRequest(testEmail, wrongPassword);
            User user = createTestUser();

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));
            when(passwordService.matches(anyString(), anyString())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidCredentialsException.class);

            // Проверяем, что счётчик неудачных попыток увеличился
            verify(userRepository).save(user);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("блокирует аккаунт после 5 неудачных попыток")
        void login_FiveFailedAttempts_LocksAccount() {
            // Arrange
            String wrongPassword = FAKER.internet().password();
            LoginRequest request = new LoginRequest(testEmail, wrongPassword);
            User user = createTestUser();

            // Симулируем 4 предыдущих неудачных попытки (MAX_FAILED_LOGIN_ATTEMPTS - 1)
            for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS - 1; i++) {
                user.recordFailedLogin();
            }

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));
            when(passwordService.matches(anyString(), anyString())).thenReturn(false);

            // Act & Assert - 5-я попытка должна заблокировать аккаунт
            assertThatThrownBy(() -> authService.login(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(AccountLockedException.class);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("отклоняет вход для заблокированного аккаунта")
        void login_LockedAccount_ThrowsException() {
            // Arrange
            LoginRequest request = new LoginRequest(testEmail, testPassword);
            User user = createTestUser();

            // Блокируем аккаунт (MAX_FAILED_LOGIN_ATTEMPTS неудачных попыток)
            for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS; i++) {
                user.recordFailedLogin();
            }

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(AccountLockedException.class);

            // Пароль не должен проверяться для заблокированного аккаунта
            verify(passwordService, never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.AUTHENTICATION)
    @DisplayName("refresh")
    class Refresh {

        @Test
        @Severity(CRITICAL)
        @DisplayName("успешно обновляет токены")
        void refresh_ValidToken_ReturnsNewAuthResponse() {
            // Arrange
            String refreshTokenValue = "valid.refresh.token";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

            User user = createTestUser();
            RefreshToken storedToken = createTestRefreshToken(user);
            UserDto userDto = createTestUserDto(user);

            when(jwtTokenProvider.validateRefreshToken(refreshTokenValue)).thenReturn(user.getId());
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(storedToken));
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(user)).thenReturn(userDto);

            // Act
            AuthResponse response = authService.refresh(request, TEST_USER_AGENT, TEST_IP);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(response.tokenType()).isEqualTo("Bearer");

            // Проверяем что старый токен отозван (one-time use)
            assertThat(storedToken.isRevoked()).isTrue();

            // save вызывается дважды: для отзыва старого токена и сохранения нового
            verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение если токен не найден в БД")
        void refresh_TokenNotFound_ThrowsException() {
            // Arrange
            String refreshTokenValue = "unknown.refresh.token";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

            when(jwtTokenProvider.validateRefreshToken(refreshTokenValue)).thenReturn(UUID.randomUUID());
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.refresh(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение если токен отозван")
        void refresh_RevokedToken_ThrowsException() {
            // Arrange
            String refreshTokenValue = "revoked.refresh.token";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

            User user = createTestUser();
            RefreshToken storedToken = createTestRefreshToken(user);
            storedToken.revoke(); // Отзываем токен

            when(jwtTokenProvider.validateRefreshToken(refreshTokenValue)).thenReturn(user.getId());
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(storedToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refresh(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение если токен истёк")
        void refresh_ExpiredToken_ThrowsException() {
            // Arrange
            String refreshTokenValue = "expired.refresh.token";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

            User user = createTestUser();
            RefreshToken storedToken = createExpiredRefreshToken(user);

            when(jwtTokenProvider.validateRefreshToken(refreshTokenValue)).thenReturn(user.getId());
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(storedToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refresh(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("выбрасывает исключение если токен принадлежит другому пользователю")
        void refresh_TokenBelongsToAnotherUser_ThrowsException() {
            // Arrange
            String refreshTokenValue = "valid.refresh.token";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

            User tokenOwner = createTestUser();
            RefreshToken storedToken = createTestRefreshToken(tokenOwner);

            // JWT содержит другой userId
            UUID claimedUserId = UUID.randomUUID();
            when(jwtTokenProvider.validateRefreshToken(refreshTokenValue)).thenReturn(claimedUserId);
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(storedToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refresh(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // === Вспомогательные методы ===

    private User createTestUser() {
        User user = User.createWithEmail(testEmail, TEST_PASSWORD_HASH, testFirstName, testLastName);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private UserDto createTestUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getAvatarUrl(),
            user.isEmailVerified(),
            user.isAdmin(),
            Instant.now()
        );
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

    private RefreshToken createExpiredRefreshToken(User user) {
        return RefreshToken.create(
            user,
            "expired-token-hash",
            Instant.now().minus(java.time.Duration.ofHours(1)), // Токен истёк час назад
            TEST_USER_AGENT,
            TEST_IP
        );
    }
}
