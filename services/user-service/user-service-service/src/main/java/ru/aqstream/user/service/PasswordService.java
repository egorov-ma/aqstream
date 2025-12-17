package ru.aqstream.user.service;

import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.aqstream.common.api.exception.ValidationException;

/**
 * Сервис для работы с паролями.
 * Валидация, хеширование (bcrypt), проверка соответствия.
 */
@Service
public class PasswordService {

    private static final int BCRYPT_STRENGTH = 12;

    // Пароль должен содержать минимум одну букву и одну цифру
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-zА-Яа-яЁё])(?=.*\\d).+$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 100;

    private final BCryptPasswordEncoder encoder;

    public PasswordService() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Валидирует пароль согласно требованиям безопасности.
     *
     * @param password пароль для валидации
     * @throws ValidationException если пароль не соответствует требованиям
     */
    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Пароль обязателен");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                String.format("Пароль должен содержать минимум %d символов", MIN_PASSWORD_LENGTH)
            );
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException(
                String.format("Пароль не должен превышать %d символов", MAX_PASSWORD_LENGTH)
            );
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("Пароль должен содержать буквы и цифры");
        }
    }

    /**
     * Хеширует пароль с использованием bcrypt.
     *
     * @param rawPassword открытый пароль
     * @return хеш пароля
     */
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * Проверяет соответствие пароля хешу.
     *
     * @param rawPassword открытый пароль
     * @param encodedPassword хеш пароля
     * @return true если пароль соответствует хешу
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
