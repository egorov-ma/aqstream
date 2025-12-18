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
import ru.aqstream.user.api.dto.ForgotPasswordRequest;
import ru.aqstream.user.api.dto.LoginRequest;
import ru.aqstream.user.api.dto.RefreshTokenRequest;
import ru.aqstream.user.api.dto.RegisterRequest;
import ru.aqstream.user.api.dto.ResendVerificationRequest;
import ru.aqstream.user.api.dto.ResetPasswordRequest;
import ru.aqstream.user.api.dto.VerifyEmailRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.entity.VerificationToken;
import ru.aqstream.user.db.entity.VerificationToken.TokenType;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.db.repository.VerificationTokenRepository;

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

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String VERIFY_EMAIL_URL = "/api/v1/auth/verify-email";
    private static final String RESEND_VERIFICATION_URL = "/api/v1/auth/resend-verification";
    private static final String FORGOT_PASSWORD_URL = "/api/v1/auth/forgot-password";
    private static final String RESET_PASSWORD_URL = "/api/v1/auth/reset-password";

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

    @Nested
    @DisplayName("POST /api/v1/auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("успешно подтверждает email по валидному токену")
        void verifyEmail_ValidToken_ReturnsNoContent() throws Exception {
            // Регистрируемся
            String email = fakeEmail();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            // Получаем токен верификации из БД
            User user = userRepository.findByEmail(email).orElseThrow();
            VerificationToken token = verificationTokenRepository
                .findValidTokenByUserIdAndType(user.getId(), TokenType.EMAIL_VERIFICATION, java.time.Instant.now())
                .orElseThrow();

            // Верифицируем email
            VerifyEmailRequest verifyRequest = new VerifyEmailRequest(token.getToken());
            mockMvc.perform(post(VERIFY_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isNoContent());

            // Проверяем, что email подтверждён
            User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(verifiedUser.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("возвращает 400 при невалидном токене")
        void verifyEmail_InvalidToken_ReturnsBadRequest() throws Exception {
            VerifyEmailRequest verifyRequest = new VerifyEmailRequest(
                "invalid-token-12345678901234567890123456789012"
            );

            mockMvc.perform(post(VERIFY_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_verification_token"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/resend-verification")
    class ResendVerification {

        @Test
        @DisplayName("успешно отправляет повторное письмо")
        void resendVerification_ValidEmail_ReturnsNoContent() throws Exception {
            // Регистрируемся
            String email = fakeEmail();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            // Запрашиваем повторную отправку
            ResendVerificationRequest resendRequest = new ResendVerificationRequest(email);
            mockMvc.perform(post(RESEND_VERIFICATION_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("не раскрывает существование email")
        void resendVerification_NonExistentEmail_ReturnsNoContent() throws Exception {
            ResendVerificationRequest resendRequest = new ResendVerificationRequest(fakeEmail());

            // Не должен выдавать ошибку для несуществующего email
            mockMvc.perform(post(RESEND_VERIFICATION_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("успешно создаёт токен сброса пароля")
        void forgotPassword_ValidEmail_ReturnsNoContent() throws Exception {
            // Регистрируемся
            String email = fakeEmail();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            // Запрашиваем сброс пароля
            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(email);
            mockMvc.perform(post(FORGOT_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isNoContent());

            // Проверяем, что токен создан
            User user = userRepository.findByEmail(email).orElseThrow();
            assertThat(verificationTokenRepository.findValidTokenByUserIdAndType(
                user.getId(), TokenType.PASSWORD_RESET, java.time.Instant.now()
            )).isPresent();
        }

        @Test
        @DisplayName("не раскрывает существование email")
        void forgotPassword_NonExistentEmail_ReturnsNoContent() throws Exception {
            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(fakeEmail());

            mockMvc.perform(post(FORGOT_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("успешно сбрасывает пароль по валидному токену")
        void resetPassword_ValidToken_ReturnsNoContent() throws Exception {
            // Регистрируемся
            String email = fakeEmail();
            String oldPassword = "Password123";
            RegisterRequest registerRequest = new RegisterRequest(
                email, oldPassword, FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            // Запрашиваем сброс пароля
            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(email);
            mockMvc.perform(post(FORGOT_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isNoContent());

            // Получаем токен из БД
            User user = userRepository.findByEmail(email).orElseThrow();
            VerificationToken token = verificationTokenRepository
                .findValidTokenByUserIdAndType(user.getId(), TokenType.PASSWORD_RESET, java.time.Instant.now())
                .orElseThrow();

            // Сбрасываем пароль
            String newPassword = "NewPassword456";
            ResetPasswordRequest resetRequest = new ResetPasswordRequest(token.getToken(), newPassword);
            mockMvc.perform(post(RESET_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isNoContent());

            // Проверяем, что старый пароль не работает
            LoginRequest oldLoginRequest = new LoginRequest(email, oldPassword);
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(oldLoginRequest)))
                .andExpect(status().isUnauthorized());

            // Проверяем, что новый пароль работает
            LoginRequest newLoginRequest = new LoginRequest(email, newPassword);
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newLoginRequest)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("возвращает 400 при невалидном токене")
        void resetPassword_InvalidToken_ReturnsBadRequest() throws Exception {
            ResetPasswordRequest resetRequest = new ResetPasswordRequest(
                "invalid-token-12345678901234567890123456789012",
                "NewPassword456"
            );

            mockMvc.perform(post(RESET_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_verification_token"));
        }
    }
}
