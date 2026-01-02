package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.aqstream.notification.config.TelegramProperties;

/**
 * Обработчик команд Telegram бота.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramCommandHandler {

    private static final String AUTH_PREFIX = "auth_";
    private static final String INVITE_PREFIX = "invite_";
    private static final String LINK_PREFIX = "link_";
    private static final String REG_PREFIX = "reg_";

    private final TelegramMessageSender messageSender;
    private final TelegramProperties properties;
    private final DeeplinkHandler deeplinkHandler;

    /**
     * Обработка команды /start.
     *
     * @param chatId ID чата
     * @param text   полный текст команды
     * @param from   информация о пользователе
     */
    public void handleStart(Long chatId, String text, User from) {
        String param = extractStartParam(text);

        if (param == null) {
            // /start без параметров — приветствие
            sendWelcomeMessage(chatId, from);
            return;
        }

        // Обработка deeplinks
        if (param.startsWith(AUTH_PREFIX)) {
            String authToken = param.substring(AUTH_PREFIX.length());
            deeplinkHandler.handleAuth(chatId, authToken, from);
        } else if (param.startsWith(INVITE_PREFIX)) {
            String inviteCode = param.substring(INVITE_PREFIX.length());
            deeplinkHandler.handleInvite(chatId, inviteCode, from);
        } else if (param.startsWith(LINK_PREFIX)) {
            String linkToken = param.substring(LINK_PREFIX.length());
            deeplinkHandler.handleLink(chatId, linkToken, from);
        } else if (param.startsWith(REG_PREFIX)) {
            String registrationId = param.substring(REG_PREFIX.length());
            deeplinkHandler.handleRegistration(chatId, registrationId, from);
        } else {
            log.warn("Неизвестный deeplink параметр: chatId={}, param={}", chatId, param);
            sendWelcomeMessage(chatId, from);
        }
    }

    /**
     * Обработка команды /help.
     *
     * @param chatId ID чата
     */
    public void handleHelp(Long chatId) {
        String helpMessage = """
                *Помощь по боту AqStream*

                Доступные команды:
                /start — начать работу с ботом
                /help — показать эту справку

                Бот отправляет уведомления о ваших регистрациях на события:
                • Билеты с QR-кодами
                • Напоминания о событиях
                • Изменения в расписании
                • Отмены событий

                По вопросам: support@aqstream.ru
                """;

        messageSender.sendMessage(chatId, helpMessage);
        log.info("Отправлена справка: chatId={}", chatId);
    }

    /**
     * Отправляет приветственное сообщение.
     */
    private void sendWelcomeMessage(Long chatId, User from) {
        String firstName = from != null ? from.firstName() : "пользователь";

        String welcomeMessage = String.format("""
                Привет, *%s*! 👋

                Я бот платформы *AqStream* для управления мероприятиями.

                Я буду отправлять вам:
                • 🎫 Билеты на события
                • ⏰ Напоминания
                • 📋 Изменения и обновления

                Чтобы получать уведомления, привяжите Telegram к вашему аккаунту на сайте.

                /help — справка по боту
                """, escapeMarkdown(firstName));

        messageSender.sendMessage(chatId, welcomeMessage);
        log.info("Отправлено приветствие: chatId={}, user={}",
                chatId, from != null ? from.id() : "unknown");
    }

    /**
     * Извлекает параметр из команды /start.
     *
     * @param text полный текст команды
     * @return параметр или null если отсутствует
     */
    private String extractStartParam(String text) {
        if (text == null || !text.startsWith("/start")) {
            return null;
        }

        String[] parts = text.split("\\s+", 2);
        if (parts.length > 1 && !parts[1].isBlank()) {
            return parts[1].trim();
        }

        return null;
    }

    /**
     * Экранирует специальные символы legacy Markdown (ParseMode.Markdown).
     * В legacy Markdown экранируются только: _ * [ ` \
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        // Экранируем только символы legacy Markdown
        return text
                .replace("\\", "\\\\")  // сначала экранируем backslash
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }
}
