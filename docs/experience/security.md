# Security

Политики и практики безопасности AqStream.

## Аутентификация

### JWT Tokens

| Тип | Срок жизни | Хранение |
|-----|-----------|----------|
| Access Token | 15 минут | Memory (frontend) |
| Refresh Token | 7 дней | HttpOnly cookie / DB |

```java
// JWT payload
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "tenantId": "org-uuid",
  "roles": ["ADMIN"],
  "iat": 1623456789,
  "exp": 1623457689
}
```

### Алгоритм

- **HS256** (HMAC-SHA256) для подписи
- Секретный ключ минимум 256 бит
- Ротация ключей: планируется

### Password Policy

- Минимум 8 символов
- Обязательно: буквы и цифры
- Хранение: bcrypt (cost factor 12)

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

## Авторизация

### Role-Based Access Control (RBAC)

| Роль | Уровень | Права |
|------|---------|-------|
| OWNER | Организация | Полный доступ |
| ADMIN | Организация | Управление, кроме удаления org |
| MANAGER | Организация | Управление событиями |
| VIEWER | Организация | Только просмотр |

### Resource-Based Access

```java
@PreAuthorize("@eventSecurity.canEdit(#eventId)")
public EventDto update(UUID eventId, UpdateEventRequest request) {
    // ...
}
```

## Multi-Tenancy Security

### Row Level Security (PostgreSQL)

```sql
-- Включение RLS
ALTER TABLE events ENABLE ROW LEVEL SECURITY;

-- Политика изоляции
CREATE POLICY tenant_isolation ON events
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
```

### Tenant Context

```java
@Component
public class TenantFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain chain) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null) {
            TenantContext.setTenantId(UUID.fromString(tenantId));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
```

## API Security

### Rate Limiting

| Тип | Лимит |
|-----|-------|
| Anonymous | 100 req/min per IP |
| Authenticated | 1000 req/min per user |
| File upload | 10 req/min per user |

### Input Validation

```java
public record CreateEventRequest(
    @NotBlank
    @Size(max = 255)
    String title,
    
    @Size(max = 10000)
    String description,
    
    @NotNull
    @Future
    Instant startsAt
) {}
```

### SQL Injection Prevention

- Использование JPA/Hibernate с параметризованными запросами
- Запрет native queries без параметров

```java
// ✅ Безопасно
@Query("SELECT e FROM Event e WHERE e.title = :title")
List<Event> findByTitle(@Param("title") String title);

// ❌ Опасно
@Query(value = "SELECT * FROM events WHERE title = '" + title + "'", nativeQuery = true)
```

### XSS Prevention

Frontend:
- React автоматически экранирует вывод
- CSP headers

Backend:
- Санитизация HTML в Markdown

### CORS

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://aqstream.com",
            "https://staging.aqstream.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

## Data Protection

### Encryption

| Данные | At Rest | In Transit |
|--------|---------|------------|
| Database | PostgreSQL encryption | TLS |
| Files (MinIO) | Server-side encryption | TLS |
| Redis | — | TLS (production) |

### PII (Personal Identifiable Information)

Хранимые PII:
- Email
- Имя, фамилия
- (опционально) Телефон

Правила:
- Минимизация хранения
- Soft delete с retention policy
- Логи без PII

```java
// Логирование без PII
log.info("Пользователь вошёл: userId={}", userId);  // ✅
log.info("Пользователь вошёл: email={}", email);    // ❌
```

### Payment Data

- Номера карт **не хранятся**
- Используются токены платёжных провайдеров (Stripe, ЮKassa)
- PCI DSS: провайдер отвечает за compliance

## Security Headers

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'"))
            .frameOptions(frame -> frame.deny())
            .xssProtection(xss -> xss.enable())
            .contentTypeOptions(content -> {})
        );
        return http.build();
    }
}
```

## Secrets Management

### Environment Variables

```bash
# Secrets через environment variables
JWT_SECRET=...
DATABASE_PASSWORD=...
STRIPE_API_KEY=...
```

### Запрещено

- Secrets в коде
- Secrets в git
- Secrets в логах

## Vulnerability Management

### Dependency Scanning

```bash
# Backend
./gradlew dependencyCheckAnalyze

# Frontend
pnpm audit
```

### Security Updates

- Dependabot включен
- Critical updates — в течение 24 часов
- High updates — в течение 7 дней

## Incident Response

При обнаружении security инцидента:

1. Изолировать затронутые компоненты
2. Оценить масштаб
3. Уведомить команду
4. Исправить уязвимость
5. Провести post-mortem

См. [Incident Response Runbook](../operations/runbooks/incident-response.md)

## Compliance

### GDPR

- Право на удаление данных (soft delete → hard delete по запросу)
- Право на экспорт данных
- Согласие на обработку

### Cookie Policy

- Минимум cookies
- HttpOnly для session cookies
- Secure flag в production

## Security Checklist

### Перед релизом

- [ ] Dependency scan пройден
- [ ] Секреты не в коде
- [ ] Input validation везде
- [ ] SQL injection невозможен
- [ ] XSS невозможен
- [ ] Rate limiting настроен
- [ ] CORS настроен корректно
- [ ] Security headers настроены

## Дальнейшее чтение

- [API Guidelines](../tech-stack/backend/api-guidelines.md) — правила API
- [User Service](../tech-stack/backend/services/user-service.md) — аутентификация
