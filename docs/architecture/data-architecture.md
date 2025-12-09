# Data Architecture

Архитектура данных платформы AqStream.

## Обзор

```mermaid
flowchart TB
    subgraph Services["Микросервисы"]
        US["User Service"]
        ES["Event Service"]
        PS["Payment Service"]
        NS["Notification Service"]
        MS["Media Service"]
        AS["Analytics Service"]
    end
    
    subgraph Databases["PostgreSQL Instances"]
        PG_USER["postgres-user<br/>:5433"]
        PG_SHARED["postgres-shared<br/>:5432"]
        PG_PAYMENT["postgres-payment<br/>:5434"]
        PG_ANALYTICS["postgres-analytics<br/>:5435"]
    end
    
    subgraph Cache["Redis :6379"]
        Sessions["Sessions"]
        AppCache["App Cache"]
        RateLimit["Rate Limits"]
    end
    
    subgraph Files["MinIO :9000"]
        Images["Images"]
        Documents["Documents"]
    end
    
    US --> PG_USER
    ES --> PG_SHARED
    NS --> PG_SHARED
    MS --> PG_SHARED
    PS --> PG_PAYMENT
    AS --> PG_ANALYTICS
    
    Services --> Cache
    MS --> Files
```

## Стратегия баз данных

### Database-per-Service с Mixed Deployment

Используем гибридный подход:

| Сервис | Instance | Причина |
|--------|----------|---------|
| User Service | Dedicated (postgres-user) | Критичность, изоляция auth данных |
| Payment Service | Dedicated (postgres-payment) | PCI DSS compliance, аудит |
| Analytics Service | Dedicated (postgres-analytics) | TimescaleDB, высокая нагрузка на запись |
| Event Service | Shared (postgres-shared) | Стандартная нагрузка |
| Notification Service | Shared (postgres-shared) | Стандартная нагрузка |
| Media Service | Shared (postgres-shared) | Только метаданные |

### Schema-per-Service

Каждый сервис имеет собственную схему:

```sql
-- postgres-shared
CREATE SCHEMA event_service;
CREATE SCHEMA notification_service;
CREATE SCHEMA media_service;

-- postgres-user
CREATE SCHEMA user_service;

-- postgres-payment
CREATE SCHEMA payment_service;

-- postgres-analytics
CREATE SCHEMA analytics_service;
```

## Multi-Tenancy

### Row Level Security

Изоляция данных организаций на уровне PostgreSQL:

```sql
-- Функция для получения tenant_id из session variable
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.tenant_id', true), '')::UUID;
EXCEPTION
    WHEN OTHERS THEN RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Включение RLS на таблице
ALTER TABLE event_service.events ENABLE ROW LEVEL SECURITY;

-- Политика изоляции
CREATE POLICY tenant_isolation_events ON event_service.events
    FOR ALL
    USING (tenant_id = current_tenant_id());
```

### Tenant Context в приложении

Для автоматической установки `app.tenant_id` используется `TenantAwareDataSourceDecorator`:

```java
// TenantAwareDataSourceDecorator автоматически устанавливает session variable
// при получении соединения из пула

@Override
public Connection getConnection() throws SQLException {
    Connection connection = delegate.getConnection();

    UUID tenantId = TenantContext.getTenantIdOptional().orElse(null);
    try (Statement stmt = connection.createStatement()) {
        if (tenantId != null) {
            stmt.execute(String.format("SET app.tenant_id = '%s'", tenantId));
        } else {
            stmt.execute("RESET app.tenant_id");
        }
    }
    return connection;
}
```

Активация в `application.yml`:

```yaml
aqstream:
  multitenancy:
    rls:
      enabled: true
```

### Таблицы без tenant_id

Некоторые таблицы глобальные:

| Таблица | Причина |
|---------|---------|
| users | Пользователь может быть в нескольких организациях |
| notification_templates | Системные шаблоны |
| analytics_events | Глобальные метрики |

## Схемы данных

Детальные ER-диаграммы всех сущностей: [Domain Model](../data/domain-model.md)

## Индексы

### Стратегия индексирования

