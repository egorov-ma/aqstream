package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
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
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.ConfirmTelegramAuthRequest;
import ru.aqstream.user.api.dto.TelegramAuthInitResponse;
import ru.aqstream.user.api.dto.TelegramAuthStatusResponse;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.exception.TelegramAuthTokenExpiredException;
import ru.aqstream.user.api.exception.TelegramAuthTokenNotFoundException;
import ru.aqstream.user.db.entity.AuthTokenStatus;
import ru.aqstream.user.db.entity.TelegramAuthToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.TelegramAuthTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.websocket.TelegramAuthWebSocketHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramBotAuthService")
class TelegramBotAuthServiceTest {

    @Mock
    private TelegramAuthTokenRepository authTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private TelegramAuthWebSocketHandler webSocketHandler;

    private TelegramBotAuthService service;

    private static final Faker FAKER = new Faker();
    private static final String TEST_BOT_USERNAME = "AqStreamBot";
    private static final String TEST_ACCESS_TOKEN = "access.token.jwt";
    private static final String TEST_REFRESH_TOKEN = "refresh.token.jwt";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";
    private static final String TEST_IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        service = new TelegramBotAuthService(
            authTokenRepository,
            userRepository,
            refreshTokenRepository,
            jwtTokenProvider,
            userMapper,
            eventPublisher,
            webSocketHandler
        );

