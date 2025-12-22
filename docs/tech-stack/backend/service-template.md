# Service Template

Шаблон для создания микросервиса в AqStream.

## Структура сервиса

```text
{service-name}/
├── {service-name}-api/       # DTO, Events, Exceptions (shared)
├── {service-name}-service/   # Controllers, Services, Mappers
├── {service-name}-db/        # Entities, Repositories, Migrations
└── {service-name}-client/    # Feign client (опционально)
```

## Модули

### {service-name}-api

Публичные контракты, используемые другими сервисами.

```text
api/dto/           → CreateXxxRequest, UpdateXxxRequest, XxxDto
api/event/         → XxxCreatedEvent extends DomainEvent
api/exception/     → XxxNotFoundException extends EntityNotFoundException
```

### {service-name}-db

Persistence layer.

```text
db/entity/         → XxxEntity extends TenantAwareEntity
db/repository/     → XxxRepository extends JpaRepository
db/changelog/      → Liquibase migrations
```

### {service-name}-service

Business logic и web layer.

```text
controller/        → XxxController (@RestController)
service/           → XxxService (@Service, @Transactional)
mapper/            → XxxMapper (@Mapper componentModel="spring")
config/            → Configuration classes
```

## Ключевые паттерны

**Controller:**

```java
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {
    private final ExampleService service;

    @PostMapping
    public ResponseEntity<ExampleDto> create(@Valid @RequestBody CreateExampleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}
```

**Service:**

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {
    private final ExampleRepository repository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ExampleDto create(CreateExampleRequest request) {
        ExampleEntity entity = mapper.toEntity(request);
        ExampleEntity saved = repository.save(entity);
        eventPublisher.publish(new ExampleCreatedEvent(saved.getId()));
        return mapper.toDto(saved);
    }
}
```

## Checklist нового сервиса

- [ ] Структура модулей (api, service, db)
- [ ] build.gradle.kts для каждого модуля
- [ ] DTO в api модуле
- [ ] Entity + Repository в db модуле
- [ ] Liquibase миграции с rollback
- [ ] Controller + Service + Mapper
- [ ] application.yml (порт, БД, RabbitMQ)
- [ ] Добавить в docker-compose.yml
- [ ] Маршрут в Gateway
- [ ] Integration тесты
- [ ] Документация в docs/tech-stack/backend/services/
