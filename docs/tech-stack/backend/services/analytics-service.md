# Analytics Service

Analytics Service отвечает за сбор метрик и отчётность.

## Обзор

| Параметр | Значение |
|----------|----------|
| Порт | 8086 |
| База данных | postgres-analytics (dedicated, TimescaleDB) |
| Схема | analytics_service |

## Ответственности

- Сбор событий (event tracking)
- Агрегация метрик
- Дашборды для организаторов
- Экспорт отчётов

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/analytics/track` | Трекинг события |
| GET | `/api/v1/analytics/events/{eventId}/dashboard` | Дашборд события |
| GET | `/api/v1/analytics/events/{eventId}/funnel` | Воронка |
| GET | `/api/v1/analytics/organizations/{orgId}/report` | Отчёт организации |
| POST | `/api/v1/analytics/export` | Экспорт данных |

## Модель данных

```sql
-- TimescaleDB hypertable для событий
CREATE TABLE analytics_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_id UUID,
    user_id UUID,
    properties JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable('analytics_events', 'created_at');

-- Индексы
CREATE INDEX idx_analytics_tenant_type ON analytics_events(tenant_id, event_type, created_at DESC);
CREATE INDEX idx_analytics_event ON analytics_events(event_id, created_at DESC);
```

## Типы событий

| Event Type | Описание |
|------------|----------|
| `page.view` | Просмотр страницы события |
| `registration.started` | Начало регистрации |
| `registration.completed` | Завершение регистрации |
| `registration.cancelled` | Отмена регистрации |
| `checkin.completed` | Check-in |

## Трекинг

**Процесс:**
1. Получение события (из API или RabbitMQ)
2. Обогащение контекстом (tenant_id, user_id)
3. Сохранение в TimescaleDB hypertable

**Properties (JSONB):**
- Произвольные данные для каждого типа события
- Например: `ticketTypeId`, `registrationId`, `source`

## Дашборд события

**Метрики:**

| Метрика | Описание |
|---------|----------|
| `totalViews` | Общее количество просмотров |
| `uniqueViews` | Уникальные посетители |
| `totalRegistrations` | Всего регистраций |
| `confirmedRegistrations` | Подтверждённых |
| `checkedIn` | Пришедших |
| `viewToRegistrationRate` | Конверсия просмотр → регистрация |
| `registrationToCheckinRate` | Конверсия регистрация → check-in |

**Группировки:**
- По дням (графики динамики)
- По типам билетов
- По источникам трафика

## Воронка регистраций

| Этап | Описание |
|------|----------|
| Page views | Просмотры страницы события |
| Registration started | Начали заполнять форму |
| Registration completed | Завершили регистрацию |
| Checked in | Пришли на событие |

**Конверсии:**
- `startRate` = started / views
- `completionRate` = completed / started
- `attendanceRate` = checkedIn / completed

## События (RabbitMQ)

### Потребляемые

| Event | Действие |
|-------|----------|
| `event.published` | Начало трекинга |
| `registration.created` | registration.completed |
| `registration.cancelled` | registration.cancelled |
| `checkin.completed` | checkin.completed |
| `payment.completed` | payment.completed |

## Экспорт

### Форматы

| Формат | MIME Type |
|--------|-----------|
| CSV | text/csv |
| XLSX | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |

### Параметры запроса

| Параметр | Описание |
|----------|----------|
| `eventId` | ID события (опционально) |
| `organizationId` | ID организации |
| `dateFrom` | Начало периода |
| `dateTo` | Конец периода |
| `format` | Формат экспорта (csv/xlsx) |

## Retention

**Политика хранения:**
- Автоматическое удаление данных старше 1 года
- Реализовано через TimescaleDB retention policy

```sql
SELECT add_retention_policy('analytics_events', INTERVAL '1 year');
```

## Дальнейшее чтение

- [Service Topology](../../../architecture/service-topology.md)
