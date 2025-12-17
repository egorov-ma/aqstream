package ru.aqstream.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Утилита для хеширования токенов.
 *
 * <p>Используется для безопасного хранения refresh токенов в БД.
 * Применяет SHA-256 для создания необратимого хеша.</p>
 *
 * <p>Пример использования:</p>
 * <pre>
 * String tokenHash = TokenHasher.hash(refreshToken);
 * refreshTokenRepository.save(new RefreshToken(user, tokenHash, ...));
 * </pre>
 */
public final class TokenHasher {

    private static final String ALGORITHM = "SHA-256";

    private TokenHasher() {
        // Утилитный класс
    }

    /**
     * Хеширует токен с использованием SHA-256.
     *
     * @param token токен для хеширования
     * @return hex-строка хеша
     * @throws IllegalStateException если SHA-256 не поддерживается (не должно случиться)
     */
    public static String hash(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Токен не может быть null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 не поддерживается", e);
        }
    }
}
