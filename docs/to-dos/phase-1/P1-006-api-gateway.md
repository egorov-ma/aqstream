# P1-006 API Gateway Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Backend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

API Gateway — единая точка входа для всех клиентских запросов. Он обеспечивает:
- Централизованную аутентификацию
- Rate limiting для защиты от злоупотреблений
- Routing к downstream сервисам
- Cross-cutting concerns (CORS, logging, correlation ID)

### Технический контекст

Gateway — **единственный** сервис на WebFlux/reactive stack. Это обусловлено природой proxy-сервиса, где неблокирующий I/O критичен для производительности.

Технологии:
- Spring Cloud Gateway
- WebFlux (reactive)
- Redis для rate limiting
- JWT validation

## Цель

Реализовать API Gateway с базовым routing, аутентификацией и rate limiting.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-005 завершена (common modules для JWT)
- [x] P1-002 завершена (Docker Compose с Redis)

## Acceptance Criteria

- [x] Создан модуль `services/gateway`
- [x] Настроен Spring Cloud Gateway
- [x] Реализованы маршруты ко всем сервисам:
  - [x] `/api/v1/auth/**` → user-service
  - [x] `/api/v1/users/**` → user-service
  - [x] `/api/v1/organizations/**` → user-service
  - [x] `/api/v1/events/**` → event-service
  - [x] `/api/v1/registrations/**` → event-service
  - [x] `/api/v1/payments/**` → payment-service
  - [x] `/api/v1/webhooks/**` → payment-service
  - [x] `/api/v1/notifications/**` → notification-service
  - [x] `/api/v1/media/**` → media-service
  - [x] `/api/v1/analytics/**` → analytics-service
- [x] Реализован `JwtAuthenticationFilter`
- [x] Реализован `CorrelationIdFilter`
- [x] Настроен rate limiting через Redis
- [x] Настроен CORS
- [x] Реализован `GlobalErrorHandler`
- [x] Health check endpoint работает
- [x] Dockerfile создан
- [x] Добавлен в docker-compose.yml
- [x] Unit тесты для filters
- [x] Документация сервиса обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Gateway запускается и проходит health check
- [x] Routing работает (можно проверить с mock downstream)
- [x] Rate limiting работает
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: services/gateway
- [x] Frontend: не затрагивается
- [x] Database: не затрагивается (только Redis)
- [x] Infrastructure: docker-compose.yml обновление

### Реализованные классы

| Package | Класс | Описание |
|---------|-------|----------|
| `ru.aqstream.gateway` | `GatewayApplication` | Точка входа |
| `ru.aqstream.gateway.config` | `CorsConfig` | Настройка CORS |
| `ru.aqstream.gateway.config` | `RateLimitConfig` | KeyResolver для rate limiting |
| `ru.aqstream.gateway.filter` | `JwtAuthenticationFilter` | JWT аутентификация |
| `ru.aqstream.gateway.filter` | `CorrelationIdFilter` | X-Correlation-ID |
| `ru.aqstream.gateway.handler` | `GlobalErrorHandler` | Обработка ошибок |
| `ru.aqstream.gateway.security` | `JwtTokenValidator` | Валидация JWT токенов |
| `ru.aqstream.gateway.security` | `JwtValidationException` | Исключение валидации |

### Rate Limit Tiers

| Тип | Лимит | Key |
|-----|-------|-----|
| Anonymous | 100 req/min | IP address |
| Authenticated | 1000 req/min | User ID |

## Зависимости

### Блокирует

- Все интеграционные тесты между сервисами
- E2E тесты

### Зависит от

- [P1-002] Docker Compose (Redis)
- [P1-004] Gradle structure
- [P1-005] Common modules (JWT)

## Out of Scope

- Service discovery (используем прямые URL в Phase 1)
- Circuit breaker (Phase 4)
- Distributed tracing (Phase 4)
- API versioning logic (сервисы сами управляют версиями)

## Заметки

- Gateway — единственный WebFlux сервис, остальные на Spring MVC
- Rate limiting хранится в Redis для persistence
- PUBLIC_PATHS можно расширять через конфигурацию
- Для локальной разработки downstream сервисы будут доступны напрямую
- В production все запросы должны идти через Gateway
- JWT валидатор создан отдельно от common-security из-за конфликта servlet/reactive stack
