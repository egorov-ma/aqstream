# P2-009 CRUD событий и жизненный цикл

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `review` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - События](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Событие — центральная сущность платформы. Организатор создаёт событие, настраивает билеты, публикует для регистрации. Событие проходит жизненный цикл: Draft → Published → Completed/Cancelled.

### Технический контекст

- Event Service отвечает за события (порт 8082)
- События принадлежат организации (tenant_id)
- Событие может быть привязано к группе (приватное)
- Видимость участников настраивается (CLOSED/OPEN)

**Связанные документы:**
- [Event Service](../../tech-stack/backend/services/event-service.md) — API endpoints, модель
- [Domain Model - Event](../../data/domain-model.md#event)
- [Functional Requirements FR-4](../../business/functional-requirements.md#fr-4-события)
- [User Journeys - Journey 1](../../business/user-journeys.md#journey-1-создание-и-публикация-события)

## Цель

Реализовать полный CRUD событий с жизненным циклом и поддержкой приватных событий.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### Создание события

- [x] OWNER/MODERATOR могут создать событие (`POST /api/v1/events`)
- [x] Обязательные поля: title, starts_at
- [x] Опциональные поля: description (Markdown), ends_at, location_type, location_address/url, cover_image_id
- [x] Событие создаётся в статусе DRAFT
- [x] Генерируется уникальный slug в рамках организации
- [x] Настройка видимости участников (CLOSED по умолчанию)
- [x] Привязка к группе (опционально, для приватных событий)

### Жизненный цикл

```
Draft → Published → Completed
  ↓         ↓
Cancelled  Cancelled
```

- [x] `POST /api/v1/events/{id}/publish` — публикация (Draft → Published)
- [x] `POST /api/v1/events/{id}/unpublish` — снятие с публикации (Published → Draft)
- [x] `POST /api/v1/events/{id}/cancel` — отмена (любой статус → Cancelled)
- [x] `POST /api/v1/events/{id}/complete` — завершение (Published → Completed)
- [x] Нельзя публиковать событие с датой в прошлом
- [x] При отмене — все регистрации отменяются, участники уведомляются

### Редактирование

- [x] В статусе DRAFT можно редактировать всё
- [x] В статусе PUBLISHED нельзя изменять типы билетов с регистрациями
- [ ] При изменении даты/места — уведомление участникам

### Видимость

- [x] Публичные события — видны всем (если организация публичная)
- [x] Приватные события (с group_id) — только участникам группы + OWNER/MODERATOR
- [x] Участники видят список зарегистрированных только при OPEN visibility

### Запросы

- [x] Список событий организации (`GET /api/v1/events`)
- [x] Фильтры: status, date range, group_id
- [x] Публичная страница события (`GET /api/v1/events/{slug}` или `/{id}`)
- [x] Pagination через PageResponse

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [x] Код написан согласно code style проекта
- [x] Unit тесты написаны (80%+ coverage)
- [x] Integration тесты написаны
- [x] Миграции созданы с RLS
- [x] Events опубликованы в RabbitMQ
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `event-service-api`, `event-service-service`, `event-service-db`
- [ ] Frontend: создание/редактирование события, список событий
- [x] Database: таблица `events` с RLS
- [ ] Infrastructure: —

### Модель данных

См. [Domain Model - Event](../../data/domain-model.md#event)

```sql
CREATE TABLE event_service.events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    organizer_id UUID NOT NULL,
    group_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    slug VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    participants_visibility VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Moscow',
    location_type VARCHAR(20),
    location_address VARCHAR(500),
    location_url VARCHAR(500),
    cover_image_id UUID,
    settings JSONB DEFAULT '{}',
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    UNIQUE(tenant_id, slug)
);

-- RLS
ALTER TABLE event_service.events ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON event_service.events
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

### API Endpoints

```
# Events CRUD
GET    /api/v1/events                    — список событий
POST   /api/v1/events                    — создание
GET    /api/v1/events/{id}               — детали
PUT    /api/v1/events/{id}               — обновление
DELETE /api/v1/events/{id}               — удаление (soft)

# Lifecycle
POST   /api/v1/events/{id}/publish       — публикация
POST   /api/v1/events/{id}/unpublish     — снятие
POST   /api/v1/events/{id}/cancel        — отмена
POST   /api/v1/events/{id}/complete      — завершение

# Public (без авторизации для публичных событий)
GET    /api/v1/public/events/{slug}      — публичная страница
```

### RabbitMQ Events

- `event.created`
- `event.updated`
- `event.published`
- `event.cancelled` → triggers notifications to all registrations
- `event.completed`

### DTO

```java
public record CreateEventRequest(
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    String timezone,
    LocationType locationType,
    String locationAddress,
    String locationUrl,
    UUID groupId,
    ParticipantsVisibility participantsVisibility
) {}

public record EventDto(
    UUID id,
    String title,
    String description,
    String slug,
    EventStatus status,
    ParticipantsVisibility participantsVisibility,
    Instant startsAt,
    Instant endsAt,
    String timezone,
    LocationType locationType,
    String locationAddress,
    String locationUrl,
    String coverImageUrl,
    UUID organizerId,
    UUID groupId,
    Instant publishedAt,
    Instant createdAt
) {}
```

## Зависимости

### Блокирует

- [P2-010](./P2-010-events-ticket-types.md) Типы билетов
- [P2-011](./P2-011-registrations-crud.md) Регистрации
- [P2-018](./P2-018-frontend-event-page.md) Страница события

### Зависит от

- [P2-006](./P2-006-organizations-crud.md) Организации (tenant_id)
- [P2-007](./P2-007-organizations-rls.md) RLS
- [P2-008](./P2-008-groups-crud.md) Группы (для приватных событий)

## Out of Scope

- Recurring events (повторяющиеся)
- Event templates
- Multi-session events
- Integration с внешними календарями

## Заметки

- Slug генерируется из title (transliteration) + random suffix при коллизии
- Timezone важен для корректного отображения дат пользователям
- При публикации проверять наличие хотя бы одного типа билета (можно как warning)
- Markdown в description рендерится на frontend
