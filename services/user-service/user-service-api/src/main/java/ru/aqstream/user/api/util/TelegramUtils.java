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
}
