# Notification Service

Notification Service отвечает за отправку уведомлений пользователям.

## Обзор

| Параметр | Значение |
|----------|----------|
| Порт | 8084 |
| База данных | postgres-shared |
| Схема | notification_service |

## Ответственности

- Email уведомления (SMTP)
- Telegram уведомления (Bot API)
- Шаблоны сообщений
- Логирование отправок

## Каналы

| Канал | Технология |
|-------|-----------|
| Email | Spring Mail + SMTP |
| Telegram | Telegram Bot API |

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
    REGISTRATION_CONFIRMED("registration.confirmed"),
    REGISTRATION_CANCELLED("registration.cancelled"),
    EVENT_REMINDER("event.reminder"),
    EVENT_CHANGED("event.changed"),
    EVENT_CANCELLED("event.cancelled"),
    WAITLIST_AVAILABLE("waitlist.available"),
    PASSWORD_RESET("password.reset"),
    EMAIL_VERIFICATION("email.verification");
}
```

### Email шаблон (Mustache)

```html
<!-- templates/email/registration.confirmed.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Регистрация подтверждена</title>
</head>
<body>
    <h1>Привет, {{firstName}}!</h1>
    <p>Вы успешно зарегистрировались на событие <strong>{{eventTitle}}</strong>.</p>
    
    <div class="ticket">
        <p>Код билета: <strong>{{confirmationCode}}</strong></p>
        <p>Дата: {{eventDate}}</p>
        <p>Место: {{eventLocation}}</p>
    </div>
    
    <img src="{{qrCodeUrl}}" alt="QR код билета" />
</body>
</html>
```

## Отправка уведомлений

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailSender emailSender;
    private final TelegramSender telegramSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository logRepository;

    public void send(SendNotificationRequest request) {
        // Рендеринг шаблона
        String subject = templateEngine.render(
            request.template().getSubjectTemplate(),
            request.variables()
        );
        String body = templateEngine.render(
            request.template().getBodyTemplate(),
            request.variables()
        );
        
        // Отправка по каналам
        if (request.channels().contains(Channel.EMAIL)) {
            sendEmail(request.email(), subject, body);
        }
        
        if (request.channels().contains(Channel.TELEGRAM) && request.telegramChatId() != null) {
            sendTelegram(request.telegramChatId(), body);
        }
    }

    private void sendEmail(String email, String subject, String body) {
        try {
            emailSender.send(email, subject, body);
            logSuccess(email, Channel.EMAIL);
        } catch (Exception e) {
            logFailure(email, Channel.EMAIL, e.getMessage());
            throw new NotificationFailedException(e);
        }
    }
}
```

## События (RabbitMQ)

### Потребляемые

| Event | Уведомление |
|-------|-------------|
| `user.registered` | Email verification |
| `registration.created` | Confirmation email |
| `registration.cancelled` | Cancellation notice |
| `event.published` | — |
| `event.cancelled` | Cancellation notice to all |
| `event.changed` | Update notice |
| `payment.completed` | Receipt |
| `waitlist.available` | Spot available |

```java
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notifications.registration.created")
    public void handleRegistrationCreated(RegistrationCreatedEvent event) {
        notificationService.send(SendNotificationRequest.builder()
            .template(NotificationTemplate.REGISTRATION_CONFIRMED)
            .email(event.getEmail())
            .channels(Set.of(Channel.EMAIL))
            .variables(Map.of(
                "firstName", event.getFirstName(),
                "eventTitle", event.getEventTitle(),
                "confirmationCode", event.getConfirmationCode(),
                "eventDate", formatDate(event.getEventStartsAt()),
                "qrCodeUrl", generateQrCodeUrl(event.getConfirmationCode())
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
spring:
  mail:
    host: ${SMTP_HOST:smtp.gmail.com}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  bot-username: ${TELEGRAM_BOT_USERNAME}
```

## Дальнейшее чтение

- [Service Topology](../../../architecture/service-topology.md)
