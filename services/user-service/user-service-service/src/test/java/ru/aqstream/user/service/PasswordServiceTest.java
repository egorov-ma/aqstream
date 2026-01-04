package ru.aqstream.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static io.qameta.allure.SeverityLevel.NORMAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.aqstream.common.api.exception.ValidationException;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;

@UnitTest
@Feature(AllureFeatures.Features.USER_MANAGEMENT)
@DisplayName("PasswordService")
class PasswordServiceTest {

    private static final Faker FAKER = new Faker();

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }

    @Nested
    @Story(AllureFeatures.Stories.PROFILE)
    @DisplayName("validate")
    class Validate {

        @Test
        @Severity(NORMAL)
        @DisplayName("принимает корректный пароль")
        void validate_ValidPassword_NoException() {
            // Генерируем валидные пароли с буквами и цифрами
            String password1 = FAKER.internet().password(8, 20, true, false, true);
            String password2 = FAKER.internet().password(8, 20, true, false, true);

            passwordService.validate(password1);
            passwordService.validate(password2);
            // Дополнительно проверяем пароль с кириллицей
            passwordService.validate("Пароль123");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("отклоняет null пароль")
        void validate_NullPassword_ThrowsException() {
            assertThatThrownBy(() -> passwordService.validate(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("обязателен");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("отклоняет пустой пароль")
        void validate_EmptyPassword_ThrowsException() {
            assertThatThrownBy(() -> passwordService.validate(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("обязателен");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("отклоняет короткий пароль")
        void validate_ShortPassword_ThrowsException() {
            assertThatThrownBy(() -> passwordService.validate("Pass1"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("минимум 8 символов");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("отклоняет пароль без цифр")
        void validate_NoDigits_ThrowsException() {
            assertThatThrownBy(() -> passwordService.validate("PasswordWithoutDigits"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("буквы и цифры");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("отклоняет пароль без букв")
        void validate_NoLetters_ThrowsException() {
            assertThatThrownBy(() -> passwordService.validate("123456789"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("буквы и цифры");
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("отклоняет слишком длинный пароль")
        void validate_TooLongPassword_ThrowsException() {
            String longPassword = "a".repeat(101) + "1";
            assertThatThrownBy(() -> passwordService.validate(longPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("превышать");
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.PROFILE)
    @DisplayName("hash и matches")
    class HashAndMatches {

        @Test
        @Severity(NORMAL)
        @DisplayName("хеширует пароль и успешно проверяет его")
        void hash_AndMatches_Success() {
            String rawPassword = FAKER.internet().password(8, 20, true, false, true);
            String hash = passwordService.hash(rawPassword);

            assertThat(hash).isNotNull();
            assertThat(hash).isNotEqualTo(rawPassword);
            assertThat(passwordService.matches(rawPassword, hash)).isTrue();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("разные хеши для одного пароля")
        void hash_SamePassword_DifferentHashes() {
            String rawPassword = FAKER.internet().password(8, 20, true, false, true);
            String hash1 = passwordService.hash(rawPassword);
            String hash2 = passwordService.hash(rawPassword);

            // bcrypt генерирует разные хеши благодаря соли
            assertThat(hash1).isNotEqualTo(hash2);
            // Но оба валидны
            assertThat(passwordService.matches(rawPassword, hash1)).isTrue();
            assertThat(passwordService.matches(rawPassword, hash2)).isTrue();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("не совпадает с неверным паролем")
        void matches_WrongPassword_ReturnsFalse() {
            String rawPassword = FAKER.internet().password(8, 20, true, false, true);
            String wrongPassword = FAKER.internet().password(8, 20, true, false, true);
            String hash = passwordService.hash(rawPassword);

            assertThat(passwordService.matches(wrongPassword, hash)).isFalse();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает false для null значений")
        void matches_NullValues_ReturnsFalse() {
            String password = FAKER.internet().password(8, 20, true, false, true);
            String hash = passwordService.hash(password);

            assertThat(passwordService.matches(null, hash)).isFalse();
            assertThat(passwordService.matches(password, null)).isFalse();
            assertThat(passwordService.matches(null, null)).isFalse();
        }
    }
}
