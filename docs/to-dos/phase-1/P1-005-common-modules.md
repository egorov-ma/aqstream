# P1-005 Реализация Common Modules

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Backend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Common modules — фундамент для всех микросервисов. Они обеспечивают:
- Единообразную обработку ошибок
- Централизованную аутентификацию и multi-tenancy
- Базовые сущности с аудитом
- Надёжную публикацию событий через Outbox pattern
- Переиспользуемые тестовые утилиты

### Технический контекст

6 модулей:
- `common-api` — DTO, exceptions, events
- `common-security` — JWT, TenantContext, filters
- `common-data` — BaseEntity, TenantAwareEntity, auditing
- `common-messaging` — Outbox pattern, EventPublisher
- `common-web` — GlobalExceptionHandler, CorrelationIdFilter
- `common-test` — Testcontainers setup, fixtures

## Цель

Реализовать все common модули с базовым функционалом, готовым для использования в сервисах.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-004 завершена

## Acceptance Criteria

### common-api

- [x] `PageResponse<T>` record для пагинации
- [x] `ErrorResponse` record (code, message, details)
- [x] `DomainEvent` абстрактный класс для событий
- [x] `AqStreamException` базовое исключение
- [x] `EntityNotFoundException` для 404
- [x] `ValidationException` для 400
- [x] `ConflictException` для 409

### common-security

- [x] `TenantContext` (ThreadLocal для tenant_id)
- [x] `UserPrincipal` record с данными пользователя
- [x] `JwtTokenProvider` для генерации/валидации JWT
- [x] `SecurityContext` utility для получения текущего пользователя
- [x] Unit тесты для JwtTokenProvider

### common-data

- [x] `BaseEntity` (id UUID, createdAt, updatedAt)
- [x] `TenantAwareEntity` extends BaseEntity (+tenantId)
- [x] `SoftDeletableEntity` extends TenantAwareEntity (+deletedAt)
- [x] `AuditingConfig` для автозаполнения createdAt/updatedAt
- [x] `TenantEntityListener` для автозаполнения tenantId

### common-messaging

- [x] `OutboxMessage` entity
- [x] `OutboxRepository`
- [x] `EventPublisher` interface и implementation
- [x] `OutboxProcessor` scheduled job

### common-web

- [x] `GlobalExceptionHandler` (@ControllerAdvice)
- [x] `CorrelationIdFilter` для X-Correlation-ID
- [x] `TenantContextFilter` для установки TenantContext
- [x] `RequestLoggingFilter` для логирования запросов

### common-test

- [x] `@IntegrationTest` composite annotation
- [x] `PostgresTestContainer` singleton
- [x] `RabbitMQTestContainer` singleton
- [x] `TestFixtures` утилиты

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Код соответствует code style (checkstyle проходит)
- [x] Unit тесты написаны (JwtTokenProvider)
- [x] Javadoc для публичных классов
- [x] Code review пройден
- [ ] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: все common модули
- [x] Frontend: не затрагивается
- [x] Database: не затрагивается напрямую
- [x] Infrastructure: не затрагивается

### Реализованные классы

| Модуль | Package | Классы |
|--------|---------|--------|
| common-api | `ru.aqstream.common.api` | `PageResponse`, `ErrorResponse` |
| common-api | `ru.aqstream.common.api.event` | `DomainEvent` |
| common-api | `ru.aqstream.common.api.exception` | `AqStreamException`, `EntityNotFoundException`, `ValidationException`, `ConflictException` |
| common-security | `ru.aqstream.common.security` | `TenantContext`, `UserPrincipal`, `JwtTokenProvider`, `JwtAuthenticationException`, `SecurityContext` |
| common-data | `ru.aqstream.common.data` | `BaseEntity`, `TenantAwareEntity`, `SoftDeletableEntity`, `TenantEntityListener`, `AuditingConfig` |
| common-messaging | `ru.aqstream.common.messaging` | `OutboxMessage`, `OutboxRepository`, `EventPublisher`, `EventPublishingException`, `OutboxProcessor`, `OutboxSchedulingConfig` |
| common-web | `ru.aqstream.common.web` | `GlobalExceptionHandler`, `CorrelationIdFilter`, `TenantContextFilter`, `RequestLoggingFilter` |
| common-test | `ru.aqstream.common.test` | `@IntegrationTest`, `PostgresTestContainer`, `RabbitMQTestContainer`, `TestFixtures` |

## Зависимости

### Блокирует

- [P1-006] API Gateway setup
- Все задачи сервисов в Phase 2

### Зависит от

- [P1-004] Gradle multi-module структура

## Out of Scope

- Реализация сервисов
- Liquibase миграции для OutboxMessage (будут в конкретных сервисах)
- Redis caching (Phase 4)
- Production-ready конфигурация JWT

## Заметки

- TenantContext использует ThreadLocal, требует очистки в filter
- Outbox pattern критичен для reliable event publishing
- Все exceptions наследуются от AqStreamException для единообразной обработки
- Testcontainers используют singleton pattern для ускорения тестов
- Комментарии в коде на русском согласно CLAUDE.md
