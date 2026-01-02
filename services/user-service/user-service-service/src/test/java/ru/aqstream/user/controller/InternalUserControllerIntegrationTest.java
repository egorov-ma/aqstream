package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.user.api.dto.ConfirmTelegramAuthRequest;
import ru.aqstream.user.db.entity.AuthTokenStatus;
import ru.aqstream.user.db.entity.TelegramAuthToken;
import ru.aqstream.user.db.repository.TelegramAuthTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.websocket.TelegramAuthWebSocketHandler;

/**
 * Интеграционные тесты для InternalUserController.
 *
 * <p>Тестирует confirm Telegram auth endpoint через полный HTTP стек.</p>
 *
 * <p>WebSocketHandler мокируется, т.к. MockMvc не поддерживает WebSocket</p>
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("InternalUserController Integration Tests")
class InternalUserControllerIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelegramAuthTokenRepository authTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private TelegramAuthWebSocketHandler webSocketHandler;

    private static final String CONFIRM_URL = "/api/v1/internal/users/auth/telegram/confirm";

    @BeforeEach
    void setUp() {
        // Очищаем токены перед каждым тестом
        authTokenRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/v1/internal/users/auth/telegram/confirm")
    class ConfirmTelegramAuth {

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
        @DisplayName("успешное подтверждение — создаёт пользователя и помечает токен USED")
        void confirmAuth_ValidToken_CreatesUserAndMarksTokenUsed() throws Exception {
            // Arrange: создаём pending токен
            String token = "valid-test-token-" + System.currentTimeMillis();
            TelegramAuthToken authToken = TelegramAuthToken.create(token);
            authTokenRepository.save(authToken);

            ConfirmTelegramAuthRequest request = new ConfirmTelegramAuthRequest(
                token,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            // Act
            mockMvc.perform(post(CONFIRM_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // Assert: проверяем что токен помечен как использованный
            TelegramAuthToken updatedToken = authTokenRepository.findByToken(token).orElseThrow();
            assertThat(updatedToken.getStatus()).isEqualTo(AuthTokenStatus.USED);
            assertThat(updatedToken.getTelegramId()).isEqualTo(testTelegramId.toString());

            // Assert: проверяем что пользователь создан
            assertThat(userRepository.findByTelegramId(testTelegramId.toString())).isPresent();
        }

        @Test
        @DisplayName("подтверждение для существующего пользователя — не создаёт дубликат")
        void confirmAuth_ExistingUser_DoesNotCreateDuplicate() throws Exception {
            // Arrange: создаём первый токен и подтверждаем (создаём пользователя)
            String token1 = "token1-" + System.currentTimeMillis();
            TelegramAuthToken authToken1 = TelegramAuthToken.create(token1);
            authTokenRepository.save(authToken1);

            ConfirmTelegramAuthRequest request1 = new ConfirmTelegramAuthRequest(
                token1,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            mockMvc.perform(post(CONFIRM_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

            long userCountBefore = userRepository.count();

            // Arrange: создаём второй токен для того же пользователя
            String token2 = "token2-" + System.currentTimeMillis();
            TelegramAuthToken authToken2 = TelegramAuthToken.create(token2);
            authTokenRepository.save(authToken2);

            ConfirmTelegramAuthRequest request2 = new ConfirmTelegramAuthRequest(
                token2,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            // Act
            mockMvc.perform(post(CONFIRM_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

            // Assert: количество пользователей не изменилось
            long userCountAfter = userRepository.count();
            assertThat(userCountAfter).isEqualTo(userCountBefore);
        }

        @Test
        @DisplayName("истёкший токен — возвращает 404")
        void confirmAuth_ExpiredToken_Returns404() throws Exception {
            // Arrange: создаём токен с истёкшим временем
            String token = "expired-test-token-" + System.currentTimeMillis();
            TelegramAuthToken authToken = TelegramAuthToken.create(token);
            // Устанавливаем время истечения в прошлое
            ReflectionTestUtils.setField(authToken, "expiresAt", Instant.now().minusSeconds(60));
            authTokenRepository.save(authToken);

            ConfirmTelegramAuthRequest request = new ConfirmTelegramAuthRequest(
                token,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            // Act & Assert
            mockMvc.perform(post(CONFIRM_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("несуществующий токен — возвращает 404")
        void confirmAuth_NonExistentToken_Returns404() throws Exception {
            // Arrange
            ConfirmTelegramAuthRequest request = new ConfirmTelegramAuthRequest(
                "non-existent-token-12345",
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            // Act & Assert
            mockMvc.perform(post(CONFIRM_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("уже использованный токен — возвращает 404")
        void confirmAuth_UsedToken_Returns404() throws Exception {
            // Arrange: создаём и сразу помечаем как использованный
            String token = "used-test-token-" + System.currentTimeMillis();
            TelegramAuthToken authToken = TelegramAuthToken.create(token);
            authToken.confirm(
                testTelegramId.toString(),
                testFirstName,
                testLastName,
                testUsername,
                testChatId.toString()
            );
            authToken.markAsUsed();
            authTokenRepository.save(authToken);

            ConfirmTelegramAuthRequest request = new ConfirmTelegramAuthRequest(
                token,
                testTelegramId,
                testFirstName,
                testLastName,
                testUsername,
                testChatId
            );

            // Act & Assert
            mockMvc.perform(post(CONFIRM_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("невалидный запрос (без token) — возвращает 400")
        void confirmAuth_MissingToken_Returns400() throws Exception {
            // Arrange
            String requestJson = """
                {
                    "telegramId": 123456789,
                    "firstName": "Test",
                    "chatId": 123456789
                }
                """;

            // Act & Assert
            mockMvc.perform(post(CONFIRM_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }
    }
}
