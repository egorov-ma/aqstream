# P2-001 Регистрация и вход по email

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `review` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Аутентификация](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Email-аутентификация — альтернативный способ входа для пользователей, не использующих Telegram. Согласно [FR-1.1](../../business/functional-requirements.md), система должна поддерживать регистрацию по email как альтернативу Telegram.

### Технический контекст

- Существует `JwtTokenProvider` в `common-security` — генерация и валидация JWT токенов
- Существует `UserPrincipal` — модель данных пользователя в токене
- User Service запущен на порту 8081, но бизнес-логика не реализована
- Структура сервиса: `user-service-api`, `user-service-service`, `user-service-db`, `user-service-client`

**Связанные документы:**
- [User Service](../../tech-stack/backend/services/user-service.md) — API endpoints, модель данных
- [Domain Model - User](../../data/domain-model.md#user) — структура сущности User
- [Security](../../experience/security.md) — требования к паролям
- [Common Library](../../tech-stack/backend/common-library.md) — базовые классы

## Цель

Реализовать полный цикл регистрации и входа пользователя по email и паролю в User Service.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

- [x] Пользователь может зарегистрироваться по email и паролю (`POST /api/v1/auth/register`)
- [x] Пароль валидируется: минимум 8 символов, буквы и цифры ([FR-1.1.5](../../business/functional-requirements.md))
- [x] Email проверяется на уникальность ([FR-1.1.6](../../business/functional-requirements.md))
- [x] Пользователь может войти по email и паролю (`POST /api/v1/auth/login`)
- [x] При успешном входе возвращается JWT access token (15 минут) и refresh token (7 дней)
- [x] Аккаунт блокируется после 5 неудачных попыток входа ([FR-1.2.6](../../business/functional-requirements.md))
- [x] Пароль хешируется через bcrypt с cost factor 12
- [x] Email хранится в нижнем регистре
- [x] Сообщения об ошибках на русском языке

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Код написан согласно code style проекта (Spring MVC, не WebFlux)
- [x] Unit тесты написаны и проходят (19 unit тестов)
- [x] Integration тесты с Testcontainers написаны (11 integration тестов)
- [x] API соответствует [User Service API](../../tech-stack/backend/services/user-service.md#authentication)
- [x] Liquibase миграции созданы с rollback
- [x] Code review пройден
- [ ] CI/CD pipeline проходит
- [x] Функционал проверен на локальном окружении

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service-api` (DTO), `user-service-service` (логика), `user-service-db` (entities, migrations)
- [ ] Frontend: —
- [x] Database: `user_service` schema, таблицы `users`, `refresh_tokens`
- [ ] Infrastructure: —

### Подход к реализации

1. **user-service-db:**
   - Entity `User` (extends `SoftDeletableEntity`)
   - Entity `RefreshToken`
   - Repository `UserRepository`, `RefreshTokenRepository`
   - Liquibase миграции для таблиц

2. **user-service-api:**
   - `RegisterRequest`, `LoginRequest`, `AuthResponse` DTO
   - `UserDto`
   - Exception classes (`EmailAlreadyExistsException`, `InvalidCredentialsException`)

3. **user-service-service:**
   - `AuthService` — регистрация, вход, refresh, лимит сессий
   - `AuthController` — REST endpoints
   - `PasswordService` — валидация и хеширование
   - `TokenCleanupService` — периодическая очистка истёкших токенов
   - Интеграция с `JwtTokenProvider` из common-security

### API Endpoints

```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

### Модель данных

См. [Domain Model - User](../../data/domain-model.md#user)

## Зависимости

### Блокирует

- [P2-004](./P2-004-auth-email-verification.md) Email verification
- [P2-005](./P2-005-organizations-requests.md) Организации (нужен User)
- [P2-015](./P2-015-frontend-auth-pages.md) Frontend auth pages

### Зависит от

- Нет блокирующих зависимостей (common modules готовы)

## Out of Scope

- Регистрация через Telegram (P2-002)
- Email verification (P2-004)
- Password reset (P2-004)
- Social login (OAuth)

## Заметки

**ВНИМАНИЕ — Расхождение в документации:**
- В [functional-requirements.md](../../business/functional-requirements.md) указано `JWT (RS256)`
- В [security.md](../../experience/security.md) указано `HS256`
- В коде `JwtTokenProvider` используется HMAC (HS256)

**Решение:** Использовать HS256 как в коде. Обновить functional-requirements.md при выполнении задачи.

**Дополнительные улучшения (сверх scope):**
- Лимит активных сессий: максимум 10 активных refresh tokens на пользователя. При превышении старые сессии автоматически отзываются.
- Периодическая очистка: `TokenCleanupService` удаляет истёкшие refresh tokens каждый час.
- Refresh tokens хранятся хешированными (SHA-256) для безопасности.
- One-time use для refresh tokens с rotation.

**Константы безопасности:**
- `User.MAX_FAILED_LOGIN_ATTEMPTS = 5` — блокировка после 5 неудачных попыток
- `User.LOCK_DURATION_MINUTES = 15` — длительность блокировки
- `AuthService.MAX_ACTIVE_SESSIONS = 10` — максимум активных сессий
