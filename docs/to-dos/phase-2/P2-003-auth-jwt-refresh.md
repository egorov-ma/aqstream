# P2-003 JWT токены и refresh механизм

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Аутентификация](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

JWT токены обеспечивают stateless аутентификацию между клиентом и сервисами. Access token имеет короткий срок жизни (15 минут) для безопасности, refresh token — длинный (7 дней) для удобства пользователя.

### Технический контекст

- `JwtTokenProvider` уже реализован в `common-security`
- Генерация access и refresh токенов работает
- Нужно реализовать хранение refresh токенов в БД и механизм ротации
- Gateway валидирует access токены через `JwtAuthenticationFilter`

**Связанные документы:**
- [Security](../../experience/security.md#jwt-tokens) — требования к токенам
- [User Service](../../tech-stack/backend/services/user-service.md#jwt-tokens) — структура токенов
- [Common Library](../../tech-stack/backend/common-library.md#common-security) — JwtTokenProvider

**Существующий код:**
- [JwtTokenProvider.java](../../../common/common-security/src/main/java/ru/aqstream/common/security/JwtTokenProvider.java)
- [JwtAuthenticationFilter.java](../../../services/gateway/src/main/java/ru/aqstream/gateway/filter/JwtAuthenticationFilter.java)

## Цель

Реализовать полный механизм JWT токенов с хранением refresh токенов в БД, ротацией и отзывом.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

- [ ] Access token содержит: userId, email, tenantId, roles
- [ ] Access token действителен 15 минут ([FR-1.2.4](../../business/functional-requirements.md))
- [ ] Refresh token действителен 7 дней ([FR-1.2.5](../../business/functional-requirements.md))
- [ ] Refresh token хранится в БД (hashed)
- [ ] При использовании refresh token — выдаётся новая пара токенов (rotation)
- [ ] Использованный refresh token помечается как revoked
- [ ] Endpoint `POST /api/v1/auth/refresh` работает корректно
- [ ] Endpoint `POST /api/v1/auth/logout` отзывает все refresh токены пользователя
- [ ] При выходе все активные сессии завершаются
- [ ] Refresh token одноразовый (one-time use)

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Код написан согласно code style проекта
- [ ] Unit тесты для JwtTokenProvider расширены
- [ ] Integration тесты написаны
- [ ] Миграции для таблицы `refresh_tokens` созданы
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service-db` (RefreshToken entity), `user-service-service` (TokenService)
- [ ] Frontend: interceptor для автоматического refresh
- [x] Database: таблица `refresh_tokens`
- [ ] Infrastructure: —

### Модель данных RefreshToken

```sql
CREATE TABLE user_service.refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_service.users(id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON user_service.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON user_service.refresh_tokens(token_hash) WHERE NOT revoked;
```

### Алгоритм refresh

1. Клиент отправляет refresh token
2. Сервис ищет токен по hash в БД
3. Проверяет: не revoked, не expired
4. Помечает старый токен как revoked
5. Генерирует новую пару токенов
6. Сохраняет новый refresh token
7. Возвращает клиенту

### Безопасность

- Refresh token хранится как SHA-256 hash
- При компрометации одного токена — автоматический отзыв всех (anomaly detection)
- Cleanup job для удаления старых токенов

## Зависимости

### Блокирует

- Все задачи требующие аутентификации

### Зависит от

- [P2-001](./P2-001-auth-email-registration.md) User entity и базовая структура

## Out of Scope

- Sliding session expiration
- Device tracking
- Concurrent session limits

## Заметки

- Текущий `JwtTokenProvider` использует HMAC (HS256) — это правильно для нашего случая
- В production JWT_SECRET должен быть минимум 256 бит
- Рассмотреть добавление jti (JWT ID) для tracking
