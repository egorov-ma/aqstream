# API Guidelines

Правила проектирования REST API в AqStream.

## Общие принципы

### RESTful Design

- Ресурсо-ориентированные URL
- HTTP методы по назначению
- Stateless взаимодействие
- Консистентные ответы

### URL Structure

```text
/api/v1/{resource}
/api/v1/{resource}/{id}
/api/v1/{resource}/{id}/{sub-resource}
```

```text
✅ ПРАВИЛЬНО
GET  /api/v1/events
GET  /api/v1/events/123
GET  /api/v1/events/123/registrations
POST /api/v1/events/123/publish

❌ НЕПРАВИЛЬНО
GET  /api/v1/getEvents
GET  /api/v1/event/123
POST /api/v1/events/123/doPublish
```

## HTTP Methods

| Method | Назначение | Idempotent |
|--------|-----------|------------|
| GET | Получение ресурса | Да |
| POST | Создание ресурса | Нет |
| PUT | Полное обновление | Да |
| PATCH | Частичное обновление | Да |
| DELETE | Удаление | Да |

### Actions

Для действий используем POST с глаголом:

```text
POST /api/v1/events/{id}/publish
POST /api/v1/events/{id}/cancel
POST /api/v1/registrations/{id}/check-in
```

## Response Format

### Success Response

Данные возвращаются напрямую, без обёртки:

