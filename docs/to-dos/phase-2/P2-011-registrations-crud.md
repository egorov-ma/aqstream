# P2-011 Регистрации на события

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `review` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Регистрации](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Регистрация — запись участника на событие. Участник выбирает тип билета, заполняет форму и получает подтверждение с уникальным кодом. В Phase 2 все билеты бесплатные, поэтому регистрация подтверждается сразу.

### Технический контекст

- Registration связывает User, Event, TicketType
- confirmation_code — уникальный 8-символьный код для check-in
- В Phase 2: status сразу CONFIRMED (бесплатные билеты)
- События публикуются в RabbitMQ для уведомлений

**Связанные документы:**
- [Event Service](../../tech-stack/backend/services/event-service.md#registrations) — API
- [Domain Model - Registration](../../data/domain-model.md#registration)
- [Functional Requirements FR-6](../../business/functional-requirements.md#fr-6-регистрации)
- [User Journeys - Journey 2](../../business/user-journeys.md#journey-2-регистрация-на-событие)

## Цель

Реализовать полный цикл регистрации на событие с генерацией confirmation code и отменой.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### Создание регистрации

- [x] Пользователь регистрируется на событие (`POST /api/v1/events/{id}/registrations`)
- [x] Выбирает тип билета
- [x] Заполняет: first_name, last_name, email
- [x] Поддержка custom_fields (JSONB) для дополнительных полей формы
- [x] Генерируется уникальный confirmation_code (8 символов)
- [x] Для бесплатных билетов статус сразу CONFIRMED
- [x] Один пользователь — одна регистрация на событие
- [x] sold_count в TicketType увеличивается атомарно

### Валидации

- [x] Событие в статусе PUBLISHED
- [x] Тип билета активен и в периоде продаж
- [x] Есть доступные билеты (available > 0)
- [x] Для приватных событий — пользователь член группы или организации
- [x] Email валиден

### Просмотр и отмена

- [x] Список своих регистраций (`GET /api/v1/registrations/my`)
- [x] Детали регистрации (`GET /api/v1/registrations/{id}`)
- [x] Отмена участником (`DELETE /api/v1/registrations/{id}`)
- [x] Отмена организатором (с указанием причины)
- [x] При отмене — sold_count уменьшается, место возвращается

### Для организатора

- [x] Список регистраций события (`GET /api/v1/events/{id}/registrations`)
- [x] Фильтры: ticket_type, status
- [x] Поиск по имени/email
- [ ] Экспорт (CSV) — можно в Phase 3

### События RabbitMQ

- [x] `registration.created` → уведомление участнику (билет с QR)
- [x] `registration.cancelled` → уведомление

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены (2 AC заблокированы: приватные события, CSV экспорт)
- [x] Код написан согласно code style проекта
- [x] Unit тесты написаны
- [x] Integration тесты (concurrent registration, overselling prevention)
- [x] Миграции созданы с RLS
- [x] Code review пройден
- [x] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `event-service-api`, `event-service-service`, `event-service-db`
- [ ] Frontend: форма регистрации, мои билеты
- [x] Database: таблица `registrations` с RLS
- [ ] Infrastructure: —

### Модель данных

```sql
CREATE TABLE event_service.registrations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL REFERENCES event_service.events(id),
    ticket_type_id UUID NOT NULL REFERENCES event_service.ticket_types(id),
    user_id UUID, -- NULL для анонимных регистраций
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    confirmation_code VARCHAR(8) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    custom_fields JSONB DEFAULT '{}',
    expires_at TIMESTAMP, -- для RESERVED статуса (Phase 3)
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(event_id, user_id) -- один пользователь — одна регистрация
);

-- RLS
ALTER TABLE event_service.registrations ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON event_service.registrations
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX idx_registrations_event ON event_service.registrations(event_id);
CREATE INDEX idx_registrations_user ON event_service.registrations(user_id);
CREATE INDEX idx_registrations_confirmation ON event_service.registrations(confirmation_code);
```

### API Endpoints

```
# For participants
POST   /api/v1/events/{id}/registrations    — регистрация
GET    /api/v1/registrations/my             — мои регистрации
GET    /api/v1/registrations/{id}           — детали
DELETE /api/v1/registrations/{id}           — отмена

# For organizers
GET    /api/v1/events/{id}/registrations    — список регистраций
DELETE /api/v1/events/{id}/registrations/{regId} — отмена организатором
```

### Confirmation Code Generation

```java
private String generateConfirmationCode() {
    // 8 символов: uppercase буквы + цифры (без похожих)
    String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    SecureRandom random = new SecureRandom();
    String code;
    do {
        code = random.ints(8, 0, chars.length())
            .mapToObj(chars::charAt)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    } while (registrationRepository.existsByConfirmationCode(code));
    return code;
}
```

### DTO

```java
public record CreateRegistrationRequest(
    UUID ticketTypeId,
    String firstName,
    String lastName,
    String email,
    Map<String, Object> customFields
) {}

public record RegistrationDto(
    UUID id,
    UUID eventId,
    String eventTitle,
    UUID ticketTypeId,
    String ticketTypeName,
    RegistrationStatus status,
    String confirmationCode,
    String firstName,
    String lastName,
    String email,
    Instant createdAt
) {}
```

## Зависимости

### Блокирует

- [P2-012](./P2-012-registrations-qr-code.md) QR-код для билета
- [P2-014](./P2-014-notifications-templates.md) Уведомление о регистрации

### Зависит от

- [P2-009](./P2-009-events-crud.md) События
- [P2-010](./P2-010-events-ticket-types.md) Типы билетов

## Out of Scope

- Бронирование с таймером (Phase 3)
- Платные регистрации (Phase 3)
- Лист ожидания (Phase 3)
- Групповые регистрации (Phase 4)
- Custom поля формы с валидацией (Phase 4)

## Заметки

- Confirmation code должен быть читаемым (для check-in)
- При отмене события — все регистрации отменяются автоматически
- Рассмотреть добавление idempotency_key для предотвращения дублей
- В Phase 3 добавятся статусы RESERVED, PENDING, EXPIRED
