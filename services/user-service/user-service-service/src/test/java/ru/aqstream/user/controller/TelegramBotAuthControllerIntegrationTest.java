package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static io.qameta.allure.SeverityLevel.BLOCKER;
import static io.qameta.allure.SeverityLevel.CRITICAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import net.datafaker.Faker;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.api.dto.TelegramAuthInitResponse;
import ru.aqstream.user.db.entity.AuthTokenStatus;
import ru.aqstream.user.db.entity.TelegramAuthToken;
import ru.aqstream.user.db.repository.TelegramAuthTokenRepository;
import ru.aqstream.user.websocket.TelegramAuthWebSocketHandler;

/**
 * Интеграционные тесты для авторизации через Telegram бота.
 *
 * <p>Тестируют полный цикл: HTTP -> Controller -> Service -> Repository -> DB</p>
 *
 * <p>WebSocketHandler мокируется, т.к. MockMvc не поддерживает WebSocket</p>
 */
@IntegrationTest
@AutoConfigureMockMvc
@Feature(AllureFeatures.Features.USER_MANAGEMENT)
@DisplayName("Telegram Bot Auth Integration Tests")
class TelegramBotAuthControllerIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelegramAuthTokenRepository authTokenRepository;

    @MockitoBean
    private TelegramAuthWebSocketHandler webSocketHandler;

    @Value("${telegram.bot.username:AqStreamBot}")
    private String telegramBotUsername;

    private static final String INIT_URL = "/api/v1/auth/telegram/init";
    private static final String STATUS_URL = "/api/v1/auth/telegram/status";

    @BeforeEach
    void setUp() {
        // Очищаем токены перед каждым тестом
        authTokenRepository.deleteAll();
    }

    @Nested
    @Story(AllureFeatures.Stories.TELEGRAM_AUTH)
    @DisplayName("POST /api/v1/auth/telegram/init")
    class InitAuth {

        @Test
        @Severity(BLOCKER)
        @DisplayName("возвращает токен и deeplink")
        void initAuth_ReturnsTokenAndDeeplink() throws Exception {
            MvcResult result = mockMvc.perform(post(INIT_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.deeplink").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();

            // Проверяем формат deeplink
            String responseBody = result.getResponse().getContentAsString();
            TelegramAuthInitResponse response = objectMapper.readValue(
                responseBody, TelegramAuthInitResponse.class);

            assertThat(response.deeplink()).contains("t.me/");
            assertThat(response.deeplink()).contains("?start=auth_");
            assertThat(response.deeplink()).contains(response.token());
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("сохраняет токен в БД со статусом PENDING")
        void initAuth_TokenSavedInDatabase() throws Exception {
            MvcResult result = mockMvc.perform(post(INIT_URL))
                .andExpect(status().isOk())
                .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TelegramAuthInitResponse response = objectMapper.readValue(
                responseBody, TelegramAuthInitResponse.class);

            // Проверяем БД
            TelegramAuthToken savedToken = authTokenRepository.findByToken(response.token())
                .orElse(null);

            assertThat(savedToken).isNotNull();
            assertThat(savedToken.getStatus()).isEqualTo(AuthTokenStatus.PENDING);
            assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
            assertThat(savedToken.getTelegramId()).isNull(); // Ещё не подтверждён
        }

        @Test
        @Severity(BLOCKER)
        @DisplayName("генерирует уникальные токены")
        void initAuth_GeneratesUniqueTokens() throws Exception {
            // Первый запрос
            MvcResult result1 = mockMvc.perform(post(INIT_URL))
                .andExpect(status().isOk())
                .andReturn();

            // Второй запрос
            MvcResult result2 = mockMvc.perform(post(INIT_URL))
                .andExpect(status().isOk())
                .andReturn();

            TelegramAuthInitResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), TelegramAuthInitResponse.class);
            TelegramAuthInitResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(), TelegramAuthInitResponse.class);

            assertThat(response1.token()).isNotEqualTo(response2.token());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.TELEGRAM_AUTH)
    @DisplayName("GET /api/v1/auth/telegram/status/{token}")
    class CheckStatus {

        @Test
        @Severity(CRITICAL)
        @DisplayName("PENDING токен — возвращает статус PENDING")
        void checkStatus_PendingToken_ReturnsPending() throws Exception {
            // Создаём токен через init
            MvcResult initResult = mockMvc.perform(post(INIT_URL))
                .andExpect(status().isOk())
                .andReturn();

            TelegramAuthInitResponse initResponse = objectMapper.readValue(
                initResult.getResponse().getContentAsString(), TelegramAuthInitResponse.class);

            // Проверяем статус
            mockMvc.perform(get(STATUS_URL + "/" + initResponse.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("истёкший токен — возвращает статус EXPIRED")
        void checkStatus_ExpiredToken_ReturnsExpired() throws Exception {
            // Создаём токен напрямую в БД с истёкшим временем
            String token = "expired-test-token-" + System.currentTimeMillis();
            TelegramAuthToken authToken = TelegramAuthToken.create(token);
            // Устанавливаем время истечения в прошлое
            ReflectionTestUtils.setField(authToken, "expiresAt", Instant.now().minusSeconds(60));
            authTokenRepository.save(authToken);

            // Проверяем статус
            mockMvc.perform(get(STATUS_URL + "/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("несуществующий токен — возвращает 404")
        void checkStatus_TokenNotFound_Returns404() throws Exception {
            String nonExistentToken = "non-existent-token-12345";

            mockMvc.perform(get(STATUS_URL + "/" + nonExistentToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(Matchers.containsString("not_found")));
        }
    }
}
