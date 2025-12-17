package ru.aqstream.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.user.api.dto.LoginRequest;
import ru.aqstream.user.api.dto.RefreshTokenRequest;
import ru.aqstream.user.api.dto.RegisterRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.UserRepository;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    /**
     * Генерирует уникальный email для теста.
     */
    private static String fakeEmail() {
        return FAKER.internet().emailAddress();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("успешно регистрирует нового пользователя")
        void register_ValidRequest_ReturnsCreated() throws Exception {
            String email = fakeEmail();
            String firstName = FAKER.name().firstName();
            String lastName = FAKER.name().lastName();

            RegisterRequest request = new RegisterRequest(email, "Password123", firstName, lastName);

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.firstName").value(firstName));

            // Проверяем, что пользователь создан в БД
            assertThat(userRepository.existsByEmail(email)).isTrue();
        }

        @Test
        @DisplayName("возвращает 400 при невалидном email")
        void register_InvalidEmail_ReturnsBadRequest() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "invalid-email",
                "Password123",
                FAKER.name().firstName(),
                null
            );

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"));
        }

        @Test
        @DisplayName("возвращает 400 при слабом пароле")
        void register_WeakPassword_ReturnsBadRequest() throws Exception {
            RegisterRequest request = new RegisterRequest(
                fakeEmail(),
                "short",  // Слишком короткий
                FAKER.name().firstName(),
                null
            );

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("возвращает 409 при дублировании email")
        void register_DuplicateEmail_ReturnsConflict() throws Exception {
            String duplicateEmail = fakeEmail();

            // Первая регистрация
            RegisterRequest first = new RegisterRequest(
                duplicateEmail, "Password123", FAKER.name().firstName(), null
            );

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

            // Вторая регистрация с тем же email
            RegisterRequest second = new RegisterRequest(
                duplicateEmail, "Password456", FAKER.name().firstName(), null
            );

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_already_exists"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("успешный вход с корректными данными")
        void login_ValidCredentials_ReturnsOk() throws Exception {
            String email = fakeEmail();
            String password = "Password123";

            // Сначала регистрируем пользователя
            RegisterRequest registerRequest = new RegisterRequest(
                email, password, FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            // Затем входим
            LoginRequest loginRequest = new LoginRequest(email, password);
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
        }

        @Test
        @DisplayName("возвращает 401 при неверном пароле")
        void login_WrongPassword_ReturnsUnauthorized() throws Exception {
            String email = fakeEmail();

            // Регистрируем
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            // Пробуем войти с неверным паролем
            LoginRequest loginRequest = new LoginRequest(email, "WrongPassword123");
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
        }

        @Test
        @DisplayName("возвращает 401 при несуществующем email")
        void login_NonExistentEmail_ReturnsUnauthorized() throws Exception {
            LoginRequest loginRequest = new LoginRequest(fakeEmail(), "Password123");

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
        }

        @Test
        @DisplayName("блокирует аккаунт после 5 неудачных попыток")
        void login_FiveFailedAttempts_ReturnsLockedAccount() throws Exception {
            String email = fakeEmail();

            // Регистрируем
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            // MAX_FAILED_LOGIN_ATTEMPTS - 1 неудачных попыток (возвращают 401)
            LoginRequest wrongLogin = new LoginRequest(email, "WrongPassword");
            for (int i = 0; i < User.MAX_FAILED_LOGIN_ATTEMPTS - 1; i++) {
                mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                    .andExpect(status().isUnauthorized());
            }

            // 5-я попытка должна вернуть 403 (аккаунт заблокирован)
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_locked"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("успешно обновляет токены")
        void refresh_ValidToken_ReturnsNewTokens() throws Exception {
            // Регистрируемся и получаем токены
            RegisterRequest registerRequest = new RegisterRequest(
                fakeEmail(), "Password123", FAKER.name().firstName(), null
            );

            MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

            // Извлекаем refresh token
            String responseJson = registerResult.getResponse().getContentAsString();
            String refreshToken = objectMapper.readTree(responseJson).get("refreshToken").asText();

            // Обновляем токены
            RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

            mockMvc.perform(post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("отклоняет невалидный refresh token")
        void refresh_InvalidToken_ReturnsUnauthorized() throws Exception {
            RefreshTokenRequest refreshRequest = new RefreshTokenRequest("invalid.refresh.token");

            mockMvc.perform(post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("успешный выход")
        void logout_ValidToken_ReturnsNoContent() throws Exception {
            // Регистрируемся
            RegisterRequest registerRequest = new RegisterRequest(
                fakeEmail(), "Password123", FAKER.name().firstName(), null
            );

            MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

            String responseJson = registerResult.getResponse().getContentAsString();
            String refreshToken = objectMapper.readTree(responseJson).get("refreshToken").asText();

            // Выходим
            RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);

            mockMvc.perform(post(LOGOUT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

            // Пробуем использовать отозванный токен — должен быть отклонён
            mockMvc.perform(post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
        }
    }
}
