package ru.aqstream.user.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aqstream.user.api.util.InviteCodeGenerator;

@DisplayName("InviteCodeGenerator")
class InviteCodeGeneratorTest {

    private static final String ALLOWED_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final String FORBIDDEN_CHARS = "0OIL1";

    @Test
    @DisplayName("Генерирует код длиной 8 символов")
    void generate_ReturnsCorrectLength() {
        String code = InviteCodeGenerator.generate();
        assertThat(code).hasSize(InviteCodeGenerator.INVITE_CODE_LENGTH);
    }

    @Test
    @DisplayName("Использует только допустимые символы")
    void generate_UsesOnlyAllowedChars() {
        // Генерируем много кодов для статистической значимости
        for (int i = 0; i < 100; i++) {
            String code = InviteCodeGenerator.generate();
            for (char c : code.toCharArray()) {
                assertThat(ALLOWED_CHARS).contains(String.valueOf(c));
            }
        }
    }

    @Test
    @DisplayName("Не содержит запрещённых символов (0, O, I, L, 1)")
    void generate_ExcludesForbiddenChars() {
        // Генерируем много кодов для статистической значимости
        for (int i = 0; i < 100; i++) {
            String code = InviteCodeGenerator.generate();
            for (char forbidden : FORBIDDEN_CHARS.toCharArray()) {
                assertThat(code).doesNotContain(String.valueOf(forbidden));
            }
        }
    }

    @Test
    @DisplayName("Генерирует уникальные коды")
    void generate_ProducesUniqueCodes() {
        Set<String> codes = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            codes.add(InviteCodeGenerator.generate());
        }

        // Допускаем минимальное количество коллизий (практически невозможно)
        assertThat(codes.size()).isGreaterThan(count - 5);
    }

    @Test
    @DisplayName("Генерирует коды в верхнем регистре")
    void generate_ProducesUppercaseCodes() {
        for (int i = 0; i < 50; i++) {
            String code = InviteCodeGenerator.generate();
            assertThat(code).isEqualTo(code.toUpperCase());
        }
    }
}
