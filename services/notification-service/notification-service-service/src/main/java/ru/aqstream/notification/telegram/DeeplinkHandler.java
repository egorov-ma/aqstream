package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.model.User;
import feign.FeignException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.client.EventClient;
import ru.aqstream.user.api.dto.AcceptInviteByTelegramRequest;
import ru.aqstream.user.api.dto.LinkTelegramByTokenRequest;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.client.UserClient;

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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
        .ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
        .withZone(ZoneId.of("Europe/Moscow"));

    private final TelegramMessageSender messageSender;
    private final UserClient userClient;
    private final EventClient eventClient;

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

        Optional<UserDto> userOpt = findUserByTelegramId(chatId, from,
            () -> sendInviteNotRegisteredMessage(chatId));
        if (userOpt.isEmpty()) {
            return;
        }

        UserDto user = userOpt.get();
        acceptInviteForUser(chatId, inviteCode, user);
    }

    /**
     * Принимает приглашение для пользователя.
     */
    private void acceptInviteForUser(Long chatId, String inviteCode, UserDto user) {
        try {
            AcceptInviteByTelegramRequest request = new AcceptInviteByTelegramRequest(
                user.id(),
                inviteCode
            );
            OrganizationMemberDto member = userClient.acceptInviteByTelegram(request);
            sendInviteSuccessMessage(chatId, member);
            log.info("Пользователь присоединился к организации: userId={}, role={}",
                user.id(), member.role());
        } catch (FeignException.NotFound e) {
            log.info("Приглашение не найдено: inviteCode={}", maskCode(inviteCode));
            sendErrorMessage(chatId, "Приглашение не найдено или недействительно.");
        } catch (FeignException.Conflict e) {
            log.info("Конфликт при принятии приглашения: userId={}, error={}", user.id(), e.getMessage());
            sendErrorMessage(chatId, "Не удалось принять приглашение. "
                + "Возможно, вы уже являетесь членом организации или приглашение недействительно.");
        } catch (FeignException e) {
            log.error("Ошибка при принятии приглашения: userId={}, error={}", user.id(), e.getMessage());
            sendErrorMessage(chatId, "Произошла ошибка. Попробуйте позже.");
        }
    }

    /**
     * Отправляет сообщение об успешном принятии приглашения.
     */
    private void sendInviteSuccessMessage(Long chatId, OrganizationMemberDto member) {
        String successMessage = String.format("""
            ✅ *Добро пожаловать!*

            Вы успешно присоединились к организации!
            Ваша роль: %s

            Теперь вы будете получать уведомления о событиях организации.
            """, formatRole(member.role().name()));
        messageSender.sendMessage(chatId, successMessage);
    }

    /**
     * Отправляет сообщение о необходимости регистрации для принятия приглашения.
     */
    private void sendInviteNotRegisteredMessage(Long chatId) {
        String message = """
            📨 *Приглашение в организацию*

            Чтобы принять приглашение, необходимо сначала войти в систему через Telegram.

            1. Перейдите на сайт
            2. Нажмите «Войти через Telegram»
            3. После входа повторите переход по ссылке приглашения

            Код приглашения сохранён.
            """;
        messageSender.sendMessage(chatId, message);
    }

    /**
     * Отправляет сообщение об ошибке.
     */
    private void sendErrorMessage(Long chatId, String errorText) {
        String message = String.format("""
            ❌ *Ошибка*

            %s
            """, errorText);
        messageSender.sendMessage(chatId, message);
    }

    /**
     * Форматирует роль для отображения.
     */
    private String formatRole(String role) {
        return switch (role) {
            case "OWNER" -> "Владелец";
            case "MODERATOR" -> "Модератор";
            case "MEMBER" -> "Участник";
            default -> role;
        };
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
        log.info("Обработка привязки аккаунта: chatId={}, token={}", chatId, maskCode(linkToken));

        if (from == null || from.id() == null) {
            log.warn("Не удалось получить информацию о пользователе Telegram");
            sendErrorMessage(chatId, "Не удалось получить информацию о вашем Telegram аккаунте.");
            return;
        }

        Long telegramId = from.id();

        try {
            LinkTelegramByTokenRequest request = new LinkTelegramByTokenRequest(
                linkToken,
                telegramId,
                chatId
            );
            userClient.linkTelegramByToken(request);

            String successMessage = """
                ✅ *Telegram привязан!*

                Ваш Telegram аккаунт успешно привязан к аккаунту на сайте.

                Теперь вы будете получать уведомления о событиях в этот чат.
                """;

            messageSender.sendMessage(chatId, successMessage);
            log.info("Telegram успешно привязан: chatId={}, telegramId={}", chatId, telegramId);

        } catch (FeignException.NotFound e) {
            log.info("Токен привязки не найден или недействителен: token={}", maskCode(linkToken));
            sendLinkNotFoundMessage(chatId);
        } catch (FeignException.Conflict e) {
            log.info("Telegram уже привязан к другому аккаунту: telegramId={}", telegramId);
            sendErrorMessage(chatId, "Этот Telegram аккаунт уже привязан к другому пользователю. "
                + "Если это ваш аккаунт, сначала отвяжите Telegram в настройках профиля.");
        } catch (FeignException e) {
            log.error("Ошибка при привязке Telegram: telegramId={}, error={}", telegramId, e.getMessage());
            sendErrorMessage(chatId, "Произошла ошибка. Попробуйте позже.");
        }
    }

    /**
     * Отправляет сообщение о недействительном токене привязки.
     */
    private void sendLinkNotFoundMessage(Long chatId) {
        String message = """
            ⚠️ *Токен недействителен*

            Токен привязки не найден или истёк.

            Для привязки Telegram к вашему аккаунту:
            1. Войдите на сайт
            2. Перейдите в настройки профиля
            3. Нажмите «Привязать Telegram» для получения новой ссылки

            Ссылка действительна 15 минут.
            """;
        messageSender.sendMessage(chatId, message);
    }

    /**
     * Обработка просмотра регистрации.
     * /start reg_{registrationId}
     *
     * @param chatId         ID чата
     * @param registrationId ID регистрации (UUID в строковом формате)
     * @param from           информация о пользователе
     */
    public void handleRegistration(Long chatId, String registrationId, User from) {
        log.info("Обработка просмотра регистрации: chatId={}, registrationId={}", chatId, registrationId);

        UUID regId = parseRegistrationId(chatId, registrationId);
        if (regId == null) {
            return;
        }

        Optional<UserDto> userOpt = findUserByTelegramId(chatId, from,
            () -> sendRegistrationAuthRequiredMessage(chatId));
        if (userOpt.isEmpty()) {
            return;
        }

        UserDto user = userOpt.get();
        showRegistrationForUser(chatId, regId, user);
    }

    /**
     * Парсит ID регистрации из строки.
     */
    private UUID parseRegistrationId(Long chatId, String registrationId) {
        try {
            return UUID.fromString(registrationId);
        } catch (IllegalArgumentException e) {
            log.info("Невалидный формат registrationId: {}", registrationId);
            sendErrorMessage(chatId, "Неверный формат идентификатора регистрации.");
            return null;
        }
    }

    /**
     * Показывает информацию о регистрации пользователю.
     */
    private void showRegistrationForUser(Long chatId, UUID regId, UserDto user) {
        Optional<RegistrationDto> registrationOpt = fetchRegistration(chatId, regId);
        if (registrationOpt.isEmpty()) {
            return;
        }

        RegistrationDto registration = registrationOpt.get();

        if (!user.id().equals(registration.userId())) {
            log.warn("Попытка доступа к чужой регистрации: userId={}, regUserId={}, regId={}",
                user.id(), registration.userId(), regId);
            sendErrorMessage(chatId, "У вас нет доступа к этой регистрации.");
            return;
        }

        if (registration.status() == RegistrationStatus.CANCELLED) {
            log.info("Регистрация отменена: regId={}", regId);
            sendRegistrationCancelledMessage(chatId, registration);
            return;
        }

        sendTicketMessage(chatId, registration);
        log.info("Отправлена информация о билете: chatId={}, regId={}", chatId, regId);
    }

    /**
     * Получает регистрацию по ID.
     */
    private Optional<RegistrationDto> fetchRegistration(Long chatId, UUID regId) {
        try {
            Optional<RegistrationDto> registrationOpt = eventClient.findRegistrationById(regId);
            if (registrationOpt.isEmpty()) {
                log.info("Регистрация не найдена: regId={}", regId);
                sendErrorMessage(chatId, "Регистрация не найдена.");
            }
            return registrationOpt;
        } catch (FeignException e) {
            log.error("Ошибка при получении регистрации: regId={}, error={}", regId, e.getMessage());
            sendErrorMessage(chatId, "Произошла ошибка. Попробуйте позже.");
            return Optional.empty();
        }
    }

    /**
     * Отправляет сообщение о необходимости авторизации для просмотра регистрации.
     */
    private void sendRegistrationAuthRequiredMessage(Long chatId) {
        String message = """
            🎫 *Просмотр билета*

            Для просмотра билета необходимо войти в систему через Telegram.

            1. Перейдите на сайт
            2. Нажмите «Войти через Telegram»
            3. После входа повторите переход по ссылке
            """;
        messageSender.sendMessage(chatId, message);
    }

    /**
     * Отправляет сообщение об отменённой регистрации.
     */
    private void sendRegistrationCancelledMessage(Long chatId, RegistrationDto registration) {
        String message = String.format("""
            ❌ *Регистрация отменена*

            Событие: %s
            Причина: %s

            Если вы хотите снова зарегистрироваться, перейдите на страницу события.
            """,
            escapeMarkdown(registration.eventTitle()),
            registration.cancellationReason() != null
                ? escapeMarkdown(registration.cancellationReason())
                : "не указана"
        );
        messageSender.sendMessage(chatId, message);
    }

    /**
     * Отправляет сообщение с информацией о билете.
     */
    private void sendTicketMessage(Long chatId, RegistrationDto registration) {
        String formattedDate = registration.eventStartsAt() != null
            ? DATE_FORMATTER.format(registration.eventStartsAt())
            : "дата не указана";

        String statusText = formatRegistrationStatus(registration.status());

        String message = String.format("""
            🎫 *Ваш билет*

            *Событие:* %s
            *Дата:* %s
            *Тип билета:* %s
            *Статус:* %s

            *Участник:* %s %s
            *Код подтверждения:* `%s`

            Покажите этот код или QR-код на входе.
            """,
            escapeMarkdown(registration.eventTitle()),
            formattedDate,
            escapeMarkdown(registration.ticketTypeName() != null ? registration.ticketTypeName() : "Стандартный"),
            statusText,
            escapeMarkdown(registration.firstName() != null ? registration.firstName() : ""),
            escapeMarkdown(registration.lastName() != null ? registration.lastName() : ""),
            registration.confirmationCode()
        );

        messageSender.sendMessage(chatId, message);
    }

    /**
     * Форматирует статус регистрации для отображения.
     */
    private String formatRegistrationStatus(RegistrationStatus status) {
        if (status == null) {
            return "неизвестен";
        }
        return switch (status) {
            case CONFIRMED -> "✅ Подтверждена";
            case CANCELLED -> "❌ Отменена";
            case RESERVED -> "🕐 Ожидает оплаты";
            case PENDING -> "⏳ Обработка платежа";
            case EXPIRED -> "⌛ Истекла";
        };
    }

    /**
     * Экранирует специальные символы Markdown.
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`");
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

    /**
     * Ищет пользователя по Telegram ID.
     *
     * @param chatId            ID чата для отправки ошибки
     * @param from              информация о пользователе Telegram
     * @param onNotRegistered   действие при отсутствии регистрации
     * @return Optional с пользователем или empty при ошибке
     */
    private Optional<UserDto> findUserByTelegramId(Long chatId, User from, Runnable onNotRegistered) {
        if (from == null || from.id() == null) {
            log.warn("Не удалось получить информацию о пользователе Telegram");
            onNotRegistered.run();
            return Optional.empty();
        }

        String telegramId = String.valueOf(from.id());

        Optional<UserDto> userOpt;
        try {
            userOpt = userClient.findByTelegramId(telegramId);
        } catch (FeignException e) {
            log.error("Ошибка при поиске пользователя по Telegram ID: telegramId={}, error={}",
                telegramId, e.getMessage());
            sendErrorMessage(chatId, "Произошла ошибка. Попробуйте позже.");
            return Optional.empty();
        }

        if (userOpt.isEmpty()) {
            log.info("Пользователь не найден по Telegram ID: telegramId={}", telegramId);
            onNotRegistered.run();
            return Optional.empty();
        }

        return userOpt;
    }
}
