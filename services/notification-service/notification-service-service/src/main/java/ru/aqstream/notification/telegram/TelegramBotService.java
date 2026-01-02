package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.GetUpdates;
import feign.FeignException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aqstream.notification.config.TelegramProperties;
import ru.aqstream.user.api.dto.ConfirmTelegramAuthRequest;
import ru.aqstream.user.client.UserClient;

/**
 * Сервис для управления Telegram ботом.
 * Обрабатывает входящие сообщения и команды.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService {

    private static final String CONFIRM_AUTH_PREFIX = "confirm_auth:";

    private final TelegramBot bot;
    private final TelegramProperties properties;
    private final TelegramCommandHandler commandHandler;
    private final TelegramMessageSender messageSender;
    private final UserClient userClient;

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
        // Обработка callback_query (нажатие inline-кнопки)
        CallbackQuery callbackQuery = update.callbackQuery();
        if (callbackQuery != null) {
            processCallbackQuery(callbackQuery);
            return;
        }

        // Обработка текстовых сообщений
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
     * Обрабатывает callback_query (нажатие inline-кнопки).
     *
     * @param callbackQuery данные callback запроса
     */
    private void processCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        if (data == null) {
            return;
        }

        log.debug("Получен callback: data={}", maskText(data));

        if (data.startsWith(CONFIRM_AUTH_PREFIX)) {
            String token = data.substring(CONFIRM_AUTH_PREFIX.length());
            handleAuthConfirmation(token, callbackQuery);
        } else {
            log.debug("Неизвестный callback: data={}", maskText(data));
        }
    }

    /**
     * Обрабатывает подтверждение авторизации через Telegram бота.
     *
     * @param token         токен авторизации
     * @param callbackQuery данные callback запроса
     */
    private void handleAuthConfirmation(String token, CallbackQuery callbackQuery) {
        User telegramUser = callbackQuery.from();
        Long chatId = callbackQuery.message().chat().id();

        log.info("Подтверждение авторизации: chatId={}, telegramId={}, token={}...",
                chatId, telegramUser.id(), token.length() > 8 ? token.substring(0, 8) : token);

        try {
            // Вызываем User Service для подтверждения авторизации
            userClient.confirmTelegramAuth(new ConfirmTelegramAuthRequest(
                    token,
                    telegramUser.id(),
                    telegramUser.firstName(),
                    telegramUser.lastName(),
                    telegramUser.username(),
                    chatId
            ));

            // Ответ на callback — всплывающее уведомление
            bot.execute(new AnswerCallbackQuery(callbackQuery.id())
                    .text("✅ Вход подтверждён!"));

            // Сообщение в чат
            messageSender.sendMessage(chatId,
                    "✅ *Вход выполнен!*\n\nВы успешно вошли в AqStream.");

            log.info("Авторизация подтверждена: chatId={}, telegramId={}",
                    chatId, telegramUser.id());

        } catch (FeignException.NotFound e) {
            // Токен не найден или истёк
            bot.execute(new AnswerCallbackQuery(callbackQuery.id())
                    .text("❌ Ссылка устарела"));

            messageSender.sendMessage(chatId,
                    "❌ *Ссылка для входа устарела*\n\nПопробуйте снова на сайте.");

            log.warn("Токен авторизации не найден: token={}...",
                    token.length() > 8 ? token.substring(0, 8) : token);

        } catch (Exception e) {
            // Другая ошибка
            bot.execute(new AnswerCallbackQuery(callbackQuery.id())
                    .text("❌ Ошибка, попробуйте позже"));

            messageSender.sendMessage(chatId,
                    "❌ *Произошла ошибка*\n\nПопробуйте ещё раз или обратитесь в поддержку.");

            log.error("Ошибка подтверждения авторизации: chatId={}, error={}",
                    chatId, e.getMessage(), e);
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
