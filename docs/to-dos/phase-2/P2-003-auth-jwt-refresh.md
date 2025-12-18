# P2-003 JWT токены и refresh механизм

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Аутентификация](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

JWT токены обеспечивают stateless аутентификацию между клиентом и сервисами. Access token имеет короткий срок жизни (15 минут) для безопасности, refresh token — длинный (7 дней) для удобства пользователя.

### Технический контекст

- `JwtTokenProvider` реализован в `common-security`
- Генерация access и refresh токенов работает
- Хранение refresh токенов в БД с механизмом ротации реализовано
- Gateway валидирует access токены через `JwtAuthenticationFilter`

**Связанные документы:**
- [Security](../../experience/security.md#jwt-tokens) — требования к токенам
- [User Service](../../tech-stack/backend/services/user-service.md#jwt-tokens) — структура токенов
- [Common Library](../../tech-stack/backend/common-library.md#common-security) — JwtTokenProvider

**Реализованный код:**
- [JwtTokenProvider.java](../../../common/common-security/src/main/java/ru/aqstream/common/security/JwtTokenProvider.java)
- [TokenHasher.java](../../../common/common-security/src/main/java/ru/aqstream/common/security/TokenHasher.java)
- [RefreshToken.java](../../../services/user-service/user-service-db/src/main/java/ru/aqstream/user/db/entity/RefreshToken.java)
- [AuthService.java](../../../services/user-service/user-service-service/src/main/java/ru/aqstream/user/service/AuthService.java)
- [AuthController.java](../../../services/user-service/user-service-service/src/main/java/ru/aqstream/user/controller/AuthController.java)

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

- [x] Access token содержит: userId, email, tenantId, roles
- [x] Access token действителен 15 минут ([FR-1.2.4](../../business/functional-requirements.md))
- [x] Refresh token действителен 7 дней ([FR-1.2.5](../../business/functional-requirements.md))
- [x] Refresh token хранится в БД (hashed) — `TokenHasher.hash()` SHA-256
- [x] При использовании refresh token — выдаётся новая пара токенов (rotation)
- [x] Использованный refresh token помечается как revoked
- [x] Endpoint `POST /api/v1/auth/refresh` работает корректно
- [x] Endpoint `POST /api/v1/auth/logout` отзывает все refresh токены пользователя
- [x] При выходе все активные сессии завершаются
- [x] Refresh token одноразовый (one-time use)

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Код написан согласно code style проекта
- [x] Unit тесты для AuthService написаны (включая refresh, logout, revokeToken)
- [x] Integration тесты написаны (AuthControllerIntegrationTest)
- [x] Миграции для таблицы `refresh_tokens` созданы
- [x] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service-db` (RefreshToken entity), `user-service-service` (AuthService)
- [ ] Frontend: interceptor для автоматического refresh (P2-015)
- [x] Database: таблица `refresh_tokens`
- [x] Infrastructure: TokenCleanupService для очистки истёкших токенов

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
- Device tracking (user agent и IP сохраняются для аудита, но не используются для блокировки)

## Реализовано дополнительно

- **Concurrent session limits** — MAX_ACTIVE_SESSIONS = 10, старые сессии автоматически отзываются
- **jti (JWT ID)** — уникальный ID для каждого refresh token
- **TokenCleanupService** — scheduled job для очистки истёкших токенов (каждый час)
- **Audit metadata** — сохранение user_agent и ip_address для каждого токена

## Заметки

- `JwtTokenProvider` использует HMAC (HS256) — это правильно для нашего случая
- В production JWT_SECRET должен быть минимум 256 бит
