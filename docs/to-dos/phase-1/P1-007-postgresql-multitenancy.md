# P1-007 PostgreSQL с Multi-Tenancy (RLS)

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Backend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

AqStream — multi-tenant платформа, где каждая организация (tenant) имеет изолированные данные. Row Level Security (RLS) обеспечивает:
- Автоматическую изоляцию данных на уровне БД
- Защиту от случайного доступа к чужим данным
- Невозможность обойти изоляцию через SQL injection

### Технический контекст

Архитектура данных:
- 3 PostgreSQL инстанса (shared, user, payment)
- Schema-per-service внутри shared database
- RLS политики на всех таблицах с tenant_id
- `app.tenant_id` session variable для RLS

Сервисы и их базы данных:
| Сервис | База данных | Схема |
|--------|-------------|-------|
| Event Service | postgres-shared | event_service |
| Notification Service | postgres-shared | notification_service |
| Media Service | postgres-shared | media_service |
| Analytics Service | postgres-shared | analytics_service |
| User Service | postgres-user | user_service |
| Payment Service | postgres-payment | payment_service |

## Цель

Настроить PostgreSQL с Row Level Security для изоляции данных между tenants.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-002 завершена (Docker Compose с PostgreSQL)

## Acceptance Criteria

- [x] PostgreSQL init scripts созданы для всех баз
- [x] Схемы созданы для каждого сервиса
- [x] RLS включён на тестовых таблицах
- [x] RLS политики работают корректно
- [x] Liquibase настроен для каждого сервиса
- [x] Базовый changelog с созданием схемы
- [x] `TenantAwareDataSourceDecorator` для установки session variable
- [x] Интеграционные тесты для RLS
- [x] Документация в `docs/architecture/data-architecture.md` обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] RLS изоляция протестирована
- [x] Миграции применяются без ошибок
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: Liquibase, TenantAwareDataSourceDecorator
- [ ] Frontend: не затрагивается
- [x] Database: PostgreSQL configuration, RLS
- [x] Infrastructure: init scripts

### Реализованные файлы

| Тип | Файл | Описание |
|-----|------|----------|
| Init Script | `docker/postgres/init/01-init-shared-db.sql` | RLS функции, схемы shared DB |
| Init Script | `docker/postgres/init/02-init-user-db.sql` | RLS функции, схема user DB |
| Init Script | `docker/postgres/init/03-init-payment-db.sql` | RLS функции, схема payment DB |
| Java | `common/common-data/.../TenantAwareDataSourceDecorator.java` | Установка app.tenant_id |
| Java | `common/common-data/.../TenantAwareDataSourceConfig.java` | Автоконфигурация |
| Liquibase | `event-service-db/.../db.changelog-master.xml` | Master changelog |
| Liquibase | `event-service-db/.../001-create-events-table.xml` | Таблица events с RLS |
| Test | `common/common-data/.../TenantAwareDataSourceDecoratorTest.java` | Unit тесты декоратора |

### Ключевые компоненты

**current_tenant_id()** — PostgreSQL функция для RLS политик:
- Читает `app.tenant_id` из session variable
- Возвращает NULL если не установлен (RLS вернёт 0 строк)
- Создаётся во всех БД через init scripts

**TenantAwareDataSourceDecorator** — декоратор DataSource:
- Устанавливает `SET app.tenant_id` при получении connection
- Сбрасывает `RESET app.tenant_id` если контекст не установлен
- Активируется через `aqstream.multitenancy.rls.enabled=true`

**RLS политика** — применяется к таблицам с tenant_id:
- `ENABLE ROW LEVEL SECURITY` на таблице
- `CREATE POLICY ... USING (tenant_id = current_tenant_id())`

## Зависимости

### Блокирует

- Все сервисы, работающие с БД

### Зависит от

- [P1-002] Docker Compose (PostgreSQL)
- [P1-005] Common modules (TenantContext)

## Out of Scope

- Создание конкретных таблиц сервисов (будет в Phase 2)
- Backup и restore процедуры
- Read replicas (Phase 4)
- Database monitoring

## Заметки

- RLS политики применяются ко ВСЕМ запросам, включая raw SQL
- Superuser (postgres) обходит RLS — не используем его в приложении
- При отсутствии tenant_id RLS вернёт пустой результат
- Для system operations (миграции, scheduled jobs) нужен bypass RLS role
- Тестируем RLS на каждом PR с изменениями схемы
