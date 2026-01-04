package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.NORMAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.user.api.dto.ChangePasswordRequest;
import ru.aqstream.user.api.dto.UpdateProfileRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.exception.UserNotFoundException;
import ru.aqstream.user.api.exception.WrongPasswordException;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.UserRepository;

@UnitTest
@Feature(AllureFeatures.Features.USER_MANAGEMENT)
@DisplayName("ProfileService")
class ProfileServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private User user;
    private String email;
    private String firstName;
    private String lastName;
    private String passwordHash;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = FAKER.internet().emailAddress();
        firstName = FAKER.name().firstName();
        lastName = FAKER.name().lastName();
        passwordHash = FAKER.internet().password(60, 60);

        // Используем фабричный метод для создания User
        user = User.createWithEmail(email, passwordHash, firstName, lastName);
    }

    @Nested
    @Story(AllureFeatures.Stories.PROFILE)
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @Severity(NORMAL)
        @DisplayName("успешно обновляет профиль")
        void updateProfile_ValidRequest_ReturnsUpdatedUser() {
            // Arrange
            String newFirstName = FAKER.name().firstName();
            String newLastName = FAKER.name().lastName();
            UpdateProfileRequest request = new UpdateProfileRequest(newFirstName, newLastName);

            UserDto expectedDto = new UserDto(
                userId,
                email,
                newFirstName,
                newLastName,
                null,
                false,
                false,
                Instant.now()
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(expectedDto);

            // Act
            UserDto result = profileService.updateProfile(userId, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.firstName()).isEqualTo(newFirstName);
            assertThat(result.lastName()).isEqualTo(newLastName);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFirstName()).isEqualTo(newFirstName);
            assertThat(savedUser.getLastName()).isEqualTo(newLastName);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает UserNotFoundException если пользователь не найден")
        void updateProfile_UserNotFound_ThrowsException() {
            // Arrange
            UpdateProfileRequest request = new UpdateProfileRequest(firstName, lastName);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> profileService.updateProfile(userId, request))
                .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("обновляет только firstName если lastName null")
        void updateProfile_NullLastName_UpdatesOnlyFirstName() {
            // Arrange
            String newFirstName = FAKER.name().firstName();
            UpdateProfileRequest request = new UpdateProfileRequest(newFirstName, null);

            UserDto expectedDto = new UserDto(
                userId,
                email,
                newFirstName,
                null,
                null,
                false,
                false,
                Instant.now()
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(expectedDto);

            // Act
            UserDto result = profileService.updateProfile(userId, request);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFirstName()).isEqualTo(newFirstName);
            assertThat(savedUser.getLastName()).isNull();
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.PROFILE)
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @Severity(NORMAL)
        @DisplayName("успешно меняет пароль")
        void changePassword_ValidRequest_ChangesPassword() {
            // Arrange
            String currentPassword = FAKER.internet().password(8, 20, true, false, true);
            String newPassword = FAKER.internet().password(8, 20, true, false, true);
            String newPasswordHash = FAKER.internet().password(60, 60);
            ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordService.matches(currentPassword, passwordHash)).thenReturn(true);
            when(passwordService.hash(newPassword)).thenReturn(newPasswordHash);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            profileService.changePassword(userId, request);

            // Assert
            verify(passwordService).validate(newPassword);
            verify(passwordService).hash(newPassword);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPasswordHash()).isEqualTo(newPasswordHash);
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает WrongPasswordException если текущий пароль неверный")
        void changePassword_WrongCurrentPassword_ThrowsException() {
            // Arrange
            String wrongPassword = FAKER.internet().password(8, 20, true, false, true);
            String newPassword = FAKER.internet().password(8, 20, true, false, true);
            ChangePasswordRequest request = new ChangePasswordRequest(wrongPassword, newPassword);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordService.matches(wrongPassword, passwordHash)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> profileService.changePassword(userId, request))
                .isInstanceOf(WrongPasswordException.class);

            verify(passwordService, never()).validate(any());
            verify(passwordService, never()).hash(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает UserNotFoundException если пользователь не найден")
        void changePassword_UserNotFound_ThrowsException() {
            // Arrange
            String currentPassword = FAKER.internet().password(8, 20, true, false, true);
            String newPassword = FAKER.internet().password(8, 20, true, false, true);
            ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> profileService.changePassword(userId, request))
                .isInstanceOf(UserNotFoundException.class);

            verify(passwordService, never()).matches(any(), any());
            verify(userRepository, never()).save(any());
        }
    }
}
