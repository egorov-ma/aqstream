# P1-005 Реализация Common Modules

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
| Приоритет | `critical` |
| Связь с roadmap | [Backend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Common modules — фундамент для всех микросервисов. Они обеспечивают:
- Единообразную обработку ошибок
- Централизованную аутентификацию и multi-tenancy
- Базовые сущности с аудитом
- Надёжную публикацию событий через Outbox pattern
- Переиспользуемые тестовые утилиты

### Технический контекст

6 модулей:
- `common-api` — DTO, exceptions, events
- `common-security` — JWT, TenantContext, filters
- `common-data` — BaseEntity, TenantAwareEntity, auditing
- `common-messaging` — Outbox pattern, EventPublisher
- `common-web` — GlobalExceptionHandler, CorrelationIdFilter
- `common-test` — Testcontainers setup, fixtures

## Цель

Реализовать все common модули с базовым функционалом, готовым для использования в сервисах.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [ ] P1-004 завершена

## Acceptance Criteria

### common-api

- [ ] `PageResponse<T>` record для пагинации
- [ ] `ErrorResponse` record (code, message, details)
- [ ] `DomainEvent` абстрактный класс для событий
- [ ] `AqStreamException` базовое исключение
- [ ] `EntityNotFoundException` для 404
- [ ] `ValidationException` для 400
- [ ] `ConflictException` для 409

### common-security

- [ ] `TenantContext` (ThreadLocal для tenant_id)
- [ ] `UserPrincipal` record с данными пользователя
- [ ] `JwtTokenProvider` для генерации/валидации JWT
- [ ] `SecurityContext` utility для получения текущего пользователя
- [ ] Unit тесты для JwtTokenProvider

### common-data

- [ ] `BaseEntity` (id UUID, createdAt, updatedAt)
- [ ] `TenantAwareEntity` extends BaseEntity (+tenantId)
- [ ] `SoftDeletableEntity` extends TenantAwareEntity (+deletedAt)
- [ ] `AuditingConfig` для автозаполнения createdAt/updatedAt
- [ ] `TenantEntityListener` для автозаполнения tenantId

### common-messaging

- [ ] `OutboxMessage` entity
- [ ] `OutboxRepository`
- [ ] `EventPublisher` interface и implementation
- [ ] `OutboxProcessor` scheduled job

### common-web

- [ ] `GlobalExceptionHandler` (@ControllerAdvice)
- [ ] `CorrelationIdFilter` для X-Correlation-ID
- [ ] `TenantContextFilter` для установки TenantContext
- [ ] `RequestLoggingFilter` для логирования запросов

### common-test

- [ ] `@IntegrationTest` composite annotation
- [ ] `PostgresTestContainer` singleton
- [ ] `RabbitMQTestContainer` singleton
- [ ] `TestFixtures` утилиты

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Код соответствует code style (checkstyle проходит)
- [ ] Unit тесты написаны (coverage > 80%)
- [ ] Javadoc для публичных классов
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: все common модули
- [ ] Frontend: не затрагивается
- [ ] Database: не затрагивается напрямую
- [ ] Infrastructure: не затрагивается

### common-api классы

**Package:** `ru.aqstream.common.api`

```java
// PageResponse.java
public record PageResponse<T>(
    List<T> data,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T> PageResponse<T> of(Page<T> page) { ... }
}

// ErrorResponse.java
public record ErrorResponse(
    String code,
    String message,
    Map<String, Object> details
) {}

// DomainEvent.java
public abstract class DomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();

    public abstract String getEventType();
}

// Exceptions
public class AqStreamException extends RuntimeException { ... }
public class EntityNotFoundException extends AqStreamException { ... }
public class ValidationException extends AqStreamException { ... }
public class ConflictException extends AqStreamException { ... }
```

### common-security классы

**Package:** `ru.aqstream.common.security`

```java
// TenantContext.java
public final class TenantContext {
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) { ... }
    public static UUID getTenantId() { ... } // throws if not set
    public static Optional<UUID> getTenantIdOptional() { ... }
    public static void clear() { ... }
}

// UserPrincipal.java
public record UserPrincipal(
    UUID userId,
    String email,
    UUID tenantId,
    Set<String> roles
) {}

// JwtTokenProvider.java
@Component
public class JwtTokenProvider {
    public String generateAccessToken(UserPrincipal principal) { ... }
    public String generateRefreshToken(UUID userId) { ... }
    public UserPrincipal validateAndGetPrincipal(String token) { ... }
}
```

### common-data классы

**Package:** `ru.aqstream.common.data`

```java
// BaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

// TenantAwareEntity.java
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, TenantEntityListener.class})
public abstract class TenantAwareEntity extends BaseEntity {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}

// SoftDeletableEntity.java
@MappedSuperclass
public abstract class SoftDeletableEntity extends TenantAwareEntity {
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() { return deletedAt != null; }
    public void softDelete() { this.deletedAt = Instant.now(); }
}
```

### common-messaging классы

**Package:** `ru.aqstream.common.messaging`

```java
// OutboxMessage.java
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}

// EventPublisher.java
@Component
@RequiredArgsConstructor
public class EventPublisher {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) { ... }
}
```

### common-web классы

**Package:** `ru.aqstream.common.web`

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) { ... }
}

// CorrelationIdFilter.java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(...) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        // ...
    }
}
```

### common-test классы

**Package:** `ru.aqstream.common.test`

```java
// IntegrationTest.java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
public @interface IntegrationTest {}

// PostgresTestContainer.java
public class PostgresTestContainer {
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

## Зависимости

### Блокирует

- [P1-006] API Gateway setup
- Все задачи сервисов в Phase 2

### Зависит от

- [P1-004] Gradle multi-module структура

## Out of Scope

- Реализация сервисов
- Liquibase миграции для OutboxMessage (будут в конкретных сервисах)
- Redis caching (Phase 4)
- Production-ready конфигурация JWT

## Заметки

- TenantContext использует ThreadLocal, требует очистки в filter
- Outbox pattern критичен для reliable event publishing
- Все exceptions наследуются от AqStreamException для единообразной обработки
- Testcontainers используют singleton pattern для ускорения тестов
- Комментарии в коде на русском согласно CLAUDE.md
