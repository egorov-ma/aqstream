package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.LinkPreviewOptions;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.aqstream.user.client.UserClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Компонент для отправки сообщений через Telegram Bot API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramMessageSender {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long DEFAULT_RATE_LIMIT_DELAY_MS = 5000;

    /**
     * Отключение предпросмотра ссылок в сообщениях.
     */
    private static final LinkPreviewOptions DISABLED_LINK_PREVIEW = new LinkPreviewOptions().isDisabled(true);

    /**
     * Паттерн для извлечения retry_after из описания ошибки 429.
     * Пример: "Too Many Requests: retry after 35"
     */
    private static final Pattern RETRY_AFTER_PATTERN = Pattern.compile("retry after (\\d+)");

    private final TelegramBot bot;
    private final UserClient userClient;

    /**
     * Отправляет текстовое сообщение с Markdown форматированием.
     *
     * @param chatId ID чата
     * @param text   текст сообщения (Markdown)
     * @return true если сообщение отправлено успешно
     */
    public boolean sendMessage(Long chatId, String text) {
        return sendMessageWithRetry(chatId, text, null, 0);
    }

    /**
     * Отправляет сообщение с inline-кнопками.
     *
     * @param chatId  ID чата
     * @param text    текст сообщения
     * @param buttons массив кнопок [label, callbackData или url, ...]
     * @return true если сообщение отправлено успешно
     */
    public boolean sendMessageWithButtons(Long chatId, String text, String[][] buttons) {
        InlineKeyboardMarkup keyboard = createKeyboard(buttons);
        return sendMessageWithRetry(chatId, text, keyboard, 0);
    }

    /**
     * Отправляет изображение с подписью.
     *
     * @param chatId  ID чата
     * @param photo   байты изображения
     * @param caption подпись к изображению (Markdown)
     * @return true если изображение отправлено успешно
     */
    public boolean sendPhoto(Long chatId, byte[] photo, String caption) {
        return sendPhotoWithRetry(chatId, photo, caption, 0);
    }

    /**
     * Отправляет сообщение с retry механизмом.
     */
    private boolean sendMessageWithRetry(Long chatId, String text, InlineKeyboardMarkup keyboard, int attempt) {
        try {
            SendMessage request = new SendMessage(chatId, text)
                    .parseMode(ParseMode.Markdown)
                    .linkPreviewOptions(DISABLED_LINK_PREVIEW);

            if (keyboard != null) {
                request.replyMarkup(keyboard);
            }

            SendResponse response = bot.execute(request);

            if (response.isOk()) {
                log.debug("Сообщение отправлено: chatId={}", chatId);
                return true;
            }

            return handleError(chatId, response, text, keyboard, attempt);

        } catch (Exception e) {
            log.error("Ошибка отправки сообщения: chatId={}, error={}", chatId, e.getMessage());

            if (attempt < MAX_RETRY_ATTEMPTS) {
                sleep(RETRY_DELAY_MS * (attempt + 1));
                return sendMessageWithRetry(chatId, text, keyboard, attempt + 1);
            }

            return false;
        }
    }

    /**
     * Отправляет изображение с retry механизмом.
     */
    private boolean sendPhotoWithRetry(Long chatId, byte[] photo, String caption, int attempt) {
        try {
            SendPhoto request = new SendPhoto(chatId, photo)
                    .caption(caption)
                    .parseMode(ParseMode.Markdown);

            SendResponse response = bot.execute(request);

            if (response.isOk()) {
                log.debug("Изображение отправлено: chatId={}", chatId);
                return true;
            }

            return handlePhotoError(chatId, response, photo, caption, attempt);

        } catch (Exception e) {
            log.error("Ошибка отправки изображения: chatId={}, error={}", chatId, e.getMessage());

            if (attempt < MAX_RETRY_ATTEMPTS) {
                sleep(RETRY_DELAY_MS * (attempt + 1));
                return sendPhotoWithRetry(chatId, photo, caption, attempt + 1);
            }

            return false;
        }
    }

    /**
     * Обрабатывает ошибку отправки сообщения.
     */
    private boolean handleError(Long chatId, SendResponse response,
                                String text, InlineKeyboardMarkup keyboard, int attempt) {
        int errorCode = response.errorCode();
        String description = response.description();

        if (errorCode == 403) {
            // Пользователь заблокировал бота — очищаем chat_id
            log.warn("Пользователь заблокировал бота: chatId={}", chatId);
            clearTelegramChatIdSafe(chatId);
            return false;
        }

        if (errorCode == 400 && description != null && description.contains("chat not found")) {
            log.warn("Чат не найден: chatId={}", chatId);
            clearTelegramChatIdSafe(chatId);
            return false;
        }

        if (errorCode == 429) {
            // Rate limit — извлекаем retry_after и ждём
            long retryAfterMs = extractRetryAfter(description);
            log.warn("Rate limit: chatId={}, retryAfter={}ms, попытка {}/{}",
                    chatId, retryAfterMs, attempt + 1, MAX_RETRY_ATTEMPTS);
            if (attempt < MAX_RETRY_ATTEMPTS) {
                sleep(retryAfterMs);
                return sendMessageWithRetry(chatId, text, keyboard, attempt + 1);
            }
        }

        log.error("Ошибка Telegram API: chatId={}, code={}, description={}",
                chatId, errorCode, description);

        // Retry для временных ошибок
        if (attempt < MAX_RETRY_ATTEMPTS && isRetryableError(errorCode)) {
            sleep(RETRY_DELAY_MS * (attempt + 1));
            return sendMessageWithRetry(chatId, text, keyboard, attempt + 1);
        }

        return false;
    }

    /**
     * Обрабатывает ошибку отправки изображения.
     */
    private boolean handlePhotoError(Long chatId, SendResponse response,
                                     byte[] photo, String caption, int attempt) {
        int errorCode = response.errorCode();
        String description = response.description();

        if (errorCode == 403) {
            // Пользователь заблокировал бота — очищаем chat_id
            log.warn("Пользователь заблокировал бота: chatId={}", chatId);
            clearTelegramChatIdSafe(chatId);
            return false;
        }

        if (errorCode == 400 && description != null && description.contains("chat not found")) {
            log.warn("Чат не найден: chatId={}", chatId);
            clearTelegramChatIdSafe(chatId);
            return false;
        }

        log.error("Ошибка отправки изображения: chatId={}, code={}, description={}",
                chatId, errorCode, description);

        if (attempt < MAX_RETRY_ATTEMPTS && isRetryableError(errorCode)) {
            sleep(RETRY_DELAY_MS * (attempt + 1));
            return sendPhotoWithRetry(chatId, photo, caption, attempt + 1);
        }

        return false;
    }

    /**
     * Создаёт клавиатуру из массива кнопок.
     * Формат: [[label1, data1], [label2, url2], ...]
     */
    private InlineKeyboardMarkup createKeyboard(String[][] buttons) {
        InlineKeyboardButton[][] keyboardButtons = new InlineKeyboardButton[buttons.length][1];

        for (int i = 0; i < buttons.length; i++) {
            String label = buttons[i][0];
            String data = buttons[i][1];

            InlineKeyboardButton button;
            if (data.startsWith("http://") || data.startsWith("https://")) {
                button = new InlineKeyboardButton(label).url(data);
            } else {
                button = new InlineKeyboardButton(label).callbackData(data);
            }

            keyboardButtons[i][0] = button;
        }

        return new InlineKeyboardMarkup(keyboardButtons);
    }

    /**
     * Проверяет, является ли ошибка временной и стоит ли повторить запрос.
     */
    private boolean isRetryableError(int errorCode) {
        return errorCode == 429  // Rate limit
                || errorCode == 500  // Internal server error
                || errorCode == 502  // Bad gateway
                || errorCode == 503  // Service unavailable
                || errorCode == 504; // Gateway timeout
    }

    /**
     * Задержка между попытками.
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Извлекает retry_after из описания ошибки 429.
     *
     * @param description описание ошибки (например, "Too Many Requests: retry after 35")
     * @return задержка в миллисекундах
     */
    private long extractRetryAfter(String description) {
        if (description == null) {
            return DEFAULT_RATE_LIMIT_DELAY_MS;
        }

        Matcher matcher = RETRY_AFTER_PATTERN.matcher(description);
        if (matcher.find()) {
            try {
                int seconds = Integer.parseInt(matcher.group(1));
                return seconds * 1000L;
            } catch (NumberFormatException e) {
                log.debug("Не удалось распарсить retry_after: {}", description);
            }
        }

        return DEFAULT_RATE_LIMIT_DELAY_MS;
    }

    /**
     * Безопасно очищает telegram_chat_id пользователя через UserClient.
     * Не выбрасывает исключения, чтобы не прерывать основную логику.
     *
     * @param chatId Telegram Chat ID
     */
    private void clearTelegramChatIdSafe(Long chatId) {
        try {
            userClient.clearTelegramChatId(String.valueOf(chatId));
            log.info("Очищен telegram_chat_id: chatId={}", chatId);
        } catch (Exception e) {
            log.error("Ошибка при очистке telegram_chat_id: chatId={}, error={}", chatId, e.getMessage());
        }
    }
}
