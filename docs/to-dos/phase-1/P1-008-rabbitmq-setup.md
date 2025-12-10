# P1-008 RabbitMQ Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `high` |
| Связь с roadmap | [Backend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Event-driven архитектура AqStream требует надёжной передачи событий между сервисами:
- Асинхронная коммуникация снижает связанность сервисов
- Outbox pattern гарантирует доставку событий
- Очереди позволяют обрабатывать нагрузку асинхронно

Примеры событий:
- `registration.created` → Email с подтверждением
- `event.cancelled` → Массовые уведомления
- `payment.completed` → Обновление статуса регистрации

### Технический контекст

RabbitMQ используется как message broker:
- Topic exchanges для routing по типу события
- Durable queues для надёжности
- Dead letter queues для failed messages
- Отложенные сообщения для retry

## Цель

Настроить RabbitMQ с exchanges, queues и bindings для всех сервисов.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-002 завершена (Docker Compose с RabbitMQ)

## Acceptance Criteria

- [x] RabbitMQ definitions.json создан
- [x] Topic exchange `aqstream.events` настроен
- [x] Queues созданы для каждого consumer
- [x] Dead letter exchange и queues настроены
- [x] Spring AMQP конфигурация в common-messaging
- [x] `RabbitMQConfig` базовый класс
- [x] `EventPublisher` и `OutboxProcessor` реализованы
- [x] Интеграционные тесты с Testcontainers
- [x] Management UI доступен на порту 15672
- [x] Документация обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Сообщения успешно публикуются и потребляются
- [x] Dead letter queue работает
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: common-messaging, конфигурация сервисов
- [ ] Frontend: не затрагивается
- [x] Database: миграция outbox_messages для Outbox pattern
- [x] Infrastructure: RabbitMQ configuration

### Структура файлов

```
docker/
└── rabbitmq/
    └── definitions.json

common/
└── common-messaging/
    └── src/main/java/ru/aqstream/common/messaging/
        ├── config/
        │   └── RabbitMQConfig.java
        ├── publisher/
        │   ├── EventPublisher.java
        │   └── OutboxProcessor.java
        └── listener/
            └── BaseEventListener.java
```

### Exchange и Queue Topology

```
aqstream.events (topic exchange)
    │
    ├── notification.# ──────────────► notification.queue
    │                                   (Notification Service)
    │
    ├── analytics.# ─────────────────► analytics.queue
    │                                   (Analytics Service)
    │
    ├── event.# ─────────────────────► event-service.queue
    │                                   (Event Service - для payment events)
    │
    └── *.dlx ───────────────────────► dlx.queue
                                        (Dead Letter Queue)

aqstream.events.dlx (dead letter exchange)
    └── # ───────────────────────────► dlx.queue
```

### Реализованные компоненты

Код реализован в следующих файлах:

| Компонент | Путь |
|-----------|------|
| RabbitMQ definitions | `docker/rabbitmq/definitions.json` |
| RabbitMQConfig | `common/common-messaging/src/main/java/.../config/RabbitMQConfig.java` |
| EventPublisher | `common/common-messaging/src/main/java/.../EventPublisher.java` |
| OutboxProcessor | `common/common-messaging/src/main/java/.../OutboxProcessor.java` |
| OutboxMessage | `common/common-messaging/src/main/java/.../OutboxMessage.java` |
| Миграция outbox | `services/{event,user,payment}-service/...-db/src/main/resources/db/changelog/changes/00X-create-outbox-table.xml` |
| Интеграционные тесты | `common/common-messaging/src/test/java/.../RabbitMQIntegrationTest.java` |

### Event Routing Keys

| Event | Routing Key | Consumers |
|-------|-------------|-----------|
| RegistrationCreated | `registration.created` | notification, analytics |
| RegistrationCancelled | `registration.cancelled` | notification, analytics |
| EventPublished | `event.published` | analytics |
| EventCancelled | `event.cancelled` | notification, analytics |
| PaymentCompleted | `payment.completed` | event-service, analytics |
| PaymentFailed | `payment.failed` | event-service |
| CheckInCompleted | `checkin.completed` | analytics |

## Зависимости

### Блокирует

- Notification Service (Phase 2)
- Analytics Service (Phase 3)
- Асинхронная коммуникация между сервисами

### Зависит от

- [P1-002] Docker Compose
- [P1-005] Common modules (OutboxMessage entity)

## Out of Scope

- Retry с exponential backoff (можно добавить позже)
- Delayed message exchange
- Priority queues
- RabbitMQ clustering (production concern)

## Заметки

- Guest credentials только для development
- DLQ нужен для анализа failed messages
- Outbox processor использует polling (1 сек) — достаточно для MVP
- prefetchCount=10 для баланса throughput и memory
- Analytics слушает все события (#) для полной картины
