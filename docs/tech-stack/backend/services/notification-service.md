# Notification Service

Notification Service отвечает за отправку уведомлений пользователям через Telegram.

## Обзор

| Параметр | Значение |
|----------|----------|
| Порт | 8084 |
| База данных | postgres-shared |
| Схема | notification_service |

## Ответственности

- Telegram уведомления (Bot API) — единственный канал
- Шаблоны сообщений (Mustache + Markdown)
- Очередь отправки
- Логирование отправок

## Каналы

| Канал | Технология |
|-------|-----------|
| Telegram | Telegram Bot API |

**Важно:** Telegram — единственный канал уведомлений. Email используется только для альтернативной аутентификации, но не для уведомлений.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/notifications/send` | Отправка (internal) |
| GET | `/api/v1/notifications/templates` | Список шаблонов |
| GET | `/api/v1/notifications/preferences` | Настройки пользователя |
| PUT | `/api/v1/notifications/preferences` | Обновить настройки |

## Шаблоны

```java
public enum NotificationTemplate {
    USER_WELCOME("user.welcome"),
    REGISTRATION_CONFIRMED("registration.confirmed"),
    REGISTRATION_CANCELLED("registration.cancelled"),
    RESERVATION_EXPIRED("reservation.expired"),
    EVENT_REMINDER("event.reminder"),
    EVENT_CHANGED("event.changed"),
    EVENT_CANCELLED("event.cancelled"),
    WAITLIST_AVAILABLE("waitlist.available"),
    PAYMENT_RECEIPT("payment.receipt");
}
```

### Telegram шаблон (Mustache + Markdown)

```markdown
<!-- templates/telegram/registration.confirmed.md -->
🎫 *Билет на событие*

Привет, {{firstName}}!

Вы успешно зарегистрировались на *{{eventTitle}}*.

📋 *Детали:*
• Код билета: `{{confirmationCode}}`
• Дата: {{eventDate}}
• Место: {{eventLocation}}

[Подробнее о событии]({{eventUrl}})
```

## Отправка уведомлений

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TelegramSender telegramSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository logRepository;

    public void send(SendNotificationRequest request) {
        // Рендеринг шаблона
        String body = templateEngine.render(
            request.template().getBodyTemplate(),
            request.variables()
        );

        // Отправка в Telegram
        if (request.telegramChatId() != null) {
            sendTelegram(request.telegramChatId(), body);
        } else {
            log.warn("Не удалось отправить уведомление: пользователь не подключил Telegram, userId={}",
                request.userId());
        }
    }

    private void sendTelegram(String chatId, String body) {
        try {
            telegramSender.send(chatId, body);
            logSuccess(chatId, Channel.TELEGRAM);
        } catch (Exception e) {
            logFailure(chatId, Channel.TELEGRAM, e.getMessage());
            throw new NotificationFailedException(e);
        }
    }
}
```

## События (RabbitMQ)

### Потребляемые

| Event | Уведомление |
|-------|-------------|
| `user.registered` | Welcome сообщение в Telegram |
| `registration.created` | Билет с QR-кодом в Telegram |
| `registration.cancelled` | Уведомление об отмене |
| `reservation.expired` | Уведомление об истечении брони |
| `event.cancelled` | Уведомление об отмене события всем участникам |
| `event.changed` | Уведомление об изменениях |
| `event.reminder` | Напоминание о событии (за 24ч) |
| `payment.completed` | Чек об оплате |
| `waitlist.available` | Место из листа ожидания |

```java
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserClient userClient;

    @RabbitListener(queues = "notifications.registration.created")
    public void handleRegistrationCreated(RegistrationCreatedEvent event) {
        // Получаем telegram_chat_id пользователя
        UserDto user = userClient.findById(event.getUserId());

        notificationService.send(SendNotificationRequest.builder()
            .template(NotificationTemplate.REGISTRATION_CONFIRMED)
            .userId(event.getUserId())
            .telegramChatId(user.getTelegramChatId())
            .variables(Map.of(
                "firstName", event.getFirstName(),
                "eventTitle", event.getEventTitle(),
                "confirmationCode", event.getConfirmationCode(),
                "eventDate", formatDate(event.getEventStartsAt()),
                "eventLocation", event.getEventLocation(),
                "eventUrl", generateEventUrl(event.getEventId())
            ))
            .build());
    }
}
```

## Telegram Bot

```java
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramBot bot;

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        message.parseMode(ParseMode.HTML);
        bot.execute(message);
    }

    // Команда /start — привязка аккаунта
    public void handleStart(Update update) {
        Long chatId = update.message().chat().id();
        String token = extractToken(update.message().text());
        
        // Привязываем chatId к пользователю
        userService.linkTelegram(token, chatId);
        
        sendMessage(chatId, "Telegram успешно подключен!");
    }
}
```

## Конфигурация

```yaml
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  bot-username: ${TELEGRAM_BOT_USERNAME}
```

## Дальнейшее чтение

- [Service Topology](../../../architecture/service-topology.md)
