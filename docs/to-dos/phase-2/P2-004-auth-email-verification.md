# P2-004 Email verification и Password reset

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `in_progress` |
| Приоритет | `high` |
| Связь с roadmap | [Roadmap - Аутентификация](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

При регистрации по email требуется подтверждение адреса ([FR-1.1.4](../../business/functional-requirements.md)). Пользователи также должны иметь возможность сбросить забытый пароль ([FR-1.3](../../business/functional-requirements.md)).

**Важно:** Основной канал уведомлений — Telegram. Email используется только для верификации и сброса пароля, не для уведомлений о событиях.

### Технический контекст

- User Service обрабатывает verification и reset
- Notification Service отправляет письма (для этих конкретных случаев)
- Токены верификации/сброса хранятся в БД с expiration

**Связанные документы:**
- [User Service](../../tech-stack/backend/services/user-service.md#authentication)
- [Functional Requirements FR-1.3](../../business/functional-requirements.md#fr-13-восстановление-пароля)
- [Domain Model - User](../../data/domain-model.md#user) — поля `email_verified`, `email_verified_at`

## Цель

Реализовать подтверждение email при регистрации и механизм сброса пароля.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### Email Verification

- [x] После регистрации по email отправляется письмо с ссылкой подтверждения
- [x] Ссылка содержит уникальный токен (UUID)
- [x] Endpoint `POST /api/v1/auth/verify-email` подтверждает email
- [x] Токен верификации действителен 24 часа
- [x] После подтверждения `email_verified = true`, `email_verified_at` заполняется
- [x] Можно запросить повторную отправку письма

### Password Reset

- [x] Endpoint `POST /api/v1/auth/forgot-password` отправляет письмо со ссылкой
- [x] Ссылка для сброса действительна 1 час ([FR-1.3.2](../../business/functional-requirements.md))
- [x] Endpoint `POST /api/v1/auth/reset-password` устанавливает новый пароль
- [x] После сброса все активные сессии завершаются ([FR-1.3.3](../../business/functional-requirements.md))
- [x] Все refresh токены пользователя отзываются
- [x] Новый пароль валидируется по тем же правилам

### Безопасность

- [x] Токены одноразовые
- [x] При запросе на несуществующий email — ответ такой же (timing attack prevention)
- [x] Rate limiting на endpoints (не более 3 запросов в час на email)

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Код написан согласно code style проекта
- [x] Unit тесты написаны
- [x] Integration тесты написаны
- [ ] Email шаблоны созданы (TODO: интеграция с notification-service)
- [x] Миграции созданы
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service` (VerificationService), `notification-service` (EmailSender)
- [ ] Frontend: страницы verify-email, forgot-password, reset-password
- [x] Database: таблица `verification_tokens`
- [ ] Infrastructure: SMTP конфигурация

### Модель данных

```sql
CREATE TABLE user_service.verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_service.users(id),
    token VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL, -- EMAIL_VERIFICATION, PASSWORD_RESET
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API Endpoints

```
POST /api/v1/auth/verify-email        { token: string }
POST /api/v1/auth/resend-verification { email: string }
POST /api/v1/auth/forgot-password     { email: string }
POST /api/v1/auth/reset-password      { token: string, newPassword: string }
```

### Email Templates

- `email-verification.html` — подтверждение email
- `password-reset.html` — сброс пароля

## Зависимости

### Блокирует

- Нет

### Зависит от

- [P2-001](./P2-001-auth-email-registration.md) User entity
- [P2-003](./P2-003-auth-jwt-refresh.md) Refresh token revocation

## Out of Scope

- Magic link login (вход по ссылке без пароля)
- Email change verification (изменение email)
- Telegram notifications — только email для этих операций

## Заметки

- Для локальной разработки использовать Mailhog или console output
- В production — настроить SMTP (SendGrid, Amazon SES)
- Ссылки должны вести на frontend, который вызывает API
