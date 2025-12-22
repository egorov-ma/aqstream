# P2-014 Шаблоны уведомлений и отправка

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `review` |
| Приоритет | `high` |
| Связь с roadmap | [Roadmap - Уведомления](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Уведомления информируют участников о событиях: подтверждение регистрации с билетом, напоминания, изменения. Шаблоны позволяют стандартизировать сообщения и упрощают локализацию.

### Технический контекст

- Notification Service слушает события из RabbitMQ
- Шаблоны на Mustache + Markdown
- Отправка через Telegram Bot API (основной канал)
- Email отправка для верификации и сброса пароля (SMTP)
- Логирование всех отправок

**Связанные документы:**
- [Notification Service](../../tech-stack/backend/services/notification-service.md)
- [Domain Model - NotificationTemplate](../../data/domain-model.md#notificationtemplate)
- [Domain Model - NotificationLog](../../data/domain-model.md#notificationlog)

## Цель

Реализовать систему шаблонов уведомлений и автоматическую отправку по событиям из RabbitMQ.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### Шаблоны

- [x] Системные шаблоны хранятся в БД (NotificationTemplate)
- [x] Шаблоны используют Mustache синтаксис
- [x] Поддержка Markdown в теле сообщения
- [x] Список переменных для каждого шаблона

### Phase 2 шаблоны (Telegram)

- [x] `user.welcome` — приветствие после регистрации
- [x] `registration.confirmed` — билет с QR-кодом
- [x] `registration.cancelled` — отмена регистрации
- [x] `event.reminder` — напоминание о событии (за 24ч)
- [x] `event.changed` — изменение даты/места
- [x] `event.cancelled` — отмена события
- [x] `organization.request.approved` — уведомление об одобрении запроса на создание организации
- [x] `organization.request.rejected` — уведомление об отклонении с причиной

### Email шаблоны (аутентификация)

- [x] `auth.email-verification` — подтверждение email при регистрации
- [x] `auth.password-reset` — ссылка для сброса пароля
- [x] SMTP конфигурация (SendGrid/Amazon SES для production, Mailhog для dev)

### Event Listeners (Telegram)

- [ ] `UserRegisteredEvent` → `user.welcome` (событие не реализовано)
- [x] `RegistrationCreatedEvent` → `registration.confirmed` + билет с QR
- [x] `RegistrationCancelledEvent` → `registration.cancelled`
- [ ] `EventChangedEvent` → `event.changed` (требует EventClient для получения участников)
- [ ] `EventCancelledEvent` → `event.cancelled` (требует EventClient для получения участников)
- [x] `OrganizationRequestApprovedEvent` → `organization.request.approved` (уведомление пользователю)
- [x] `OrganizationRequestRejectedEvent` → `organization.request.rejected` (уведомление с причиной)

### Event Listeners (Email)

- [x] `EmailVerificationRequestedEvent` → `auth.email-verification` (из user-service)
- [x] `PasswordResetRequestedEvent` → `auth.password-reset` (из user-service)

### Напоминания

- [x] Scheduler для отправки напоминаний (структура готова)
- [ ] Напоминание за 24 часа до события (требует EventClient)
- [ ] Не отправлять напоминание если событие отменено (требует EventClient)

### Логирование

- [x] Все отправки логируются в NotificationLog
- [x] Статусы: PENDING, SENT, FAILED
- [x] При ошибке — сохраняется error_message
- [x] Retry механизм при временных ошибках (3 попытки)

### Настройки пользователя

- [x] Пользователь может отключить определённые типы уведомлений
- [x] API для получения и обновления настроек
- [x] Проверка настроек перед отправкой

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены (часть AC заблокирована зависимостями — EventClient, UserRegisteredEvent)
- [x] Код написан согласно code style проекта
- [x] Unit тесты для template rendering (TemplateServiceTest)
- [x] Unit тесты для notification service (NotificationServiceTest, TelegramBotServiceTest, PreferenceServiceTest)
- [ ] Integration тесты (RabbitMQ → Notification) — заблокированы инфраструктурой
- [x] Миграции для шаблонов (seed data)
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `notification-service` (все модули)
- [ ] Frontend: настройки уведомлений
- [x] Database: таблицы `notification_templates`, `notification_logs`, `notification_preferences`
- [ ] Infrastructure: —

### Модель данных

```sql
-- Шаблоны
CREATE TABLE notification_service.notification_templates (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL DEFAULT 'TELEGRAM',
    body TEXT NOT NULL, -- Mustache + Markdown
    variables JSONB NOT NULL DEFAULT '{}',
    is_system BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Лог отправок
CREATE TABLE notification_service.notification_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    template_code VARCHAR(50),
    telegram_chat_id VARCHAR(50),
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Настройки пользователя
CREATE TABLE notification_service.notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    settings JSONB NOT NULL DEFAULT '{"event_reminder": true, "registration_updates": true}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Пример шаблона

```mustache
🎫 *Билет на событие*

Привет, {{firstName}}!

Вы успешно зарегистрировались на *{{eventTitle}}*.

📋 *Детали:*
• Код билета: `{{confirmationCode}}`
• Тип: {{ticketTypeName}}
• Дата: {{eventDate}}
• Место: {{eventLocation}}

[Подробнее о событии]({{eventUrl}})
```

### Event Listener

```java
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserClient userClient;

    @RabbitListener(queues = "notifications.registration.created")
    public void handleRegistrationCreated(RegistrationCreatedEvent event) {
        UserDto user = userClient.findById(event.getUserId());

        // Проверяем настройки
        if (!notificationService.shouldNotify(user.getId(), "registration_updates")) {
            return;
        }

        // Генерируем билет с QR
        byte[] ticketImage = ticketImageService.generate(event);

        // Отправляем
        notificationService.sendWithImage(
            SendNotificationRequest.builder()
                .userId(user.getId())
                .telegramChatId(user.getTelegramChatId())
                .templateCode("registration.confirmed")
                .variables(Map.of(
                    "firstName", event.getFirstName(),
                    "eventTitle", event.getEventTitle(),
                    "confirmationCode", event.getConfirmationCode(),
                    "ticketTypeName", event.getTicketTypeName(),
                    "eventDate", formatDate(event.getEventStartsAt()),
                    "eventLocation", event.getEventLocation(),
                    "eventUrl", generateEventUrl(event.getEventId())
                ))
                .image(ticketImage)
                .build()
        );
    }
}
```

### Reminder Scheduler

```java
@Component
@RequiredArgsConstructor
public class EventReminderScheduler {

    @Scheduled(cron = "0 0 * * * *") // каждый час
    public void sendReminders() {
        // Найти события, которые начинаются через 24-25 часов
        Instant from = Instant.now().plus(24, ChronoUnit.HOURS);
        Instant to = from.plus(1, ChronoUnit.HOURS);

        List<Event> events = eventClient.findByStartsAtBetween(from, to);

        for (Event event : events) {
            if (event.getStatus() != EventStatus.PUBLISHED) continue;

            List<Registration> registrations = registrationClient.findByEventId(event.getId());
            for (Registration reg : registrations) {
                if (reg.getStatus() == RegistrationStatus.CONFIRMED) {
                    notificationService.send(/*...*/);
                }
            }
        }
    }
}
```

## Зависимости

### Блокирует

- Нет

### Зависит от

- [P2-004](./P2-004-auth-email-verification.md) Email verification (события для email)
- [P2-005](./P2-005-organizations-requests.md) Organization Requests (события одобрения/отклонения)
- [P2-011](./P2-011-registrations-crud.md) Регистрации (события)
- [P2-012](./P2-012-registrations-qr-code.md) QR-код
- [P2-013](./P2-013-notifications-telegram-bot.md) Telegram Bot

## Out of Scope

- Кастомные шаблоны организаций
- Email уведомления о событиях (напоминания, изменения) — только Telegram
- Push notifications
- Массовые рассылки от организатора

## Заметки

- При массовой отправке (отмена события) использовать batch с задержкой (rate limiting)
- Timezone события нужно учитывать при форматировании даты
- Markdown в Telegram ограничен — использовать только базовое форматирование
- Логи хранить 30 дней, затем архивировать/удалять
