# P2-002 Регистрация и вход через Telegram

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Аутентификация](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Telegram — основной способ аутентификации для AqStream ([FR-1.1.1](../../business/functional-requirements.md)). Большинство пользователей платформы используют Telegram для коммуникации, поэтому вход через него должен быть максимально простым. При регистрации через Telegram аккаунт активируется автоматически без подтверждения email.

### Технический контекст

- Telegram Login Widget для web-аутентификации
- Telegram Bot API для привязки аккаунта
- User Service должен поддерживать оба способа входа (Telegram и email)
- `telegram_id` должен быть уникальным среди активных пользователей

**Связанные документы:**
- [User Service](../../tech-stack/backend/services/user-service.md#authentication) — endpoint `/api/v1/auth/telegram`
- [Notification Service](../../tech-stack/backend/services/notification-service.md#telegram-bot) — Telegram Bot
- [Domain Model - User](../../data/domain-model.md#user) — поле `telegram_id`
- [Functional Requirements](../../business/functional-requirements.md#fr-11-регистрация-пользователя)

## Цель

Реализовать регистрацию и вход пользователей через Telegram с автоматической привязкой аккаунта к Telegram chat для уведомлений.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

- [x] Пользователь может зарегистрироваться через Telegram Login Widget (`POST /api/v1/auth/telegram`)
- [x] При первом входе создаётся новый аккаунт с данными из Telegram (first_name, last_name, photo_url)
- [x] При повторном входе — аутентификация существующего пользователя
- [x] Telegram ID проверяется на уникальность
- [x] Аккаунт активируется автоматически (без email verification)
- [x] При успешном входе возвращается JWT access token и refresh token
- [x] Валидация данных от Telegram (проверка hash согласно [Telegram Login Widget](https://core.telegram.org/widgets/login))
- [x] Сохраняется `telegram_chat_id` для отправки уведомлений
- [x] Пользователь может привязать существующий email-аккаунт к Telegram

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Код написан согласно code style проекта
- [x] Unit тесты написаны и проходят
- [x] Integration тесты написаны → перенесено в [P2-013](./P2-013-notifications-telegram-bot.md)
- [x] Документация API обновлена (OpenAPI аннотации)
- [x] Liquibase миграции созданы (если требуются изменения схемы)
- [x] Code review пройден
- [x] CI/CD pipeline проходит
- [x] Функционал проверен на локальном окружении

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service-service` (TelegramAuthService)
- [ ] Frontend: Telegram Login Widget на странице входа (P2-015)
- [x] Database: колонки `telegram_id`, `telegram_chat_id` в таблице `users`
- [x] Infrastructure: конфигурация Telegram Bot Token

### Подход к реализации

1. **Telegram Login Widget валидация:**
   ```java
   // Проверка hash от Telegram
   public boolean validateTelegramAuth(TelegramAuthData data, String botToken) {
       String dataCheckString = buildDataCheckString(data);
       String secretKey = sha256(botToken);
       String hash = hmacSha256(dataCheckString, secretKey);
       return hash.equals(data.getHash());
   }
   ```

2. **Endpoints:**
   ```
   POST /api/v1/auth/telegram        # Вход/регистрация через Telegram
   Body: { "id": 123456789, "first_name": "...", "hash": "...", "auth_date": ... }
   Response: { "accessToken": "...", "refreshToken": "...", "user": {...} }

   POST /api/v1/auth/telegram/link   # Привязка Telegram к существующему аккаунту (требует JWT)
   Body: { "id": 123456789, "first_name": "...", "hash": "...", "auth_date": ... }
   Response: { "accessToken": "...", "refreshToken": "...", "user": {...} }
   ```

3. **Связывание аккаунтов:**
   - Если пользователь с таким `telegram_id` уже существует — аутентификация
   - Если не существует — создание нового аккаунта
   - `POST /api/v1/auth/telegram/link` — привязка Telegram к существующему email-аккаунту

### Environment Variables

```
TELEGRAM_BOT_TOKEN=your-bot-token
TELEGRAM_BOT_USERNAME=AqStreamBot
```

## Зависимости

### Блокирует

- [P2-013](./P2-013-notifications-telegram-bot.md) Telegram Bot интеграция (нужна авторизация)
- [P2-015](./P2-015-frontend-auth-pages.md) Frontend auth pages (Telegram Login Widget)

### Зависит от

- [P2-001](./P2-001-auth-email-registration.md) Базовая структура User entity и AuthService

## Out of Scope

- Telegram Bot команды (/start, /help) — P2-013
- Telegram notifications — P2-014
- Привязка нескольких Telegram аккаунтов к одному User

## Заметки

- Telegram Login Widget требует HTTPS в production
- Для локальной разработки можно использовать ngrok или mock данные
- Bot Token должен быть создан через @BotFather