```json
// GET /api/v1/events/123
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Tech Conference",
  "status": "PUBLISHED",
  "startsAt": "2024-06-15T09:00:00Z",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Error Response

```json
{
  "code": "event_not_found",
  "message": "Событие не найдено",
  "details": {
    "eventId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

```json
// Validation error
{
  "code": "validation_error",
  "message": "Ошибка валидации",
  "details": {
    "title": "Заголовок обязателен",
    "startsAt": "Дата должна быть в будущем"
  }
}
```

### HTTP Status Codes

| Code | Когда использовать |
|------|-------------------|
| 200 | Успешный GET, PUT, PATCH |
| 201 | Успешный POST (создание) |
| 204 | Успешный DELETE |
| 400 | Ошибка валидации |
| 401 | Не аутентифицирован |
| 403 | Нет прав доступа |
| 404 | Ресурс не найден |
| 409 | Конфликт (duplicate) |
| 422 | Business rule violation |
| 429 | Rate limit exceeded |
| 500 | Внутренняя ошибка |

## Pagination

### Request

```text
GET /api/v1/events?page=0&size=20&sort=startsAt,desc
```

| Параметр | Default | Max | Описание |
|----------|---------|-----|----------|
| page | 0 | — | Номер страницы (0-based) |
| size | 20 | 100 | Размер страницы |
| sort | — | — | Поле и направление |

### Response

```json
{
  "data": [
    { "id": "...", "title": "Event 1" },
    { "id": "...", "title": "Event 2" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 157,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

## Filtering

### Query Parameters

```text
GET /api/v1/events?status=PUBLISHED&organizerId=123
GET /api/v1/events?startsAfter=2024-06-01T00:00:00Z
GET /api/v1/events?search=conference
```

### Implementation

```java
@GetMapping
public PageResponse<EventDto> list(
    @RequestParam(required = false) EventStatus status,
    @RequestParam(required = false) UUID organizerId,
    @RequestParam(required = false) Instant startsAfter,
    @RequestParam(required = false) String search,
    @PageableDefault(size = 20) Pageable pageable
) {
    return PageResponse.of(eventService.findAll(
        EventFilter.builder()
            .status(status)
            .organizerId(organizerId)
            .startsAfter(startsAfter)
            .search(search)
            .build(),
        pageable
    ));
}
```

## DateTime

### Format

ISO 8601 с timezone (всегда UTC в API):

```text
2024-06-15T09:00:00Z
```

### Storage

UTC в базе данных:

```java
@Column(name = "starts_at")
private Instant startsAt;  // Всегда UTC
```

### Timezone

Клиент отвечает за конвертацию в локальную таймзону.

Событие хранит timezone для отображения:

```json
{
  "startsAt": "2024-06-15T06:00:00Z",
  "timezone": "Europe/Moscow"
}
```

## DTO Naming

| Тип | Pattern | Пример |
|-----|---------|--------|
| Создание | `Create{Entity}Request` | `CreateEventRequest` |
| Обновление | `Update{Entity}Request` | `UpdateEventRequest` |
| Ответ (полный) | `{Entity}Dto` | `EventDto` |
| Ответ (детальный) | `{Entity}DetailDto` | `EventDetailDto` |
| Ответ (краткий) | `{Entity}SummaryDto` | `EventSummaryDto` |

```java
// Request DTOs
public record CreateEventRequest(
    @NotBlank String title,
    String description,
    @NotNull @Future Instant startsAt
) {}

public record UpdateEventRequest(
    @NotBlank String title,
    String description,
    @NotNull @Future Instant startsAt
) {}

// Response DTOs
public record EventDto(
    UUID id,
    String title,
    String description,
    EventStatus status,
    Instant startsAt,
    Instant createdAt
) {}

public record EventSummaryDto(
    UUID id,
    String title,
    EventStatus status,
    Instant startsAt
) {}
```

## Versioning

### URL Versioning

```text
/api/v1/events
/api/v2/events
```

### Когда создавать v2

- Удаление поля из response
- Изменение типа поля
- Изменение семантики endpoint

### Стратегия миграции

1. Создать v2 endpoint
2. Добавить deprecation notice в v1
3. Дать время на миграцию клиентов
4. Удалить v1 (опционально)

## Headers

### Request Headers

| Header | Описание |
|--------|----------|
| Authorization | `Bearer {jwt_token}` |
| Content-Type | `application/json` |
| Accept-Language | `ru`, `en` |
| X-Idempotency-Key | UUID для идемпотентных POST |

### Response Headers

| Header | Описание |
|--------|----------|
| X-Correlation-ID | ID запроса для трейсинга |
| X-RateLimit-Limit | Лимит запросов |
| X-RateLimit-Remaining | Осталось запросов |
| Retry-After | Секунды до retry (при 429) |

## Idempotency

Для критичных POST операций требуется `X-Idempotency-Key`:

```java
@PostMapping("/registrations")
public ResponseEntity<RegistrationDto> create(
    @RequestHeader("X-Idempotency-Key") UUID idempotencyKey,
    @Valid @RequestBody CreateRegistrationRequest request
) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(registrationService.createIdempotent(idempotencyKey, request));
}
```

Операции с idempotency:
- Создание регистрации
- Создание платежа
- Возврат платежа

## Validation

### Bean Validation

```java
public record CreateEventRequest(
    @NotBlank(message = "Заголовок обязателен")
    @Size(max = 255, message = "Заголовок не более 255 символов")
    String title,
    
    @Size(max = 10000, message = "Описание не более 10000 символов")
    String description,
    
    @NotNull(message = "Дата начала обязательна")
    @Future(message = "Дата должна быть в будущем")
    Instant startsAt
) {}
```

### Custom Validation

```java
@Documented
@Constraint(validatedBy = SlugValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSlug {
    String message() default "Некорректный URL slug";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

## OpenAPI Documentation

### Annotations

```java
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Управление событиями")
public class EventController {

    @Operation(
        summary = "Получить событие",
        description = "Возвращает событие по ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успешно"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @GetMapping("/{id}")
    public EventDto getById(
        @Parameter(description = "ID события") 
        @PathVariable UUID id
    ) {
        return eventService.findById(id);
    }
}
```

### Schema

```java
@Schema(description = "Событие")
public record EventDto(
    @Schema(description = "ID события", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Название", example = "Tech Conference 2024")
    String title,
    
    @Schema(description = "Статус", example = "PUBLISHED")
    EventStatus status
) {}
```

## Rate Limiting

Реализован на API Gateway:

| Endpoint | Limit | Scope |
|----------|-------|-------|
| Public | 100 req/min | Per IP |
| Authenticated | 1000 req/min | Per user |
| File upload | 10 req/min | Per user |

Response при превышении:

```json
// 429 Too Many Requests
{
  "code": "rate_limit_exceeded",
  "message": "Превышен лимит запросов. Повторите через 60 секунд.",
  "retryAfter": 60
}
```

## Дальнейшее чтение

- [Backend Architecture](./architecture.md)
- [Common Library](./common-library.md)
- [API Documentation](./api/README.md)
