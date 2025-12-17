# P2-013 Telegram Bot интеграция

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Уведомления](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Telegram — единственный канал уведомлений в AqStream. Бот отправляет билеты, напоминания, уведомления об изменениях. Также через бот происходит авторизация и привязка аккаунта.

### Технический контекст

- Notification Service управляет ботом
- Bot API для отправки сообщений
- Webhook или long polling для получения команд
- Deeplinks для приглашений и авторизации

**Связанные документы:**
- [Notification Service](../../tech-stack/backend/services/notification-service.md#telegram-bot)
- [Functional Requirements FR-10](../../business/functional-requirements.md#fr-10-уведомления)
- [User Service](../../tech-stack/backend/services/user-service.md#authentication) — Telegram auth

## Цель

Реализовать Telegram бот для отправки уведомлений, привязки аккаунтов и обработки базовых команд.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### Создание бота и конфигурация

- [ ] Бот создан через @BotFather
- [ ] Настроены commands: /start, /help
- [ ] Webhook или long polling для получения updates
- [ ] Bot Token безопасно хранится в env variables
- [ ] Документация по получению токена от @BotFather обновлена в `docs/operations/environments.md`
- [ ] Тестовый bot token настроен для integration тестов (application-test.yaml)
- [ ] Integration тесты для `POST /api/v1/auth/telegram` написаны (из P2-002)

### Команда /start

- [ ] При `/start` без параметров — welcome сообщение
- [ ] При `/start invite_{code}` — присоединение к организации
- [ ] При `/start link_{token}` — привязка Telegram к существующему email-аккаунту
- [ ] При первом входе через Telegram Login Widget — автоматическая привязка chat_id

### Привязка аккаунта

- [ ] После авторизации через Telegram Login Widget — chat_id сохраняется
- [ ] Пользователь с email-аккаунтом может привязать Telegram через /start link_{token}
- [ ] Генерация link_token через веб-интерфейс (в настройках профиля)
- [ ] Token действителен 15 минут

### Отправка сообщений

- [ ] Отправка текстовых сообщений (Markdown)
- [ ] Отправка изображений (билет с QR)
- [ ] Inline кнопки для быстрых действий
- [ ] Обработка ошибок (blocked bot, chat not found)
- [ ] Retry механизм при временных ошибках

### Команда /help

- [ ] Показывает доступные команды
- [ ] Ссылка на FAQ или поддержку

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Код написан согласно code style проекта
- [ ] Unit тесты написаны
- [ ] Integration тесты (mock Telegram API)
- [ ] Документация конфигурации
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `notification-service` (TelegramBotService)
- [ ] Frontend: кнопка «Привязать Telegram» в настройках
- [x] Database: telegram_chat_id в users, telegram_link_tokens
- [x] Infrastructure: webhook endpoint, env variables

### Telegram Bot Library

```gradle
// java-telegram-bot-api
implementation 'com.github.pengrad:java-telegram-bot-api:6.9.1'
```

### Bot Service

```java
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramBot bot;
    private final UserClient userClient;

    @PostConstruct
    public void init() {
        // Webhook или Long Polling
        bot.setUpdatesListener(updates -> {
            updates.forEach(this::handleUpdate);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void handleUpdate(Update update) {
        if (update.message() != null && update.message().text() != null) {
            String text = update.message().text();
            Long chatId = update.message().chat().id();

            if (text.startsWith("/start")) {
                handleStart(chatId, text);
            } else if (text.equals("/help")) {
                handleHelp(chatId);
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage request = new SendMessage(chatId, text)
            .parseMode(ParseMode.Markdown);
        bot.execute(request);
    }

    public void sendPhoto(Long chatId, byte[] photo, String caption) {
        SendPhoto request = new SendPhoto(chatId, photo)
            .caption(caption)
            .parseMode(ParseMode.Markdown);
        bot.execute(request);
    }
}
```

### Deeplink Format

```
/start invite_{inviteCode}     — приглашение в организацию
/start link_{linkToken}        — привязка email-аккаунта к Telegram
/start reg_{registrationId}    — просмотр регистрации
```

### Configuration

```yaml
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  bot-username: ${TELEGRAM_BOT_USERNAME:AqStreamBot}
  webhook-url: ${TELEGRAM_WEBHOOK_URL:}  # Если пусто — long polling
```

### Error Handling

```java
public void sendMessageSafe(Long chatId, String text) {
    try {
        SendResponse response = bot.execute(new SendMessage(chatId, text));
        if (!response.isOk()) {
            if (response.errorCode() == 403) {
                // Пользователь заблокировал бота
                log.warn("Пользователь заблокировал бота: chatId={}", chatId);
                userClient.clearTelegramChatId(chatId);
            } else {
                log.error("Ошибка отправки: code={}, desc={}",
                    response.errorCode(), response.description());
            }
        }
    } catch (Exception e) {
        log.error("Ошибка Telegram API: chatId={}, error={}", chatId, e.getMessage());
    }
}
```

## Зависимости

### Блокирует

- [P2-014](./P2-014-notifications-templates.md) Шаблоны уведомлений
- [P2-002](./P2-002-auth-telegram.md) Telegram авторизация

### Зависит от

- [P2-001](./P2-001-auth-email-registration.md) User entity

## Out of Scope

- Интерактивные команды (поиск событий, регистрация через бот)
- Групповые чаты
- Inline mode
- Payments через бот

## Заметки

- Для локальной разработки использовать long polling
- Для production — webhook (требует HTTPS)
- ngrok можно использовать для тестирования webhook локально
- Bot username должен заканчиваться на 'bot' или 'Bot'
- Rate limits: не более 30 сообщений в секунду на бота
