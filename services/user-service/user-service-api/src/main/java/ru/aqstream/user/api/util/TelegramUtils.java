package ru.aqstream.user.api.util;

/**
 * Утилиты для работы с Telegram данными.
 */
public final class TelegramUtils {

    private TelegramUtils() {
        // Утилитный класс
    }

    /**
     * Маскирует Telegram ID для безопасного логирования и отображения в ошибках.
     *
     * <p>Пример: 123456789 -> 123***789</p>
     *
     * @param telegramId Telegram ID для маскирования
     * @return маскированный ID
     */
    public static String maskTelegramId(String telegramId) {
        if (telegramId == null || telegramId.length() <= 6) {
            return "***";
        }
        return telegramId.substring(0, 3) + "***" + telegramId.substring(telegramId.length() - 3);
    }

    /**
     * Маскирует токен авторизации для безопасного логирования.
     *
     * <p>Пример: abcdefghijklmnop -> abcdefgh...</p>
     *
     * @param token токен для маскирования
     * @return маскированный токен (первые 8 символов + ...)
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }
}
