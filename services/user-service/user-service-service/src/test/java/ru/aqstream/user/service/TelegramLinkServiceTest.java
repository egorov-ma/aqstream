package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.NORMAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.api.dto.LinkTelegramByTokenRequest;
import ru.aqstream.user.api.exception.TelegramIdAlreadyExistsException;
import ru.aqstream.user.api.exception.TelegramLinkTokenNotFoundException;
import ru.aqstream.user.db.entity.TelegramLinkToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.TelegramLinkTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

@UnitTest
@Feature(AllureFeatures.Features.USER_MANAGEMENT)
@DisplayName("TelegramLinkService")
class TelegramLinkServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private TelegramLinkTokenRepository linkTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TelegramLinkService telegramLinkService;

    @Captor
    private ArgumentCaptor<TelegramLinkToken> tokenCaptor;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = createTestUser();
        ReflectionTestUtils.setField(testUser, "id", userId);
    }

    @Nested
    @Story(AllureFeatures.Stories.PROFILE)
    @DisplayName("createLinkToken")
    class CreateLinkToken {

        @Test
        @Severity(NORMAL)
        @DisplayName("создаёт токен привязки для пользователя")
        void createLinkToken_ValidUser_CreatesToken() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(linkTokenRepository.invalidateAllByUserId(eq(userId), any())).thenReturn(0);

            // When
            String token = telegramLinkService.createLinkToken(userId);

            // Then
            assertThat(token).isNotNull().isNotBlank();

            verify(linkTokenRepository).save(tokenCaptor.capture());
            TelegramLinkToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getUser()).isEqualTo(testUser);
            assertThat(savedToken.getToken()).isEqualTo(token);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("инвалидирует предыдущие токены")
        void createLinkToken_HasPreviousTokens_InvalidatesThem() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(linkTokenRepository.invalidateAllByUserId(eq(userId), any())).thenReturn(2);

            // When
            telegramLinkService.createLinkToken(userId);

            // Then
            verify(linkTokenRepository).invalidateAllByUserId(eq(userId), any());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если пользователь не найден")
        void createLinkToken_UserNotFound_ThrowsException() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> telegramLinkService.createLinkToken(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь не найден");
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.PROFILE)
    @DisplayName("linkTelegramByToken")
    class LinkTelegramByToken {

        private TelegramLinkToken validToken;
        private LinkTelegramByTokenRequest request;

        @BeforeEach
        void setUp() {
            validToken = TelegramLinkToken.create(testUser, "valid-token-12345");
            request = new LinkTelegramByTokenRequest(
                "valid-token-12345",
                123456789L,
                987654321L
            );
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("привязывает Telegram при валидном токене")
        void linkTelegramByToken_ValidToken_LinksTelegram() {
            // Given
            when(linkTokenRepository.findByToken(request.linkToken()))
                .thenReturn(Optional.of(validToken));
            when(userRepository.existsByTelegramId("123456789")).thenReturn(false);

            // When
            telegramLinkService.linkTelegramByToken(request);

            // Then
            assertThat(testUser.getTelegramId()).isEqualTo("123456789");
            assertThat(testUser.getTelegramChatId()).isEqualTo("987654321");
            verify(userRepository).save(testUser);
            verify(linkTokenRepository).save(validToken);
            assertThat(validToken.isUsed()).isTrue();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если токен не найден")
        void linkTelegramByToken_TokenNotFound_ThrowsException() {
            // Given
            when(linkTokenRepository.findByToken(request.linkToken()))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> telegramLinkService.linkTelegramByToken(request))
                .isInstanceOf(TelegramLinkTokenNotFoundException.class);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если токен уже использован")
        void linkTelegramByToken_TokenUsed_ThrowsException() {
            // Given
            validToken.markAsUsed();
            when(linkTokenRepository.findByToken(request.linkToken()))
                .thenReturn(Optional.of(validToken));

            // When/Then
            assertThatThrownBy(() -> telegramLinkService.linkTelegramByToken(request))
                .isInstanceOf(TelegramLinkTokenNotFoundException.class);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если Telegram ID уже привязан")
        void linkTelegramByToken_TelegramIdExists_ThrowsException() {
            // Given
            when(linkTokenRepository.findByToken(request.linkToken()))
                .thenReturn(Optional.of(validToken));
            when(userRepository.existsByTelegramId("123456789")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> telegramLinkService.linkTelegramByToken(request))
                .isInstanceOf(TelegramIdAlreadyExistsException.class);
        }
    }

    /**
     * Создаёт тестового пользователя.
     */
    private User createTestUser() {
        return User.createWithEmail(
            FAKER.internet().emailAddress(),
            "$2a$12$hashedpassword",
            FAKER.name().firstName(),
            FAKER.name().lastName()
        );
    }
}
