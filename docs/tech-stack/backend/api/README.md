# API Documentation

Документация REST API сервисов AqStream.

## Доступ к документации

### Development

Каждый сервис предоставляет интерактивную документацию:

| Сервис | Swagger UI | OpenAPI JSON |
|--------|------------|--------------|
| Gateway | http://localhost:8080/swagger-ui.html | /v3/api-docs |
| User Service | http://localhost:8081/swagger-ui.html | /v3/api-docs |
| Event Service | http://localhost:8082/swagger-ui.html | /v3/api-docs |
| Payment Service | http://localhost:8083/swagger-ui.html | /v3/api-docs |
| Notification Service | http://localhost:8084/swagger-ui.html | /v3/api-docs |
| Media Service | http://localhost:8085/swagger-ui.html | /v3/api-docs |
| Analytics Service | http://localhost:8086/swagger-ui.html | /v3/api-docs |

### Production

- Swagger UI: https://api.aqstream.ru/swagger-ui.html
- ReDoc: https://api.aqstream.ru/redoc

## Структура документации

```text
docs/tech-stack/backend/api/
├── README.md           # Этот файл
├── specs/              # OpenAPI спецификации (YAML)
│   ├── gateway.yaml
│   ├── user-service.yaml
│   ├── event-service.yaml
│   └── ...
├── redoc/              # ReDoc статические HTML
│   └── *.html
└── swagger/            # Swagger UI конфигурация
```

## Генерация документации

### Скачать OpenAPI specs

```bash
# Запустить сервисы
make docker-up

# Скачать спецификации
./docs/_internal/generators/generate-openapi.sh
```

Скрипт генерации:

```bash
#!/bin/bash
# docs/_internal/generators/generate-openapi.sh

SERVICES=(
    "gateway:8080"
    "user-service:8081"
    "event-service:8082"
    "payment-service:8083"
    "notification-service:8084"
    "media-service:8085"
    "analytics-service:8086"
)

OUTPUT_DIR="docs/tech-stack/backend/api/specs"
mkdir -p "$OUTPUT_DIR"

for service in "${SERVICES[@]}"; do
    name="${service%%:*}"
    port="${service##*:}"
    
    echo "Скачиваем $name..."
    curl -s "http://localhost:$port/v3/api-docs.yaml" > "$OUTPUT_DIR/$name.yaml"
done

echo "Готово!"
```

### Генерация ReDoc HTML

```bash
./docs/_internal/generators/generate-redoc.sh
```

```bash
#!/bin/bash
# docs/_internal/generators/generate-redoc.sh

SPECS_DIR="docs/tech-stack/backend/api/specs"
OUTPUT_DIR="docs/tech-stack/backend/api/redoc"
mkdir -p "$OUTPUT_DIR"

for spec in "$SPECS_DIR"/*.yaml; do
    name=$(basename "$spec" .yaml)
    echo "Генерируем ReDoc для $name..."
    npx @redocly/cli build-docs "$spec" -o "$OUTPUT_DIR/$name.html"
done

echo "Готово!"
```

## OpenAPI Annotations

### Controller

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
        @ApiResponse(
            responseCode = "200",
            description = "Успешно",
            content = @Content(schema = @Schema(implementation = EventDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Событие не найдено",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public EventDto getById(
        @Parameter(description = "ID события", required = true)
        @PathVariable UUID id
    ) {
        return eventService.findById(id);
    }
}
```

### DTO

```java
@Schema(description = "Событие")
public record EventDto(
    @Schema(description = "ID события", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Название", example = "Tech Conference 2024", maxLength = 255)
    String title,
    
    @Schema(description = "Описание", example = "Ежегодная конференция...")
    String description,
    
    @Schema(description = "Статус", example = "PUBLISHED", allowableValues = {"DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED"})
    EventStatus status,
    
    @Schema(description = "Дата начала", example = "2024-06-15T09:00:00Z")
    Instant startsAt
) {}
```

### Request

```java
@Schema(description = "Запрос на создание события")
public record CreateEventRequest(
    @Schema(description = "Название", example = "Tech Conference 2024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    String title,
    
    @Schema(description = "Описание")
    String description,
    
    @Schema(description = "Дата начала", example = "2024-06-15T09:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Future
    Instant startsAt
) {}
```

## Конфигурация SpringDoc

### application.yml

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
  info:
    title: Event Service API
    description: API для управления событиями
    contact:
      name: AqStream Team
      url: https://github.com/egorov-ma/aqstream
```

### OpenApiConfig.java

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Event Service API")
                .description("API для управления событиями")
                .contact(new Contact()
                    .name("AqStream Team")
                    .url("https://github.com/egorov-ma/aqstream")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

## Валидация спецификаций

```bash
./docs/_internal/validators/validate-openapi.sh
```

```bash
#!/bin/bash
# docs/_internal/validators/validate-openapi.sh

SPECS_DIR="docs/tech-stack/backend/api/specs"

for spec in "$SPECS_DIR"/*.yaml; do
    echo "Валидация $(basename $spec)..."
    npx @stoplight/spectral-cli lint "$spec" --ruleset docs/_internal/validators/spectral.yaml
done
```

### spectral.yaml

```yaml
extends: [[spectral:oas, recommended]]

rules:
  operation-operationId:
    severity: error
  operation-description:
    severity: warn
  operation-tags:
    severity: error
  info-contact:
    severity: warn
```

## Дальнейшее чтение

- [API Guidelines](../api-guidelines.md) — правила проектирования API
- [Backend Architecture](../architecture.md) — архитектура backend
