package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.BLOCKER;
import static io.qameta.allure.SeverityLevel.CRITICAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.TelegramAuthRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.exception.InvalidTelegramAuthException;
import ru.aqstream.user.api.exception.TelegramIdAlreadyExistsException;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

@UnitTest
@Feature(AllureFeatures.Features.USER_MANAGEMENT)
@DisplayName("TelegramAuthService")
class TelegramAuthServiceTest {

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

    private TelegramAuthService telegramAuthService;

    private static final Faker FAKER = new Faker();

    // Тестовый Telegram Bot Token
    private static final String TEST_BOT_TOKEN = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11";
    private static final String TEST_ACCESS_TOKEN = "access.token.jwt";
    private static final String TEST_REFRESH_TOKEN = "refresh.token.jwt";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";
    private static final String TEST_IP = "127.0.0.1";

    // Генерируемые тестовые данные
    private Long testTelegramId;
    private String testFirstName;
    private String testLastName;
    private String testUsername;
    private String testPhotoUrl;

    @BeforeEach
    void setUp() {
        // Генерируем свежие тестовые данные для каждого теста
        testTelegramId = FAKER.number().randomNumber(9, true);
        testFirstName = FAKER.name().firstName();
        testLastName = FAKER.name().lastName();
        testUsername = FAKER.internet().username();
        testPhotoUrl = "https://t.me/i/userpic/" + FAKER.number().randomNumber(6, true) + ".jpg";

        telegramAuthService = new TelegramAuthService(
            userRepository,
            refreshTokenRepository,
            jwtTokenProvider,
            userMapper,
            eventPublisher
        );

        // Устанавливаем значения @Value полей через reflection
        ReflectionTestUtils.setField(telegramAuthService, "telegramBotToken", TEST_BOT_TOKEN);
        ReflectionTestUtils.setField(telegramAuthService, "accessTokenExpiration",
            java.time.Duration.ofMinutes(15));
        ReflectionTestUtils.setField(telegramAuthService, "refreshTokenExpiration",
            java.time.Duration.ofDays(7));
    }

    @Nested
    @Story(AllureFeatures.Stories.TELEGRAM_AUTH)
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @Severity(BLOCKER)
        @DisplayName("успешно регистрирует нового пользователя через Telegram")
        void authenticate_NewUser_CreatesAndReturnsAuthResponse() {
            // Arrange
            TelegramAuthRequest request = createValidTelegramRequest();

            User savedUser = createTestUserFromTelegram();
            UserDto userDto = createTestUserDto(savedUser);

            when(userRepository.findByTelegramId(testTelegramId.toString())).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(savedUser)).thenReturn(userDto);

