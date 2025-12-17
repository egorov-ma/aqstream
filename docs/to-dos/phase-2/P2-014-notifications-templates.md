# P2-014 Шаблоны уведомлений и отправка

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
| Приоритет | `high` |
| Связь с roadmap | [Roadmap - Уведомления](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Уведомления информируют участников о событиях: подтверждение регистрации с билетом, напоминания, изменения. Шаблоны позволяют стандартизировать сообщения и упрощают локализацию.

### Технический контекст

- Notification Service слушает события из RabbitMQ
- Шаблоны на Mustache + Markdown
- Отправка через Telegram Bot API
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

- [ ] Системные шаблоны хранятся в БД (NotificationTemplate)
- [ ] Шаблоны используют Mustache синтаксис
- [ ] Поддержка Markdown в теле сообщения
- [ ] Список переменных для каждого шаблона

### Phase 2 шаблоны

- [ ] `user.welcome` — приветствие после регистрации
- [ ] `registration.confirmed` — билет с QR-кодом
- [ ] `registration.cancelled` — отмена регистрации
- [ ] `event.reminder` — напоминание о событии (за 24ч)
- [ ] `event.changed` — изменение даты/места
- [ ] `event.cancelled` — отмена события

### Event Listeners

- [ ] `UserRegisteredEvent` → `user.welcome`
- [ ] `RegistrationCreatedEvent` → `registration.confirmed` + билет с QR
- [ ] `RegistrationCancelledEvent` → `registration.cancelled`
- [ ] `EventChangedEvent` → `event.changed` (всем участникам)
- [ ] `EventCancelledEvent` → `event.cancelled` (всем участникам)

### Напоминания

- [ ] Scheduler для отправки напоминаний
- [ ] Напоминание за 24 часа до события
- [ ] Не отправлять напоминание если событие отменено

### Логирование

- [ ] Все отправки логируются в NotificationLog
- [ ] Статусы: PENDING, SENT, FAILED
- [ ] При ошибке — сохраняется error_message
- [ ] Retry механизм при временных ошибках (3 попытки)

### Настройки пользователя

- [ ] Пользователь может отключить определённые типы уведомлений
- [ ] API для получения и обновления настроек
- [ ] Проверка настроек перед отправкой

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Код написан согласно code style проекта
- [ ] Unit тесты для template rendering
- [ ] Integration тесты (RabbitMQ → Notification)
- [ ] Миграции для шаблонов (seed data)
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

- [P2-011](./P2-011-registrations-crud.md) Регистрации (события)
- [P2-012](./P2-012-registrations-qr-code.md) QR-код
- [P2-013](./P2-013-notifications-telegram-bot.md) Telegram Bot

## Out of Scope

- Кастомные шаблоны организаций
- Email уведомления (только для verification/reset)
- Push notifications
- Массовые рассылки от организатора

## Заметки

- При массовой отправке (отмена события) использовать batch с задержкой (rate limiting)
- Timezone события нужно учитывать при форматировании даты
- Markdown в Telegram ограничен — использовать только базовое форматирование
- Логи хранить 30 дней, затем архивировать/удалять
