package ru.aqstream.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.notification.api.dto.NotificationChannel;
import ru.aqstream.notification.api.dto.NotificationStatus;
import ru.aqstream.notification.db.entity.NotificationLog;
import ru.aqstream.notification.db.repository.NotificationLogRepository;
import ru.aqstream.notification.telegram.TelegramMessageSender;
import ru.aqstream.notification.template.TemplateService;
import ru.aqstream.user.api.dto.UserTelegramInfoDto;
import ru.aqstream.user.client.UserClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Основной сервис для отправки уведомлений.
 *
 * <p>Поддерживает Telegram (основной канал) и Email (для аутентификации).
 * Логирует все отправки, учитывает настройки пользователя.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TemplateService templateService;
    private final TelegramMessageSender telegramSender;
    private final NotificationLogRepository logRepository;
    private final PreferenceService preferenceService;
    private final UserClient userClient;
    private final EmailService emailService;

    /**
     * Отправляет уведомление через Telegram.
     *
     * @param userId       ID получателя
     * @param templateCode код шаблона
     * @param variables    переменные для подстановки
     * @return true если уведомление отправлено успешно
     */
    @Transactional
    public boolean sendTelegram(UUID userId, String templateCode, Map<String, Object> variables) {
        return sendTelegram(userId, templateCode, variables, null);
    }

    /**
     * Отправляет уведомление через Telegram с проверкой настроек.
     *
     * @param userId       ID получателя
     * @param templateCode код шаблона
     * @param variables    переменные для подстановки
     * @param settingKey   ключ настройки для проверки (null = отправить без проверки)
     * @return true если уведомление отправлено успешно
     */
    @Transactional
    public boolean sendTelegram(UUID userId, String templateCode, Map<String, Object> variables, String settingKey) {
        // Проверяем настройки пользователя
        if (settingKey != null && !preferenceService.isNotificationEnabled(userId, settingKey)) {
            log.debug("Уведомление отключено пользователем: userId={}, template={}, setting={}",
                userId, templateCode, settingKey);
            return false;
        }

        // Получаем Telegram info пользователя
        Optional<UserTelegramInfoDto> telegramInfoOpt = userClient.findTelegramInfo(userId);
        if (telegramInfoOpt.isEmpty() || !telegramInfoOpt.get().hasTelegram()) {
            log.debug("Пользователь не привязал Telegram: userId={}", userId);
            return false;
        }

        UserTelegramInfoDto telegramInfo = telegramInfoOpt.get();
        String chatId = telegramInfo.telegramChatId();

        // Рендерим шаблон
        String body = templateService.render(templateCode, NotificationChannel.TELEGRAM, variables);

        // Создаём запись лога
        NotificationLog logEntry = NotificationLog.createTelegram(userId, templateCode, chatId, body);
        logEntry = logRepository.save(logEntry);

        // Отправляем
        boolean success = telegramSender.sendMessage(Long.parseLong(chatId), body);

        // Обновляем статус
        if (success) {
            logEntry.markAsSent();
            log.info("Telegram уведомление отправлено: userId={}, template={}", userId, templateCode);
        } else {
            logEntry.markAsFailed("Ошибка отправки через Telegram API");
            log.warn("Не удалось отправить Telegram уведомление: userId={}, template={}", userId, templateCode);
        }
        logRepository.save(logEntry);

        return success;
    }

    /**
     * Отправляет уведомление с изображением через Telegram.
     *
     * @param userId       ID получателя
     * @param templateCode код шаблона (для подписи)
     * @param variables    переменные для подстановки
     * @param image        байты изображения
     * @return true если уведомление отправлено успешно
     */
    @Transactional
    public boolean sendTelegramWithImage(UUID userId, String templateCode,
                                         Map<String, Object> variables, byte[] image) {
        return sendTelegramWithImage(userId, templateCode, variables, image, null);
    }

    /**
     * Отправляет уведомление с изображением через Telegram.
     *
     * @param userId       ID получателя
     * @param templateCode код шаблона (для подписи)
     * @param variables    переменные для подстановки
     * @param image        байты изображения
     * @param settingKey   ключ настройки для проверки
     * @return true если уведомление отправлено успешно
     */
    @Transactional
    public boolean sendTelegramWithImage(UUID userId, String templateCode,
                                         Map<String, Object> variables, byte[] image, String settingKey) {
        // Проверяем настройки пользователя
        if (settingKey != null && !preferenceService.isNotificationEnabled(userId, settingKey)) {
            log.debug("Уведомление отключено пользователем: userId={}, template={}", userId, templateCode);
            return false;
        }

        // Получаем Telegram info
        Optional<UserTelegramInfoDto> telegramInfoOpt = userClient.findTelegramInfo(userId);
        if (telegramInfoOpt.isEmpty() || !telegramInfoOpt.get().hasTelegram()) {
            log.debug("Пользователь не привязал Telegram: userId={}", userId);
            return false;
        }

        UserTelegramInfoDto telegramInfo = telegramInfoOpt.get();
        String chatId = telegramInfo.telegramChatId();

        // Рендерим подпись
        String caption = templateService.render(templateCode, NotificationChannel.TELEGRAM, variables);

        // Создаём запись лога
        NotificationLog logEntry = NotificationLog.createTelegram(userId, templateCode, chatId, caption);
        logEntry = logRepository.save(logEntry);

        // Отправляем
        boolean success = telegramSender.sendPhoto(Long.parseLong(chatId), image, caption);

        // Обновляем статус
        if (success) {
            logEntry.markAsSent();
            log.info("Telegram уведомление с изображением отправлено: userId={}, template={}", userId, templateCode);
        } else {
            logEntry.markAsFailed("Ошибка отправки через Telegram API");
            log.warn("Не удалось отправить Telegram уведомление с изображением: userId={}, template={}",
                userId, templateCode);
        }
        logRepository.save(logEntry);

        return success;
    }

    /**
     * Отправляет email уведомление.
     * Используется только для аутентификации (верификация email, сброс пароля).
     *
     * @param userId       ID получателя
     * @param email        email адрес
     * @param templateCode код шаблона
     * @param variables    переменные для подстановки
     * @return true если уведомление отправлено успешно
     */
    @Transactional
    public boolean sendEmail(UUID userId, String email, String templateCode, Map<String, Object> variables) {
        // Рендерим шаблон
        String subject = templateService.renderSubject(templateCode, variables);
        String body = templateService.render(templateCode, NotificationChannel.EMAIL, variables);

        // Создаём запись лога
        NotificationLog logEntry = NotificationLog.createEmail(userId, templateCode, email, subject, body);
        logEntry = logRepository.save(logEntry);

        // Отправляем
        boolean success = emailService.send(email, subject, body);

        // Обновляем статус
        if (success) {
            logEntry.markAsSent();
            log.info("Email отправлен: userId={}, template={}", userId, templateCode);
        } else {
            logEntry.markAsFailed("Ошибка отправки email");
            log.warn("Не удалось отправить email: userId={}, template={}", userId, templateCode);
        }
        logRepository.save(logEntry);

        return success;
    }

    /**
     * Проверяет, разрешено ли отправлять уведомление данного типа.
     *
     * @param userId     ID пользователя
     * @param settingKey ключ настройки
     * @return true если разрешено
     */
    public boolean shouldNotify(UUID userId, String settingKey) {
        return preferenceService.isNotificationEnabled(userId, settingKey);
    }

    /**
     * Возвращает количество ожидающих отправки уведомлений.
     *
     * @return количество pending уведомлений
     */
    public long getPendingCount() {
        return logRepository.countByStatus(NotificationStatus.PENDING);
    }

    /**
     * Возвращает количество неуспешных уведомлений.
     *
     * @return количество failed уведомлений
     */
    public long getFailedCount() {
        return logRepository.countByStatus(NotificationStatus.FAILED);
    }
}
