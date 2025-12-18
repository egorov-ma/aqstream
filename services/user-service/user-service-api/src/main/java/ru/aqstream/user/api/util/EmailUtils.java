package ru.aqstream.user.api.util;

/**
 * Утилиты для работы с email данными.
 */
public final class EmailUtils {

    private EmailUtils() {
        // Утилитный класс
    }

    /**
     * Маскирует email для безопасного логирования и отображения в ошибках.
     *
     * <p>Пример: user@example.com -> u***@example.com</p>
     *
     * @param email email для маскирования
     * @return маскированный email
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
