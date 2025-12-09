# P1-006 API Gateway Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
| Приоритет | `critical` |
| Связь с roadmap | [Backend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

API Gateway — единая точка входа для всех клиентских запросов. Он обеспечивает:
- Централизованную аутентификацию
- Rate limiting для защиты от злоупотреблений
- Routing к downstream сервисам
- Cross-cutting concerns (CORS, logging, correlation ID)

### Технический контекст

Gateway — **единственный** сервис на WebFlux/reactive stack. Это обусловлено природой proxy-сервиса, где неблокирующий I/O критичен для производительности.

Технологии:
- Spring Cloud Gateway
- WebFlux (reactive)
- Redis для rate limiting
- JWT validation

## Цель

Реализовать API Gateway с базовым routing, аутентификацией и rate limiting.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [ ] P1-005 завершена (common modules для JWT)
- [ ] P1-002 завершена (Docker Compose с Redis)

## Acceptance Criteria

- [ ] Создан модуль `services/gateway`
- [ ] Настроен Spring Cloud Gateway
- [ ] Реализованы маршруты ко всем сервисам:
  - [ ] `/api/v1/auth/**` → user-service
  - [ ] `/api/v1/users/**` → user-service
  - [ ] `/api/v1/organizations/**` → user-service
  - [ ] `/api/v1/events/**` → event-service
  - [ ] `/api/v1/registrations/**` → event-service
  - [ ] `/api/v1/payments/**` → payment-service
  - [ ] `/api/v1/webhooks/**` → payment-service
  - [ ] `/api/v1/notifications/**` → notification-service
  - [ ] `/api/v1/media/**` → media-service
  - [ ] `/api/v1/analytics/**` → analytics-service
- [ ] Реализован `JwtAuthenticationFilter`
- [ ] Реализован `CorrelationIdFilter`
- [ ] Настроен rate limiting через Redis
- [ ] Настроен CORS
- [ ] Реализован `GlobalErrorHandler`
- [ ] Health check endpoint работает
- [ ] Dockerfile создан
- [ ] Добавлен в docker-compose.yml
- [ ] Unit тесты для filters
- [ ] Документация сервиса обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Gateway запускается и проходит health check
- [ ] Routing работает (можно проверить с mock downstream)
- [ ] Rate limiting работает
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: services/gateway
- [ ] Frontend: не затрагивается
- [ ] Database: не затрагивается (только Redis)
- [x] Infrastructure: docker-compose.yml обновление

### Структура модуля

```
services/gateway/
├── src/
│   ├── main/
│   │   ├── java/ru/aqstream/gateway/
│   │   │   ├── GatewayApplication.java
│   │   │   ├── config/
│   │   │   │   ├── GatewayConfig.java
│   │   │   │   ├── RateLimitConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── filter/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── CorrelationIdFilter.java
│   │   │   └── handler/
│   │   │       └── GlobalErrorHandler.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
├── Dockerfile
└── build.gradle.kts
```

### build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

ext["springCloudVersion"] = "2023.0.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation(project(":common:common-security"))
    implementation(project(":common:common-api"))

    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
```

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway
  cloud:
    gateway:
      routes:
        - id: user-service-auth
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=0

        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**,/api/v1/organizations/**
          filters:
            - StripPrefix=0

        - id: event-service
          uri: lb://event-service
          predicates:
            - Path=/api/v1/events/**,/api/v1/registrations/**
          filters:
            - StripPrefix=0

        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/v1/payments/**,/api/v1/webhooks/**
          filters:
            - StripPrefix=0

        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - StripPrefix=0

        - id: media-service
          uri: lb://media-service
          predicates:
            - Path=/api/v1/media/**
          filters:
            - StripPrefix=0

        - id: analytics-service
          uri: lb://analytics-service
          predicates:
            - Path=/api/v1/analytics/**
          filters:
            - StripPrefix=0

      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenishRate: 100
              burstCapacity: 200
              requestedTokens: 1
            key-resolver: "#{@userKeyResolver}"

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

jwt:
  secret: ${JWT_SECRET}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
```

### JwtAuthenticationFilter

```java
package ru.aqstream.gateway.filter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider tokenProvider;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/events/public",
        "/api/v1/webhooks"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Пропускаем публичные endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorized(exchange);
        }

        try {
            UserPrincipal principal = tokenProvider.validateAndGetPrincipal(token);

            // Добавляем headers для downstream сервисов
            ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", principal.userId().toString())
                .header("X-Tenant-Id", principal.tenantId().toString())
                .header("X-User-Roles", String.join(",", principal.roles()))
                .build();

            return chain.filter(exchange.mutate().request(request).build());

        } catch (Exception e) {
            log.warn("Ошибка валидации JWT: {}", e.getMessage());
            return unauthorized(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY build/libs/gateway.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Rate Limit Tiers

| Тип | Лимит | Key |
|-----|-------|-----|
| Anonymous | 100 req/min | IP address |
| Authenticated | 1000 req/min | User ID |

## Зависимости

### Блокирует

- Все интеграционные тесты между сервисами
- E2E тесты

### Зависит от

- [P1-002] Docker Compose (Redis)
- [P1-004] Gradle structure
- [P1-005] Common modules (JWT)

## Out of Scope

- Service discovery (используем прямые URL в Phase 1)
- Circuit breaker (Phase 4)
- Distributed tracing (Phase 4)
- API versioning logic (сервисы сами управляют версиями)

## Заметки

- Gateway — единственный WebFlux сервис, остальные на Spring MVC
- Rate limiting хранится в Redis для persistence
- PUBLIC_PATHS можно расширять через конфигурацию
- Для локальной разработки downstream сервисы будут доступны напрямую
- В production все запросы должны идти через Gateway
- **Package naming:** используем `ru.aqstream` как base package (домен aqstream.ru)
