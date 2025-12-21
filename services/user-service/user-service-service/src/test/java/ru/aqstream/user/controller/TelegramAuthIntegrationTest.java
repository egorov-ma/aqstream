package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.user.api.dto.RegisterRequest;
import ru.aqstream.user.api.dto.TelegramAuthRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Интеграционные тесты для Telegram аутентификации.
 *
 * <p>Тестируют полный цикл: HTTP -> Controller -> Service -> Repository -> DB</p>
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("Telegram Auth Integration Tests")
class TelegramAuthIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Value("${telegram.bot.token:123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11}")
    private String telegramBotToken;

    private static final String TELEGRAM_AUTH_URL = "/api/v1/auth/telegram";
    private static final String TELEGRAM_LINK_URL = "/api/v1/auth/telegram/link";
    private static final String REGISTER_URL = "/api/v1/auth/register";

    @Nested
    @DisplayName("POST /api/v1/auth/telegram")
    class TelegramAuth {

        @Test
        @DisplayName("успешно регистрирует нового пользователя через Telegram")
        void telegramAuth_NewUser_ReturnsOkAndCreatesUser() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            String firstName = FAKER.name().firstName();
            String lastName = FAKER.name().lastName();

            TelegramAuthRequest request = createValidTelegramRequest(
                telegramId, firstName, lastName, null, null
            );

            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.firstName").value(firstName));

            // Проверяем, что пользователь создан в БД
            User createdUser = userRepository.findByTelegramId(telegramId.toString()).orElse(null);
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getTelegramId()).isEqualTo(telegramId.toString());
            assertThat(createdUser.getTelegramChatId()).isEqualTo(telegramId.toString());
            assertThat(createdUser.getFirstName()).isEqualTo(firstName);
            assertThat(createdUser.getLastName()).isEqualTo(lastName);
            // Email не подтверждён, т.к. регистрация через Telegram
            assertThat(createdUser.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("успешно авторизует существующего пользователя")
        void telegramAuth_ExistingUser_ReturnsOkAndAuthenticates() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            String firstName = FAKER.name().firstName();
            String lastName = FAKER.name().lastName();

            // Первый вход — регистрация
            TelegramAuthRequest firstRequest = createValidTelegramRequest(
                telegramId, firstName, lastName, null, null
            );

            MvcResult firstResult = mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andReturn();

            String firstUserId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("user").get("id").asText();

            // Небольшая пауза чтобы auth_date был другим
            Thread.sleep(100);

            // Второй вход — авторизация
            TelegramAuthRequest secondRequest = createValidTelegramRequest(
                telegramId, firstName, lastName, null, null
            );

            MvcResult secondResult = mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

            String secondUserId = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .get("user").get("id").asText();

            // Должен быть тот же пользователь
            assertThat(secondUserId).isEqualTo(firstUserId);
        }

        @Test
        @DisplayName("возвращает 401 при невалидном hash")
        void telegramAuth_InvalidHash_ReturnsUnauthorized() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            long authDate = Instant.now().getEpochSecond();

            // Hash должен быть ровно 64 символа чтобы пройти @Size валидацию
            TelegramAuthRequest request = new TelegramAuthRequest(
                telegramId,
                FAKER.name().firstName(),
                FAKER.name().lastName(),
                null,
                null,
                authDate,
                "0000000000000000000000000000000000000000000000000000000000000000"
            );

            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_telegram_auth"));
        }

        @Test
        @DisplayName("возвращает 401 при устаревшем auth_date")
        void telegramAuth_ExpiredAuthDate_ReturnsUnauthorized() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            String firstName = FAKER.name().firstName();
            // auth_date более 1 часа назад
            long expiredAuthDate = Instant.now().getEpochSecond() - 3700;

            TelegramAuthRequest request = createTelegramRequestWithAuthDate(
                telegramId, firstName, null, null, null, expiredAuthDate
            );

            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_telegram_auth"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("устарели")));
        }

        @Test
        @DisplayName("возвращает 401 при auth_date в будущем")
        void telegramAuth_FutureAuthDate_ReturnsUnauthorized() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            String firstName = FAKER.name().firstName();
            // auth_date в будущем
            long futureAuthDate = Instant.now().getEpochSecond() + 3600;

            TelegramAuthRequest request = createTelegramRequestWithAuthDate(
                telegramId, firstName, null, null, null, futureAuthDate
            );

            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_telegram_auth"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("некорректное время")));
        }

        @Test
        @DisplayName("сохраняет фото профиля из Telegram")
        void telegramAuth_WithPhotoUrl_SavesAvatarUrl() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            String photoUrl = "https://t.me/i/userpic/" + FAKER.number().randomNumber(6, true) + ".jpg";

            TelegramAuthRequest request = createValidTelegramRequest(
                telegramId, FAKER.name().firstName(), null, null, photoUrl
            );

            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.avatarUrl").value(photoUrl));

            User user = userRepository.findByTelegramId(telegramId.toString()).orElseThrow();
            assertThat(user.getAvatarUrl()).isEqualTo(photoUrl);
        }

        @Test
        @DisplayName("обновляет фото при повторном входе")
        void telegramAuth_PhotoUrlChanged_UpdatesAvatar() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            String firstName = FAKER.name().firstName();
            String oldPhotoUrl = "https://t.me/i/userpic/old.jpg";
            String newPhotoUrl = "https://t.me/i/userpic/new.jpg";

            // Первый вход с фото
            TelegramAuthRequest firstRequest = createValidTelegramRequest(
                telegramId, firstName, null, null, oldPhotoUrl
            );
            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

            Thread.sleep(100);

            // Второй вход с новым фото
            TelegramAuthRequest secondRequest = createValidTelegramRequest(
                telegramId, firstName, null, null, newPhotoUrl
            );
            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.avatarUrl").value(newPhotoUrl));

            User user = userRepository.findByTelegramId(telegramId.toString()).orElseThrow();
            assertThat(user.getAvatarUrl()).isEqualTo(newPhotoUrl);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/telegram/link")
    class TelegramLink {

        @Test
        @DisplayName("успешно привязывает Telegram к email-аккаунту")
        void linkTelegram_ValidRequest_LinksTelegramToAccount() throws Exception {
            // Сначала регистрируем пользователя по email
            String email = FAKER.internet().emailAddress();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), FAKER.name().lastName()
            );

            MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

            String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();

            // Теперь привязываем Telegram
            Long telegramId = FAKER.number().randomNumber(9, true);
            TelegramAuthRequest linkRequest = createValidTelegramRequest(
                telegramId, FAKER.name().firstName(), null, null, null
            );

            mockMvc.perform(post(TELEGRAM_LINK_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

            // Проверяем, что Telegram привязан
            User user = userRepository.findByEmail(email).orElseThrow();
            assertThat(user.getTelegramId()).isEqualTo(telegramId.toString());
            assertThat(user.getTelegramChatId()).isEqualTo(telegramId.toString());
        }

        @Test
        @DisplayName("возвращает 409 если Telegram уже привязан к другому аккаунту")
        void linkTelegram_TelegramAlreadyLinked_ReturnsConflict() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);

            // Создаём первого пользователя через Telegram
            TelegramAuthRequest firstTelegramRequest = createValidTelegramRequest(
                telegramId, FAKER.name().firstName(), null, null, null
            );
            mockMvc.perform(post(TELEGRAM_AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstTelegramRequest)))
                .andExpect(status().isOk());

            // Создаём второго пользователя по email
            String email = FAKER.internet().emailAddress();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );

            MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

            String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();

            // Пробуем привязать тот же Telegram ко второму аккаунту
            TelegramAuthRequest linkRequest = createValidTelegramRequest(
                telegramId, FAKER.name().firstName(), null, null, null
            );

            mockMvc.perform(post(TELEGRAM_LINK_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("telegram_id_already_exists"));
        }

        @Test
        @DisplayName("возвращает 401 без авторизации")
        void linkTelegram_NoAuth_ReturnsUnauthorized() throws Exception {
            Long telegramId = FAKER.number().randomNumber(9, true);
            TelegramAuthRequest linkRequest = createValidTelegramRequest(
                telegramId, FAKER.name().firstName(), null, null, null
            );

            mockMvc.perform(post(TELEGRAM_LINK_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isUnauthorized());
        }
    }

    // === Вспомогательные методы ===

    /**
     * Создаёт валидный TelegramAuthRequest с текущим auth_date.
     */
    private TelegramAuthRequest createValidTelegramRequest(
        Long telegramId,
        String firstName,
        String lastName,
        String username,
        String photoUrl
    ) {
        return createTelegramRequestWithAuthDate(
            telegramId, firstName, lastName, username, photoUrl,
            Instant.now().getEpochSecond()
        );
    }

    /**
     * Создаёт TelegramAuthRequest с указанным auth_date.
     */
    private TelegramAuthRequest createTelegramRequestWithAuthDate(
        Long telegramId,
        String firstName,
        String lastName,
        String username,
        String photoUrl,
        long authDate
    ) {
        String hash = calculateTelegramHash(telegramId, firstName, lastName, username, photoUrl, authDate);
        return new TelegramAuthRequest(telegramId, firstName, lastName, username, photoUrl, authDate, hash);
    }

    /**
     * Вычисляет hash для Telegram Login Widget валидации.
     */
    private String calculateTelegramHash(
        Long id,
        String firstName,
        String lastName,
        String username,
        String photoUrl,
        Long authDate
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
            byte[] secretKey = sha256.digest(telegramBotToken.getBytes(StandardCharsets.UTF_8));

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
