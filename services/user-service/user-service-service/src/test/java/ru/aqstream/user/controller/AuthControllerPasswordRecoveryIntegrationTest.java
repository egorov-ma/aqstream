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
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.db.repository.VerificationTokenRepository;

/**
 * Интеграционные тесты для подтверждения email и восстановления пароля.
 */
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("AuthController Password Recovery Integration Tests")
class AuthControllerPasswordRecoveryIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String VERIFY_EMAIL_URL = "/api/v1/auth/verify-email";
    private static final String RESEND_VERIFICATION_URL = "/api/v1/auth/resend-verification";
    private static final String FORGOT_PASSWORD_URL = "/api/v1/auth/forgot-password";
    private static final String RESET_PASSWORD_URL = "/api/v1/auth/reset-password";

    private static String fakeEmail() {
        return FAKER.internet().emailAddress();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("успешно подтверждает email по валидному токену")
        void verifyEmail_ValidToken_ReturnsNoContent() throws Exception {
            String email = fakeEmail();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            User user = userRepository.findByEmail(email).orElseThrow();
            VerificationToken token = verificationTokenRepository
                .findValidTokenByUserIdAndType(user.getId(), TokenType.EMAIL_VERIFICATION, java.time.Instant.now())
                .orElseThrow();

            VerifyEmailRequest verifyRequest = new VerifyEmailRequest(token.getToken());
            mockMvc.perform(post(VERIFY_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isNoContent());

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
            String email = fakeEmail();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

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
            String email = fakeEmail();
            RegisterRequest registerRequest = new RegisterRequest(
                email, "Password123", FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(email);
            mockMvc.perform(post(FORGOT_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isNoContent());

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
            String email = fakeEmail();
            String oldPassword = "Password123";
            RegisterRequest registerRequest = new RegisterRequest(
                email, oldPassword, FAKER.name().firstName(), null
            );
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(email);
            mockMvc.perform(post(FORGOT_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isNoContent());

            User user = userRepository.findByEmail(email).orElseThrow();
            VerificationToken token = verificationTokenRepository
                .findValidTokenByUserIdAndType(user.getId(), TokenType.PASSWORD_RESET, java.time.Instant.now())
                .orElseThrow();

            String newPassword = "NewPassword456";
            ResetPasswordRequest resetRequest = new ResetPasswordRequest(token.getToken(), newPassword);
            mockMvc.perform(post(RESET_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isNoContent());

            LoginRequest oldLoginRequest = new LoginRequest(email, oldPassword);
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(oldLoginRequest)))
                .andExpect(status().isUnauthorized());

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

        @Test
        @DisplayName("отзывает все refresh токены после сброса пароля")
        void resetPassword_ValidToken_RevokesAllRefreshTokens() throws Exception {
            String email = fakeEmail();
            String oldPassword = "Password123";
            RegisterRequest registerRequest = new RegisterRequest(
                email, oldPassword, FAKER.name().firstName(), null
            );

            MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

            String responseJson = registerResult.getResponse().getContentAsString();
            String refreshToken = objectMapper.readTree(responseJson).get("refreshToken").asText();

            RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
            mockMvc.perform(post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk());

            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(email);
            mockMvc.perform(post(FORGOT_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isNoContent());

            User user = userRepository.findByEmail(email).orElseThrow();
            VerificationToken resetToken = verificationTokenRepository
                .findValidTokenByUserIdAndType(user.getId(), TokenType.PASSWORD_RESET, java.time.Instant.now())
                .orElseThrow();

            String newPassword = "NewPassword456";
            ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(resetToken.getToken(), newPassword);
            mockMvc.perform(post(RESET_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetPasswordRequest)))
                .andExpect(status().isNoContent());

            mockMvc.perform(post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());

            long activeTokens = refreshTokenRepository.countActiveByUserId(user.getId(), java.time.Instant.now());
            assertThat(activeTokens).isZero();
        }
    }
}
