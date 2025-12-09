# P1-007 PostgreSQL с Multi-Tenancy (RLS)

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
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
- [ ] P1-002 завершена (Docker Compose с PostgreSQL)

## Acceptance Criteria

- [ ] PostgreSQL init scripts созданы для всех баз
- [ ] Схемы созданы для каждого сервиса
- [ ] RLS включён на тестовых таблицах
- [ ] RLS политики работают корректно
- [ ] Liquibase настроен для каждого сервиса
- [ ] Базовый changelog с созданием схемы
- [ ] `TenantAwareConnectionProvider` для установки session variable
- [ ] Интеграционные тесты для RLS
- [ ] Документация в `docs/architecture/data-architecture.md` обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] RLS изоляция протестирована
- [ ] Миграции применяются без ошибок
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: Liquibase, connection provider
- [ ] Frontend: не затрагивается
- [x] Database: PostgreSQL configuration, RLS
- [x] Infrastructure: init scripts

### Структура файлов

```
docker/
└── postgres/
    └── init/
        ├── 01-shared-init.sql      # Схемы для shared db
        ├── 02-user-init.sql        # Схема для user db
        └── 03-payment-init.sql     # Схема для payment db

services/
└── event-service/
    └── event-service-db/
        └── src/main/resources/
            └── db/changelog/
                ├── db.changelog-master.xml
                └── changes/
                    └── 001-create-schema.xml
```

### PostgreSQL Init Scripts

**01-shared-init.sql:**
```sql
-- Создание схем
CREATE SCHEMA IF NOT EXISTS event_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS media_service;
CREATE SCHEMA IF NOT EXISTS analytics_service;

-- Создание функции для получения tenant_id
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.tenant_id', true)::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Пример создания таблицы с RLS
-- (реальные таблицы будут в Liquibase миграциях сервисов)
```

**02-user-init.sql:**
```sql
CREATE SCHEMA IF NOT EXISTS user_service;

-- User service не использует RLS для users таблицы
-- (пользователи не привязаны к tenant до входа)
-- Но organizations и members используют RLS
```

**03-payment-init.sql:**
```sql
CREATE SCHEMA IF NOT EXISTS payment_service;

CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.tenant_id', true)::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### RLS Policy Example

```sql
-- Включение RLS на таблице
ALTER TABLE event_service.events ENABLE ROW LEVEL SECURITY;

-- Политика: пользователь видит только данные своего tenant
CREATE POLICY tenant_isolation ON event_service.events
    FOR ALL
    USING (tenant_id = current_tenant_id());

-- Политика для INSERT: автоматическая установка tenant_id
CREATE POLICY tenant_insert ON event_service.events
    FOR INSERT
    WITH CHECK (tenant_id = current_tenant_id());
```

### TenantAwareConnectionProvider

**Package:** `ru.aqstream.common.data`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantAwareConnectionProvider implements ConnectionProvider {

    private final DataSource dataSource;

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();

        UUID tenantId = TenantContext.getTenantIdOptional().orElse(null);
        if (tenantId != null) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format(
                    "SET app.tenant_id = '%s'",
                    tenantId.toString()
                ));
            }
            log.debug("Установлен tenant_id для соединения: {}", tenantId);
        }

        return connection;
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        // Очищаем tenant_id перед возвратом в pool
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("RESET app.tenant_id");
        }
        connection.close();
    }
}
```

### Liquibase Configuration

**application.yml (пример для event-service):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DATABASE_HOST:localhost}:${DATABASE_PORT:5432}/${DATABASE_NAME:shared_services_db}
    username: ${DATABASE_USERNAME:aqstream}
    password: ${DATABASE_PASSWORD:aqstream}

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: event_service
```

**db.changelog-master.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <include file="db/changelog/changes/001-create-schema.xml"/>

</databaseChangeLog>
```

**001-create-schema.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <changeSet id="001-1" author="aqstream">
        <comment>Создание схемы event_service</comment>
        <sql>CREATE SCHEMA IF NOT EXISTS event_service;</sql>
        <rollback>DROP SCHEMA IF EXISTS event_service CASCADE;</rollback>
    </changeSet>

</databaseChangeLog>
```

### Integration Test for RLS

```java
@IntegrationTest
class RlsIsolationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenant1 = UUID.randomUUID();
    private UUID tenant2 = UUID.randomUUID();

    @Test
    void tenant_CanOnlySeeOwnData() {
        // Given: создаём события для двух tenant'ов
        TenantContext.setTenantId(tenant1);
        Event event1 = eventRepository.save(createEvent("Событие Tenant 1"));

        TenantContext.setTenantId(tenant2);
        Event event2 = eventRepository.save(createEvent("Событие Tenant 2"));

        // When: tenant1 запрашивает свои события
        TenantContext.setTenantId(tenant1);
        List<Event> tenant1Events = eventRepository.findAll();

        // Then: видит только своё событие
        assertThat(tenant1Events).hasSize(1);
        assertThat(tenant1Events.get(0).getId()).isEqualTo(event1.getId());

        // Cleanup
        TenantContext.clear();
    }

    @Test
    void tenant_CannotAccessOtherTenantDataViaRawSql() {
        // Given: событие другого tenant'а
        TenantContext.setTenantId(tenant1);
        Event event = eventRepository.save(createEvent("Secret Event"));

        // When: попытка доступа через raw SQL с другим tenant
        TenantContext.setTenantId(tenant2);

        // Then: RLS блокирует доступ
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_service.events WHERE id = ?",
            Integer.class,
            event.getId()
        );
        assertThat(count).isZero();

        TenantContext.clear();
    }
}
```

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