        // Устанавливаем значения @Value полей через reflection
        ReflectionTestUtils.setField(service, "telegramBotUsername", TEST_BOT_USERNAME);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", Duration.ofDays(7));
    }

    @Nested
    @DisplayName("initAuth")
    class InitAuth {

        @Test
        @DisplayName("создаёт токен авторизации и возвращает deeplink")
        void initAuth_CreatesTokenAndReturnsDeeplink() {
            // Arrange
            when(authTokenRepository.save(any(TelegramAuthToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TelegramAuthInitResponse response = service.initAuth();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.token()).isNotNull().isNotBlank();
            assertThat(response.deeplink()).startsWith("https://t.me/" + TEST_BOT_USERNAME + "?start=auth_");
            assertThat(response.expiresAt()).isAfter(Instant.now());

            // Проверяем, что токен сохранён
            ArgumentCaptor<TelegramAuthToken> tokenCaptor = ArgumentCaptor.forClass(TelegramAuthToken.class);
            verify(authTokenRepository).save(tokenCaptor.capture());

            TelegramAuthToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getToken()).isEqualTo(response.token());
            assertThat(savedToken.getStatus()).isEqualTo(AuthTokenStatus.PENDING);
        }

        @Test
        @DisplayName("генерирует уникальные токены")
        void initAuth_GeneratesUniqueTokens() {
            // Arrange
            when(authTokenRepository.save(any(TelegramAuthToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TelegramAuthInitResponse response1 = service.initAuth();
            TelegramAuthInitResponse response2 = service.initAuth();

            // Assert
            assertThat(response1.token()).isNotEqualTo(response2.token());
        }
    }

    @Nested
    @DisplayName("checkStatus")
    class CheckStatus {

        @Test
        @DisplayName("возвращает PENDING для ожидающего токена")
        void checkStatus_PendingToken_ReturnsPending() {
            // Arrange
            String token = "test-token-123";
            TelegramAuthToken authToken = TelegramAuthToken.create(token);

            when(authTokenRepository.findByToken(token)).thenReturn(Optional.of(authToken));

            // Act
            TelegramAuthStatusResponse response = service.checkStatus(token);

            // Assert
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.accessToken()).isNull();
            assertThat(response.user()).isNull();
        }

        @Test
        @DisplayName("возвращает EXPIRED для истёкшего токена")
        void checkStatus_ExpiredToken_ReturnsExpired() {
            // Arrange
            String token = "test-token-123";
            TelegramAuthToken authToken = TelegramAuthToken.create(token);
            // Устанавливаем время истечения в прошлое
            ReflectionTestUtils.setField(authToken, "expiresAt", Instant.now().minusSeconds(60));

            when(authTokenRepository.findByToken(token)).thenReturn(Optional.of(authToken));

            // Act
            TelegramAuthStatusResponse response = service.checkStatus(token);

            // Assert
            assertThat(response.status()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("выбрасывает исключение если токен не найден")
        void checkStatus_TokenNotFound_ThrowsException() {
            // Arrange
            String token = "non-existent-token";
            when(authTokenRepository.findByToken(token)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.checkStatus(token))
                .isInstanceOf(TelegramAuthTokenNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("confirmAuth")
    class ConfirmAuth {

        private Long testTelegramId;
        private String testFirstName;
        private String testLastName;
        private String testUsername;
        private Long testChatId;

        @BeforeEach
        void setUpConfirmAuth() {
            testTelegramId = FAKER.number().randomNumber(9, true);
            testFirstName = FAKER.name().firstName();
            testLastName = FAKER.name().lastName();
            testUsername = FAKER.internet().username();
            testChatId = FAKER.number().randomNumber(9, true);
        }

        @Test
        @DisplayName("успешно подтверждает авторизацию для нового пользователя")
        void confirmAuth_NewUser_CreatesUserAndReturnsTokens() {
            // Arrange
            String token = "test-token-123";
            TelegramAuthToken authToken = TelegramAuthToken.create(token);

            ConfirmTelegramAuthRequest request = new ConfirmTelegramAuthRequest(
                token,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            User newUser = User.createWithTelegram(
                testTelegramId.toString(),
                testChatId.toString(),
                testFirstName,
                testLastName,
                null
            );
            ReflectionTestUtils.setField(newUser, "id", UUID.randomUUID());

            UserDto userDto = createTestUserDto(newUser);

            when(authTokenRepository.findPendingByToken(eq(token), any(Instant.class)))
                .thenReturn(Optional.of(authToken));
            when(authTokenRepository.save(any(TelegramAuthToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findByTelegramId(eq(testTelegramId.toString())))
                .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(any(User.class))).thenReturn(userDto);

            // Act
            service.confirmAuth(request, TEST_USER_AGENT, TEST_IP);

            // Assert
            // Проверяем, что токен подтверждён
            assertThat(authToken.getStatus()).isEqualTo(AuthTokenStatus.USED);
            assertThat(authToken.getTelegramId()).isEqualTo(testTelegramId.toString());

            // Проверяем, что пользователь создан
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getTelegramId()).isEqualTo(testTelegramId.toString());

            // Проверяем, что WebSocket уведомлён
            ArgumentCaptor<AuthResponse> authCaptor = ArgumentCaptor.forClass(AuthResponse.class);
            verify(webSocketHandler).notifyConfirmation(any(String.class), authCaptor.capture());
            assertThat(authCaptor.getValue().accessToken()).isEqualTo(TEST_ACCESS_TOKEN);

            // Проверяем публикацию события регистрации
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("успешно подтверждает авторизацию для существующего пользователя")
        void confirmAuth_ExistingUser_ReturnsTokensWithoutRegistration() {
            // Arrange
            String token = "test-token-123";
            TelegramAuthToken authToken = TelegramAuthToken.create(token);

            ConfirmTelegramAuthRequest request = new ConfirmTelegramAuthRequest(
                token,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            User existingUser = User.createWithTelegram(
                testTelegramId.toString(),
                testChatId.toString(),
                testFirstName,
                testLastName,
                null
            );
            ReflectionTestUtils.setField(existingUser, "id", UUID.randomUUID());

            UserDto userDto = createTestUserDto(existingUser);

            when(authTokenRepository.findPendingByToken(eq(token), any(Instant.class)))
                .thenReturn(Optional.of(authToken));
            when(authTokenRepository.save(any(TelegramAuthToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findByTelegramId(eq(testTelegramId.toString())))
                .thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(any(User.class))).thenReturn(userDto);

            // Act
            service.confirmAuth(request, TEST_USER_AGENT, TEST_IP);

            // Assert
            // Событие регистрации не должно быть опубликовано
            verify(eventPublisher, never()).publish(any());

            // WebSocket должен быть уведомлён
            verify(webSocketHandler).notifyConfirmation(any(String.class), any(AuthResponse.class));
        }

        @Test
        @DisplayName("выбрасывает исключение если токен не найден или истёк")
        void confirmAuth_TokenNotFound_ThrowsException() {
            // Arrange
            String token = "expired-token";
            ConfirmTelegramAuthRequest request = new ConfirmTelegramAuthRequest(
                token,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            when(authTokenRepository.findPendingByToken(eq(token), any(Instant.class)))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.confirmAuth(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(TelegramAuthTokenExpiredException.class);

            verify(userRepository, never()).save(any());
            verify(webSocketHandler, never()).notifyConfirmation(any(), any());
        }
    }

    @Nested
    @DisplayName("TelegramUtils.maskToken")
    class MaskTokenTests {

        @Test
        @DisplayName("маскирует токен корректно")
        void maskToken_ValidToken_ReturnsMasked() {
            String token = "abcdefghijklmnopqrstuvwxyz";
            String masked = ru.aqstream.user.api.util.TelegramUtils.maskToken(token);

            assertThat(masked).isEqualTo("abcdefgh...");
        }

        @Test
        @DisplayName("возвращает *** для null токена")
        void maskToken_NullToken_ReturnsStars() {
            String masked = ru.aqstream.user.api.util.TelegramUtils.maskToken(null);

            assertThat(masked).isEqualTo("***");
        }

        @Test
        @DisplayName("возвращает *** для короткого токена")
        void maskToken_ShortToken_ReturnsStars() {
            String masked = ru.aqstream.user.api.util.TelegramUtils.maskToken("short");

            assertThat(masked).isEqualTo("***");
        }
    }

    // === Вспомогательные методы ===

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
}
