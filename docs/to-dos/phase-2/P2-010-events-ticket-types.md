# P2-010 Типы билетов (бесплатные)

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `review` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - События](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Каждое событие может иметь несколько типов билетов с произвольными названиями, которые задаёт организатор. Например: «VIP-место», «Стандарт», «Студенческий», «Онлайн-участие». В Phase 2 все билеты бесплатные. Организатор задаёт название, описание, количество и период продаж для каждого типа.

**Важно:** Название типа билета — произвольный текст от организатора, не enum.

### Технический контекст

- TicketType принадлежит Event
- Счётчики: quantity, sold_count, reserved_count
- Available = quantity - sold_count - reserved_count
- В Phase 2: price_cents = 0, reservation_minutes = null, prepayment_percent = null

**Связанные документы:**
- [Event Service](../../tech-stack/backend/services/event-service.md#ticket-types) — API
- [Domain Model - TicketType](../../data/domain-model.md#tickettype)
- [Functional Requirements FR-5](../../business/functional-requirements.md#fr-5-билеты)
- [User Journeys - Journey 1](../../business/user-journeys.md#journey-1-создание-и-публикация-события) — «Настраивает типы билетов: Название типа задаёт организатор»

## Цель

Реализовать CRUD типов билетов для событий с поддержкой лимитов и периодов продаж, где название типа задаётся организатором.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### CRUD

- [x] Создание типа билета (`POST /api/v1/events/{id}/ticket-types`)
- [x] Название (name) — произвольный текст от организатора (1-100 символов)
- [x] Описание (description) — опционально, произвольный текст
- [x] Количество (quantity) — опционально, null = unlimited
- [x] Период продаж (sales_start, sales_end) — опционально
- [x] Обновление (`PUT /api/v1/events/{id}/ticket-types/{typeId}`)
- [x] Удаление (`DELETE ...`) — только если нет регистраций
- [x] Деактивация (is_active = false) — вместо удаления если есть регистрации
- [x] Сортировка типов (sort_order)

### Доступность

- [x] `available` вычисляется: quantity - sold_count - reserved_count
- [x] При quantity = null — unlimited
- [x] При available = 0 — тип помечается как «Распродан»
- [x] Проверка sales_start/sales_end при регистрации
- [x] Concurrent access: предотвращение overselling (optimistic locking)

### Отображение

- [x] Список типов события (`GET /api/v1/events/{id}/ticket-types`)
- [x] Публичный endpoint показывает только активные типы в период продаж
- [x] Для организатора — все типы со статистикой

### Phase 2 ограничения

- [x] price_cents = 0 (только бесплатные)
- [x] reservation_minutes = null (без бронирования)
- [x] prepayment_percent = null (без предоплаты)

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Код написан согласно code style проекта
- [x] Unit тесты написаны
- [x] Integration тесты (concurrent registration)
- [x] Миграции созданы
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `event-service-api`, `event-service-service`, `event-service-db`
- [ ] Frontend: форма создания типов билетов
- [x] Database: таблица `ticket_types`
- [ ] Infrastructure: —

### Модель данных

```sql
CREATE TABLE event_service.ticket_types (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES event_service.events(id),
    name VARCHAR(100) NOT NULL, -- Произвольное название от организатора
    description TEXT,           -- Произвольное описание от организатора
    price_cents INTEGER NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
    quantity INTEGER, -- NULL = unlimited
    sold_count INTEGER NOT NULL DEFAULT 0,
    reserved_count INTEGER NOT NULL DEFAULT 0,
    reservation_minutes INTEGER, -- NULL = no reservation (Phase 3)
    prepayment_percent INTEGER, -- NULL = full payment (Phase 3)
    sales_start TIMESTAMP,
    sales_end TIMESTAMP,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 0, -- optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_types_event ON event_service.ticket_types(event_id);
```

### API Endpoints

```
GET    /api/v1/events/{id}/ticket-types              — список типов (для организатора)
POST   /api/v1/events/{id}/ticket-types              — создание
PUT    /api/v1/events/{id}/ticket-types/{typeId}     — обновление
DELETE /api/v1/events/{id}/ticket-types/{typeId}     — удаление
POST   /api/v1/events/{id}/ticket-types/{typeId}/deactivate — деактивация

GET    /api/v1/public/events/{slug}/ticket-types     — публичный (активные в период продаж)
```

### Optimistic Locking

```java
@Entity
public class TicketType {
    @Version
    private Integer version;

    // При регистрации:
    // 1. SELECT ... FOR UPDATE
    // 2. Check available > 0
    // 3. UPDATE sold_count = sold_count + 1
}
```

### DTO

```java
public record CreateTicketTypeRequest(
    @NotBlank @Size(min = 1, max = 100)
    String name,              // Произвольное название от организатора
    String description,       // Произвольное описание
    @Min(1)
    Integer quantity,         // NULL = unlimited
    Instant salesStart,
    Instant salesEnd,
    Integer sortOrder
) {}

public record TicketTypeDto(
    UUID id,
    String name,              // Название, заданное организатором
    String description,
    Integer priceCents,
    String currency,
    Integer quantity,
    Integer soldCount,
    Integer available,
    Instant salesStart,
    Instant salesEnd,
    Integer sortOrder,
    boolean isActive,
    boolean isSoldOut
) {}
```

### Примеры названий типов билетов

Организатор может создать любые типы:
- «VIP-место с обедом»
- «Стандартный билет»
- «Студент (со скидкой)»
- «Онлайн-участие»
- «Спикер»
- «Волонтёр»
- «Early Bird»

## Зависимости

### Блокирует

- [P2-011](./P2-011-registrations-crud.md) Регистрации

### Зависит от

- [P2-009](./P2-009-events-crud.md) События

## Out of Scope

- Платные билеты (Phase 3)
- Бронирование с таймером (Phase 3)
- Предоплата (Phase 3)
- Промокоды (Phase 4)
- Групповые билеты

## Заметки

- Важно: название — произвольный текст, НЕ enum
- Версионирование для concurrent updates
- При деактивации — тип не удаляется, просто скрывается от новых регистраций
- Рассмотреть добавление max_per_user (лимит билетов на пользователя)