            // Act
            AuthResponse response = telegramAuthService.authenticate(request, TEST_USER_AGENT, TEST_IP);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user()).isEqualTo(userDto);

            // Проверяем, что пользователь создан с правильными данными
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getTelegramId()).isEqualTo(testTelegramId.toString());
            assertThat(capturedUser.getFirstName()).isEqualTo(testFirstName);
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("успешно авторизует существующего пользователя")
        void authenticate_ExistingUser_ReturnsAuthResponse() {
            // Arrange
            TelegramAuthRequest request = createValidTelegramRequest();

            User existingUser = createTestUserFromTelegram();
            UserDto userDto = createTestUserDto(existingUser);

            when(userRepository.findByTelegramId(testTelegramId.toString()))
                .thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(existingUser)).thenReturn(userDto);

            // Act
            AuthResponse response = telegramAuthService.authenticate(request, TEST_USER_AGENT, TEST_IP);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("выбрасывает исключение при невалидном hash")
        void authenticate_InvalidHash_ThrowsException() {
            // Arrange
            long authDate = Instant.now().getEpochSecond();
            TelegramAuthRequest request = new TelegramAuthRequest(
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testPhotoUrl,
                authDate,
                "invalid_hash_value_that_does_not_match_at_all_12345678"
            );

            // Act & Assert
            assertThatThrownBy(() -> telegramAuthService.authenticate(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidTelegramAuthException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение при слишком старом auth_date")
        void authenticate_ExpiredAuthDate_ThrowsException() {
            // Arrange - auth_date более 1 часа назад
            long expiredAuthDate = Instant.now().getEpochSecond() - 3700;
            String hash = calculateTelegramHash(testTelegramId, testFirstName, testLastName,
                testUsername, testPhotoUrl, expiredAuthDate);

            TelegramAuthRequest request = new TelegramAuthRequest(
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testPhotoUrl,
                expiredAuthDate,
                hash
            );

            // Act & Assert
            assertThatThrownBy(() -> telegramAuthService.authenticate(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidTelegramAuthException.class)
                .hasMessageContaining("устарели");

            verify(userRepository, never()).save(any());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение при auth_date в будущем")
        void authenticate_FutureAuthDate_ThrowsException() {
            // Arrange - auth_date в будущем (через 1 час)
            long futureAuthDate = Instant.now().getEpochSecond() + 3600;
            String hash = calculateTelegramHash(testTelegramId, testFirstName, testLastName,
                testUsername, testPhotoUrl, futureAuthDate);

            TelegramAuthRequest request = new TelegramAuthRequest(
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testPhotoUrl,
                futureAuthDate,
                hash
            );

            // Act & Assert
            assertThatThrownBy(() -> telegramAuthService.authenticate(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidTelegramAuthException.class)
                .hasMessageContaining("некорректное время");

            verify(userRepository, never()).save(any());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение если bot token не настроен")
        void authenticate_NoBotToken_ThrowsException() {
            // Arrange
            ReflectionTestUtils.setField(telegramAuthService, "telegramBotToken", "");

            TelegramAuthRequest request = createValidTelegramRequest();

            // Act & Assert
            assertThatThrownBy(() -> telegramAuthService.authenticate(request, TEST_USER_AGENT, TEST_IP))
                .isInstanceOf(InvalidTelegramAuthException.class)
                .hasMessageContaining("недоступен");
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.TELEGRAM_AUTH)
    @DisplayName("linkTelegram")
    class LinkTelegram {

        @Test
        @Severity(CRITICAL)
        @DisplayName("успешно привязывает Telegram к существующему аккаунту")
        void linkTelegram_ValidRequest_LinksTelegram() {
            // Arrange
            UUID userId = UUID.randomUUID();
            TelegramAuthRequest request = createValidTelegramRequest();

            User existingUser = User.createWithEmail(
                FAKER.internet().emailAddress(),
                "$2a$12$hashedpassword",
                testFirstName,
                testLastName
            );
            ReflectionTestUtils.setField(existingUser, "id", userId);

            UserDto userDto = createTestUserDto(existingUser);

            when(userRepository.existsByTelegramId(testTelegramId.toString())).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
            when(userMapper.toDto(existingUser)).thenReturn(userDto);

            // Act
            AuthResponse response = telegramAuthService.linkTelegram(
                userId, request, TEST_USER_AGENT, TEST_IP
            );

            // Assert
            assertThat(response).isNotNull();
            assertThat(existingUser.getTelegramId()).isEqualTo(testTelegramId.toString());
            assertThat(existingUser.getTelegramChatId()).isEqualTo(testTelegramId.toString());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("выбрасывает исключение если Telegram уже привязан к другому аккаунту")
        void linkTelegram_TelegramAlreadyLinked_ThrowsException() {
            // Arrange
            UUID userId = UUID.randomUUID();
            TelegramAuthRequest request = createValidTelegramRequest();

            when(userRepository.existsByTelegramId(testTelegramId.toString())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> telegramAuthService.linkTelegram(
                userId, request, TEST_USER_AGENT, TEST_IP
            ))
                .isInstanceOf(TelegramIdAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // === Вспомогательные методы ===

    private TelegramAuthRequest createValidTelegramRequest() {
        long authDate = Instant.now().getEpochSecond();
        String hash = calculateTelegramHash(testTelegramId, testFirstName, testLastName,
            testUsername, testPhotoUrl, authDate);

        return new TelegramAuthRequest(
            testTelegramId,
            testFirstName,
            testLastName,
            testUsername,
            testPhotoUrl,
            authDate,
            hash
        );
    }

    private User createTestUserFromTelegram() {
        User user = User.createWithTelegram(
            testTelegramId.toString(),
            testTelegramId.toString(),
            testFirstName,
            testLastName,
            testPhotoUrl
        );
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

    /**
     * Вычисляет hash для Telegram Login Widget валидации.
     * Используется для создания валидных тестовых запросов.
     */
    private String calculateTelegramHash(
        Long id, String firstName, String lastName,
        String username, String photoUrl, Long authDate
    ) {
        try {
            // Собираем data-check-string в алфавитном порядке
            TreeMap<String, String> data = new TreeMap<>();
            data.put("id", id.toString());
            data.put("first_name", firstName);
            if (lastName != null && !lastName.isBlank()) {
                data.put("last_name", lastName);
            }
            if (username != null && !username.isBlank()) {
                data.put("username", username);
            }
            if (photoUrl != null && !photoUrl.isBlank()) {
                data.put("photo_url", photoUrl);
            }
            data.put("auth_date", authDate.toString());

            StringBuilder dataCheckString = new StringBuilder();
            for (var entry : data.entrySet()) {
                if (dataCheckString.length() > 0) {
                    dataCheckString.append("\n");
                }
                dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
            }

            // Secret key = SHA256(bot_token)
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] secretKey = sha256.digest(TEST_BOT_TOKEN.getBytes(StandardCharsets.UTF_8));

            // Hash = HMAC-SHA256(data_check_string, secret_key)
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hashBytes = hmac.doFinal(dataCheckString.toString().getBytes(StandardCharsets.UTF_8));

            // Конвертируем в hex
            StringBuilder hexHash = new StringBuilder();
            for (byte b : hashBytes) {
                hexHash.append(String.format("%02x", b));
            }

            return hexHash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации тестового hash", e);
        }
    }
}
