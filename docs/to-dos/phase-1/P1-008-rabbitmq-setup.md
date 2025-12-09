# P1-008 RabbitMQ Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
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
- [ ] P1-002 завершена (Docker Compose с RabbitMQ)

## Acceptance Criteria

- [ ] RabbitMQ definitions.json создан
- [ ] Topic exchange `aqstream.events` настроен
- [ ] Queues созданы для каждого consumer
- [ ] Dead letter exchange и queues настроены
- [ ] Spring AMQP конфигурация в common-messaging
- [ ] `RabbitMQConfig` базовый класс
- [ ] `EventListener` базовый интерфейс
- [ ] Интеграционные тесты с Testcontainers
- [ ] Management UI доступен на порту 15672
- [ ] Документация обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Сообщения успешно публикуются и потребляются
- [ ] Dead letter queue работает
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: common-messaging, конфигурация сервисов
- [ ] Frontend: не затрагивается
- [ ] Database: не затрагивается
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

### definitions.json

```json
{
  "rabbit_version": "3.12.0",
  "users": [
    {
      "name": "guest",
      "password_hash": "guest",
      "tags": "administrator"
    }
  ],
  "vhosts": [
    {"name": "/"}
  ],
  "permissions": [
    {
      "user": "guest",
      "vhost": "/",
      "configure": ".*",
      "write": ".*",
      "read": ".*"
    }
  ],
  "exchanges": [
    {
      "name": "aqstream.events",
      "vhost": "/",
      "type": "topic",
      "durable": true,
      "auto_delete": false
    },
    {
      "name": "aqstream.events.dlx",
      "vhost": "/",
      "type": "topic",
      "durable": true,
      "auto_delete": false
    }
  ],
  "queues": [
    {
      "name": "notification.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-dead-letter-exchange": "aqstream.events.dlx",
        "x-dead-letter-routing-key": "notification.dlx"
      }
    },
    {
      "name": "analytics.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-dead-letter-exchange": "aqstream.events.dlx",
        "x-dead-letter-routing-key": "analytics.dlx"
      }
    },
    {
      "name": "event-service.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-dead-letter-exchange": "aqstream.events.dlx",
        "x-dead-letter-routing-key": "event-service.dlx"
      }
    },
    {
      "name": "dlx.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false
    }
  ],
  "bindings": [
    {
      "source": "aqstream.events",
      "vhost": "/",
      "destination": "notification.queue",
      "destination_type": "queue",
      "routing_key": "notification.#"
    },
    {
      "source": "aqstream.events",
      "vhost": "/",
      "destination": "notification.queue",
      "destination_type": "queue",
      "routing_key": "registration.#"
    },
    {
      "source": "aqstream.events",
      "vhost": "/",
      "destination": "notification.queue",
      "destination_type": "queue",
      "routing_key": "event.#"
    },
    {
      "source": "aqstream.events",
      "vhost": "/",
      "destination": "analytics.queue",
      "destination_type": "queue",
      "routing_key": "#"
    },
    {
      "source": "aqstream.events",
      "vhost": "/",
      "destination": "event-service.queue",
      "destination_type": "queue",
      "routing_key": "payment.#"
    },
    {
      "source": "aqstream.events.dlx",
      "vhost": "/",
      "destination": "dlx.queue",
      "destination_type": "queue",
      "routing_key": "#"
    }
  ]
}
```

### RabbitMQConfig

**Package:** `ru.aqstream.common.messaging.config`

```java
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE = "aqstream.events";
    public static final String DLX_EXCHANGE = "aqstream.events.dlx";

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter(objectMapper));
        template.setExchange(EVENTS_EXCHANGE);
        return template;
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter(objectMapper));
        factory.setDefaultRequeueRejected(false); // Отправлять в DLQ при ошибке
        factory.setPrefetchCount(10);
        return factory;
    }
}
```

### EventPublisher (обновлённый)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Публикует событие через Outbox pattern.
     * Событие сохраняется в БД в той же транзакции.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        try {
            OutboxMessage message = new OutboxMessage();
            message.setEventType(event.getEventType());
            message.setPayload(objectMapper.writeValueAsString(event));
            message.setCreatedAt(Instant.now());

            outboxRepository.save(message);
            log.debug("Событие добавлено в outbox: type={}", event.getEventType());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации события", e);
        }
    }
}
```

### OutboxProcessor

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 1000) // Каждую секунду
    @Transactional
    public void processOutbox() {
        List<OutboxMessage> pending = outboxRepository
            .findByProcessedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 100));

        for (OutboxMessage message : pending) {
            try {
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENTS_EXCHANGE,
                    message.getEventType(),
                    message.getPayload()
                );

                message.setProcessedAt(Instant.now());
                outboxRepository.save(message);

                log.debug("Событие опубликовано: type={}, id={}",
                    message.getEventType(), message.getId());

            } catch (Exception e) {
                log.error("Ошибка публикации события: id={}, error={}",
                    message.getId(), e.getMessage());
                // Событие останется в outbox и будет переотправлено
            }
        }
    }
}
```

### Пример Listener

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.queue")
    public void handleEvent(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody());

        log.info("Получено событие: routingKey={}", routingKey);

        switch (routingKey) {
            case "registration.created" -> handleRegistrationCreated(body);
            case "event.cancelled" -> handleEventCancelled(body);
            default -> log.warn("Неизвестный тип события: {}", routingKey);
        }
    }

    private void handleRegistrationCreated(String payload) {
        // Отправка email с подтверждением
    }

    private void handleEventCancelled(String payload) {
        // Массовая рассылка уведомлений
    }
}
```

### docker-compose.yml обновление

```yaml
rabbitmq:
  image: rabbitmq:3.12-management-alpine
  ports:
    - "5672:5672"
    - "15672:15672"
  volumes:
    - rabbitmq-data:/var/lib/rabbitmq
    - ./docker/rabbitmq/definitions.json:/etc/rabbitmq/definitions.json
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "check_running"]
    interval: 30s
    timeout: 10s
    retries: 5
```

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
