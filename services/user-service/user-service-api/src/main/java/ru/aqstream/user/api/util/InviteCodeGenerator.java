package ru.aqstream.user.api.util;

import java.security.SecureRandom;

/**
 * Генератор уникальных инвайт-кодов для групп.
 *
 * <p>Генерирует 8-символьные коды из алфавита без похожих символов
 * (исключены 0, O, I, L, 1) для удобства ручного ввода.</p>
 */
public final class InviteCodeGenerator {

    /**
     * Длина генерируемого кода.
     */
    public static final int INVITE_CODE_LENGTH = 8;

    /**
     * Алфавит для генерации кода.
     * Исключены похожие символы: 0 (ноль), O (буква), I (буква), L (буква), 1 (единица).
     */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private InviteCodeGenerator() {
        // Утилитный класс
    }

    /**
     * Генерирует случайный инвайт-код.
     *
     * @return 8-символьный код из допустимых символов
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