```sql
-- Обязательные индексы на tenant_id для RLS
CREATE INDEX idx_events_tenant_id ON event_service.events(tenant_id);
CREATE INDEX idx_registrations_tenant_id ON event_service.registrations(tenant_id);

-- Индексы для частых запросов
CREATE INDEX idx_events_status ON event_service.events(status) WHERE status = 'PUBLISHED';
CREATE INDEX idx_events_starts_at ON event_service.events(starts_at);
CREATE INDEX idx_registrations_event_id ON event_service.registrations(event_id);
CREATE INDEX idx_registrations_user_id ON event_service.registrations(user_id);

-- Составные индексы
CREATE INDEX idx_events_tenant_status ON event_service.events(tenant_id, status);
```

## Кэширование (Redis)

### Структура ключей

```
aqstream:{service}:{entity}:{id}
aqstream:{service}:{entity}:list:{params_hash}

# Примеры
aqstream:event:event:550e8400-e29b-41d4-a716-446655440000
aqstream:event:events:list:abc123
aqstream:user:session:jwt_token_hash
```

### Что кэшируем

| Данные | TTL | Invalidation |
|--------|-----|--------------|
| Event details | 5 мин | При обновлении события |
| User profile | 15 мин | При обновлении профиля |
| Public event list | 1 мин | При публикации/отмене |
| Sessions | 24 часа | При logout |
| Rate limit counters | 1 мин | Автоматически |

### Cache-Aside Pattern

```java
@Service
@RequiredArgsConstructor
public class EventService {
    
    private final EventRepository eventRepository;
    private final RedisTemplate<String, EventDto> redisTemplate;
    
    public EventDto findById(UUID id) {
        String key = "aqstream:event:event:" + id;
        
        // Проверяем кэш
        EventDto cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        
        // Запрос к БД
        EventDto event = eventRepository.findById(id)
            .map(eventMapper::toDto)
            .orElseThrow(() -> new EventNotFoundException(id));
        
        // Сохраняем в кэш
        redisTemplate.opsForValue().set(key, event, Duration.ofMinutes(5));
        
        return event;
    }
    
    @Transactional
    public EventDto update(UUID id, UpdateEventRequest request) {
        // ... update logic
        
        // Invalidate cache
        String key = "aqstream:event:event:" + id;
        redisTemplate.delete(key);
        
        return updated;
    }
}
```

## Миграции

### Liquibase

Каждый сервис управляет своими миграциями:

```
services/event-service/event-service-db/src/main/resources/
└── db/changelog/
    ├── db.changelog-master.xml
    └── changes/
        ├── 001-create-events-table.xml
        ├── 002-create-ticket-types-table.xml
        ├── 003-create-registrations-table.xml
        ├── 004-add-waitlist.xml
        └── 005-add-check-in.xml
```

### Правила миграций

1. **Backward compatible** — старый код должен работать с новой схемой
2. **Additive only** — не удалять колонки напрямую
3. **Всегда rollback** — каждый changeset должен иметь rollback
4. **Не изменять applied** — никогда не редактировать уже применённые changesets

Подробнее: [Migrations](../data/migrations.md)

## Репликация и отказоустойчивость

### Development

Single instance PostgreSQL (достаточно для разработки).

### Production (рекомендации)

```mermaid
flowchart TB
    subgraph Primary
        PG_Primary["PostgreSQL Primary"]
    end
    
    subgraph Replicas
        PG_Replica1["Read Replica 1"]
        PG_Replica2["Read Replica 2"]
    end
    
    subgraph App["Application"]
        Write["Write Operations"]
        Read["Read Operations"]
    end
    
    Write --> PG_Primary
    Read --> PG_Replica1
    Read --> PG_Replica2
    PG_Primary --> PG_Replica1
    PG_Primary --> PG_Replica2
```

## Бэкапы

### Стратегия

| Тип | Частота | Retention |
|-----|---------|-----------|
| Full backup | Ежедневно | 30 дней |
| WAL archiving | Continuous | 7 дней |
| Point-in-time recovery | — | До 7 дней назад |

Подробнее: [Backup & Restore](../operations/runbooks/backup-restore.md)

## Дальнейшее чтение

- [Domain Model](../data/domain-model.md) — детальная модель данных
- [Migrations](../data/migrations.md) — управление миграциями
- [Service Topology](./service-topology.md) — топология сервисов
