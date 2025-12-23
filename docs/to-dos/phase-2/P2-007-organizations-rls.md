# P2-007 Row Level Security для multi-tenancy

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Организации](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Каждая организация — изолированный tenant. Данные одной организации не должны быть доступны другой. Это критично для безопасности и доверия пользователей к платформе.

### Технический контекст

- PostgreSQL Row Level Security (RLS) обеспечивает изоляцию на уровне БД
- `tenant_id` передаётся через `app.tenant_id` session variable
- `TenantContext` в common-security хранит текущий tenant
- `TenantContextFilter` устанавливает tenant из JWT токена
- `TenantAwareDataSourceDecorator` устанавливает `app.tenant_id` в PostgreSQL

**Связанные документы:**
- [Data Architecture](../../architecture/data-architecture.md#row-level-security) — RLS описание
- [Architecture Overview](../../architecture/overview.md#multi-tenancy) — multi-tenancy принципы
- [Common Library](../../tech-stack/backend/common-library.md#common-data) — TenantAwareEntity

**Существующий код:**
- [TenantContext.java](../../../common/common-security/src/main/java/ru/aqstream/common/security/TenantContext.java)
- [TenantAwareEntity.java](../../../common/common-data/src/main/java/ru/aqstream/common/data/TenantAwareEntity.java)
- [TenantAwareDataSourceDecorator.java](../../../common/common-data/src/main/java/ru/aqstream/common/data/TenantAwareDataSourceDecorator.java)

## Цель

Настроить Row Level Security в PostgreSQL для всех бизнес-таблиц и интегрировать с приложением.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### RLS Политики

- [x] RLS включён на всех таблицах с tenant_id (events — готово, остальные при создании)
- [x] Политики SELECT/INSERT/UPDATE/DELETE применяются
- [x] Данные одной организации недоступны другой
- [x] Попытка доступа к чужим данным возвращает пустой результат (не ошибку)

### Интеграция с приложением

- [x] `TenantContextFilter` извлекает tenant_id из JWT
- [x] `TenantAwareDataSourceDecorator` устанавливает `app.tenant_id` перед каждым запросом
- [x] Все сервисы используют tenant-aware конфигурацию (`aqstream.multitenancy.rls.enabled=true`)
- [x] При отсутствии tenant_id — запросы к tenant-aware таблицам возвращают пустой результат

### Специальные случаи

- [x] Админ платформы может видеть данные всех организаций (bypass RLS через отсутствие tenant context)
- [x] Публичные endpoints (страница события) работают без tenant_id (политика `public_events_read`)
- [ ] Cross-tenant запросы (листинг публичных событий) работают корректно (будет протестировано с реальными данными)

### Тестирование

- [x] Integration тесты проверяют изоляцию данных (`RlsIntegrationTest`)
- [x] Тест: создание данных в org A, попытка чтения из org B — пустой результат
- [x] Тест: superuser видит данные всех организаций (E2E тесты добавлены)

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] RLS миграции созданы с rollback (`003-improve-events-rls.xml`)
- [x] Unit тесты для TenantContext (`TenantContextTest.java`)
- [x] Integration тесты изоляции данных (`RlsIntegrationTest.java`)
- [x] Документация обновлена
- [x] Code review пройден
- [x] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: все сервисы (конфигурация DataSource)
- [ ] Frontend: —
- [x] Database: RLS политики на всех таблицах
- [ ] Infrastructure: —

### SQL миграции для RLS

```sql
-- Включение RLS на таблице events
ALTER TABLE event_service.events ENABLE ROW LEVEL SECURITY;

-- Политика для обычных пользователей
CREATE POLICY tenant_isolation_policy ON event_service.events
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- Политика для INSERT (tenant_id должен совпадать)
CREATE POLICY tenant_insert_policy ON event_service.events
    FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- Bypass для суперпользователя (админ)
ALTER TABLE event_service.events FORCE ROW LEVEL SECURITY;
```

### Таблицы с RLS

**Event Service (postgres-shared):**
- `event_service.events`
- `event_service.ticket_types`
- `event_service.registrations`
- `event_service.check_ins`
- `event_service.waitlist_entries`

**Notification Service (postgres-shared):**
- `notification_service.notification_logs`

**Media Service (postgres-shared):**
- `media_service.media`
- `media_service.media_variants`

### Глобальные таблицы (без RLS)

- `user_service.users`
- `user_service.organizations`
- `user_service.organization_members`
- `user_service.organization_requests`
- `notification_service.notification_templates` (системные)

### Конфигурация DataSource

```java
@Configuration
public class TenantAwareDataSourceConfig {

    @Bean
    public DataSource dataSource(DataSource originalDataSource) {
        return new TenantAwareDataSourceDecorator(originalDataSource);
    }
}
```

### Bypass для админа

```java
// При запросах от админа — не устанавливать tenant_id
// RLS политика с current_setting('app.tenant_id', true) вернёт NULL
// и политика пропустит все строки
```

## Зависимости

### Блокирует

- Все задачи работающие с tenant-aware данными (события, регистрации)

### Зависит от

- [P2-006](./P2-006-organizations-crud.md) Организации (источник tenant_id)

## Out of Scope

- Шифрование данных по tenant
- Separate schemas per tenant (используем RLS)
- Cross-tenant reporting (кроме админа)

## Заметки

- RLS — последний рубеж защиты. Приложение также должно проверять права доступа
- `current_setting('app.tenant_id', true)` — true означает, что при отсутствии параметра вернётся NULL
- Для публичных страниц событий нужен отдельный подход (чтение без tenant context)
- Performance: индексы на tenant_id критичны для производительности с RLS
