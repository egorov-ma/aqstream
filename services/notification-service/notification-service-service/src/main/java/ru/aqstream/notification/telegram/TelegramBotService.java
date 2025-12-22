package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetUpdates;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aqstream.notification.config.TelegramProperties;

/**
 * Сервис для управления Telegram ботом.
 * Обрабатывает входящие сообщения и команды.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService {

    private final TelegramBot bot;
    private final TelegramProperties properties;
    private final TelegramCommandHandler commandHandler;

    /**
     * Инициализация бота при старте приложения.
     */
    @PostConstruct
    public void init() {
        if (!properties.isWebhookEnabled()) {
            startLongPolling();
        }
        log.info("Telegram бот инициализирован: @{}", properties.getBotUsername());
    }

    /**
     * Остановка бота при завершении приложения.
     */
    @PreDestroy
    public void shutdown() {
        bot.removeGetUpdatesListener();
        log.info("Telegram бот остановлен");
    }

    /**
     * Запускает long polling для получения обновлений.
     */
    private void startLongPolling() {
        // Настраиваем timeout для long polling из конфигурации
        GetUpdates getUpdatesRequest = new GetUpdates()
                .timeout(properties.getLongPollingTimeout());

        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                try {
                    processUpdate(update);
                } catch (Exception e) {
                    log.error("Ошибка обработки update: updateId={}, error={}",
                            update.updateId(), e.getMessage(), e);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                log.error("Ошибка Telegram API: code={}, description={}",
                        e.response().errorCode(), e.response().description());
            } else {
                log.error("Ошибка сети Telegram: {}", e.getMessage());
            }
        }, getUpdatesRequest);

        log.info("Long polling запущен: timeout={}s", properties.getLongPollingTimeout());
    }

    /**
     * Обрабатывает входящее обновление от Telegram.
     */
    public void processUpdate(Update update) {
        Message message = update.message();

        if (message == null || message.text() == null) {
            return;
        }

        String text = message.text();
        Long chatId = message.chat().id();

        log.debug("Получено сообщение: chatId={}, text={}", chatId, maskText(text));

        if (text.startsWith("/start")) {
            commandHandler.handleStart(chatId, text, message.from());
        } else if (text.equals("/help")) {
            commandHandler.handleHelp(chatId);
        } else {
            // Неизвестная команда или обычное сообщение — игнорируем
            log.debug("Неизвестная команда: chatId={}", chatId);
        }
    }

    /**
     * Маскирует текст для логирования (первые 20 символов).
     */
    private String maskText(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= 20) {
            return text;
        }
        return text.substring(0, 20) + "...";
    }
}
