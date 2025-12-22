package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Обработчик deeplink параметров в команде /start.
 *
 * Поддерживаемые deeplinks:
 * - /start invite_{code} — приглашение в организацию
 * - /start link_{token} — привязка Telegram к email-аккаунту
 * - /start reg_{id} — просмотр регистрации
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeeplinkHandler {

    private final TelegramMessageSender messageSender;

    /**
     * Обработка приглашения в организацию.
     * /start invite_{inviteCode}
     *
     * @param chatId     ID чата
     * @param inviteCode код приглашения
     * @param from       информация о пользователе
     */
    public void handleInvite(Long chatId, String inviteCode, User from) {
        log.info("Обработка приглашения: chatId={}, inviteCode={}", chatId, maskCode(inviteCode));

        // TODO: Backlog - интеграция с OrganizationService через internal API
        // Требуется: POST /api/v1/internal/organizations/join-by-telegram
        // 1. Найти пользователя по telegram_id (from.id())
        // 2. Если не найден — предложить войти через Telegram Login Widget
        // 3. Проверить валидность invite code
        // 4. Добавить пользователя в организацию

        String message = """
                📨 *Приглашение в организацию*

                Вы получили приглашение вступить в организацию!

                Для принятия приглашения:
                1. Войдите на сайт через Telegram
                2. Перейдите по той же ссылке приглашения

                Код приглашения сохранён.
                """;

        messageSender.sendMessage(chatId, message);
        log.info("Отправлена информация о приглашении: chatId={}", chatId);
    }

    /**
     * Обработка привязки Telegram к email-аккаунту.
     * /start link_{linkToken}
     *
     * @param chatId    ID чата
     * @param linkToken токен привязки
     * @param from      информация о пользователе
     */
    public void handleLink(Long chatId, String linkToken, User from) {
        Long telegramId = from != null ? from.id() : null;
        log.info("Обработка привязки аккаунта: chatId={}, telegramId={}",
                chatId, telegramId != null ? telegramId : "unknown");

        // TODO: Backlog - интеграция с UserService через internal API
        // Требуется: POST /api/v1/internal/users/link-telegram-by-token
        // Body: { "linkToken": "...", "telegramId": "...", "telegramChatId": "..." }
        // 1. Проверить валидность link token (не истёк, существует)
        // 2. Получить userId из токена
        // 3. Обновить telegram_id и telegram_chat_id в User
        // 4. Пометить токен как использованный
        // 5. Отправить подтверждение

        String message = """
                🔗 *Привязка Telegram*

                Для привязки Telegram к вашему аккаунту:
                1. Войдите на сайт
                2. Перейдите в настройки профиля
                3. Нажмите «Привязать Telegram»

                После привязки вы будете получать уведомления в этот чат.
                """;

        messageSender.sendMessage(chatId, message);
        log.info("Отправлена информация о привязке: chatId={}", chatId);
    }

    /**
     * Обработка просмотра регистрации.
     * /start reg_{registrationId}
     *
     * @param chatId         ID чата
     * @param registrationId ID регистрации
     * @param from           информация о пользователе
     */
    public void handleRegistration(Long chatId, String registrationId, User from) {
        Long telegramId = from != null ? from.id() : null;
        log.info("Обработка просмотра регистрации: chatId={}, registrationId={}, telegramId={}",
                chatId, registrationId, telegramId != null ? telegramId : "unknown");

        // TODO: P2-014 - интеграция с EventService через EventClient
        // Требуется: GET /api/v1/internal/registrations/{id}/telegram-info?telegramId=...
        // 1. Проверить, принадлежит ли регистрация пользователю (по telegram_id)
        // 2. Получить данные регистрации и события
        // 3. Сгенерировать и отправить билет с QR-кодом

        String message = """
                🎫 *Информация о регистрации*

                Для просмотра билета войдите в личный кабинет на сайте.

                Скоро здесь будет отображаться ваш билет с QR-кодом!
                """;

        messageSender.sendMessage(chatId, message);
        log.info("Отправлена информация о регистрации: chatId={}", chatId);
    }

    /**
     * Маскирует код для логирования.
     */
    private String maskCode(String code) {
        if (code == null || code.length() <= 4) {
            return "***";
        }
        return code.substring(0, 2) + "***" + code.substring(code.length() - 2);
    }
}
