package ru.aqstream.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.PostgresTestContainer;
import ru.aqstream.user.api.dto.ChangePasswordRequest;
import ru.aqstream.user.api.dto.UpdateProfileRequest;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.service.PasswordService;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("ProfileController Integration Tests")
class ProfileControllerIntegrationTest extends PostgresTestContainer {

    private static final Faker FAKER = new Faker();

    private static final String UPDATE_PROFILE_URL = "/api/v1/users/me";
    private static final String CHANGE_PASSWORD_URL = "/api/v1/auth/change-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Создаёт тестового пользователя с указанным паролем.
     */
    private User createTestUser(String rawPassword) {
        String email = FAKER.internet().emailAddress();
        String firstName = FAKER.name().firstName();
        String lastName = FAKER.name().lastName();
        String passwordHash = passwordService.hash(rawPassword);

        User user = User.createWithEmail(email, passwordHash, firstName, lastName);
        user.verifyEmail(); // Подтверждаем email для возможности входа
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/me")
    class UpdateProfile {

        @Test
        @DisplayName("успешно обновляет профиль")
        void updateProfile_ValidRequest_ReturnsOk() throws Exception {
            // Arrange
            String password = FAKER.internet().password(8, 20, true, false, true);
            User user = createTestUser(password);

            String newFirstName = FAKER.name().firstName();
            String newLastName = FAKER.name().lastName();
            UpdateProfileRequest request = new UpdateProfileRequest(newFirstName, newLastName);

            // Act & Assert
            mockMvc.perform(patch(UPDATE_PROFILE_URL)
                    .with(jwt(jwtTokenProvider, user.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(newFirstName))
                .andExpect(jsonPath("$.lastName").value(newLastName))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
        }

        @Test
        @DisplayName("возвращает 401 без авторизации")
        void updateProfile_NoAuth_ReturnsUnauthorized() throws Exception {
            // Arrange
            UpdateProfileRequest request = new UpdateProfileRequest("Test", "User");

            // Act & Assert
            mockMvc.perform(patch(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("возвращает 400 для пустого firstName")
        void updateProfile_EmptyFirstName_ReturnsBadRequest() throws Exception {
            // Arrange
            String password = FAKER.internet().password(8, 20, true, false, true);
            User user = createTestUser(password);

            UpdateProfileRequest request = new UpdateProfileRequest("", "Иванов");

            // Act & Assert
            mockMvc.perform(patch(UPDATE_PROFILE_URL)
                    .with(jwt(jwtTokenProvider, user.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/change-password")
    class ChangePassword {

        @Test
        @DisplayName("успешно меняет пароль")
        void changePassword_ValidRequest_ReturnsNoContent() throws Exception {
            // Arrange
            String currentPassword = FAKER.internet().password(8, 20, true, false, true);
            User user = createTestUser(currentPassword);

            String newPassword = FAKER.internet().password(8, 20, true, false, true);
            ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

            // Act & Assert
            mockMvc.perform(post(CHANGE_PASSWORD_URL)
                    .with(jwt(jwtTokenProvider, user.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

            // Verify new password works
            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(
                passwordService.matches(newPassword, updatedUser.getPasswordHash())
            ).isTrue();
        }

        @Test
        @DisplayName("возвращает 401 для неверного текущего пароля")
        void changePassword_WrongCurrentPassword_ReturnsUnauthorized() throws Exception {
            // Arrange
            String currentPassword = FAKER.internet().password(8, 20, true, false, true);
            User user = createTestUser(currentPassword);

            String wrongPassword = FAKER.internet().password(8, 20, true, false, true);
            String newPassword = FAKER.internet().password(8, 20, true, false, true);
            ChangePasswordRequest request = new ChangePasswordRequest(wrongPassword, newPassword);

            // Act & Assert
            mockMvc.perform(post(CHANGE_PASSWORD_URL)
                    .with(jwt(jwtTokenProvider, user.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("wrong_password"));
        }

        @Test
        @DisplayName("возвращает 401 без авторизации")
        void changePassword_NoAuth_ReturnsUnauthorized() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("old", "NewPass123");

            // Act & Assert
            mockMvc.perform(post(CHANGE_PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("возвращает 400 для слабого пароля")
        void changePassword_WeakPassword_ReturnsBadRequest() throws Exception {
            // Arrange
            String currentPassword = FAKER.internet().password(8, 20, true, false, true);
            User user = createTestUser(currentPassword);

            // Пароль без цифр — слабый
            ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, "NoDigitsPassword");

            // Act & Assert
            mockMvc.perform(post(CHANGE_PASSWORD_URL)
                    .with(jwt(jwtTokenProvider, user.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }
}
