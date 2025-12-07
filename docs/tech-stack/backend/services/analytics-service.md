# Analytics Service

Analytics Service отвечает за сбор метрик и отчётность.

## Обзор

| Параметр | Значение |
|----------|----------|
| Порт | 8086 |
| База данных | postgres-analytics (dedicated, TimescaleDB) |
| Схема | analytics_service |

## Ответственности

- Сбор событий (event tracking)
- Агрегация метрик
- Дашборды для организаторов
- Экспорт отчётов

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/analytics/track` | Трекинг события |
| GET | `/api/v1/analytics/events/{eventId}/dashboard` | Дашборд события |
| GET | `/api/v1/analytics/events/{eventId}/funnel` | Воронка |
| GET | `/api/v1/analytics/organizations/{orgId}/report` | Отчёт организации |
| POST | `/api/v1/analytics/export` | Экспорт данных |

## Модель данных

```sql
-- TimescaleDB hypertable для событий
CREATE TABLE analytics_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_id UUID,
    user_id UUID,
    properties JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable('analytics_events', 'created_at');

-- Индексы
CREATE INDEX idx_analytics_tenant_type ON analytics_events(tenant_id, event_type, created_at DESC);
CREATE INDEX idx_analytics_event ON analytics_events(event_id, created_at DESC);
```

## Типы событий

| Event Type | Описание |
|------------|----------|
| `page.view` | Просмотр страницы события |
| `registration.started` | Начало регистрации |
| `registration.completed` | Завершение регистрации |
| `registration.cancelled` | Отмена регистрации |
| `checkin.completed` | Check-in |

## Трекинг

```java
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/track")
    public ResponseEntity<Void> track(@Valid @RequestBody TrackRequest request) {
        analyticsService.track(request);
        return ResponseEntity.accepted().build();
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepository repository;

    public void track(TrackRequest request) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setTenantId(TenantContext.getTenantId());
        event.setEventType(request.eventType());
        event.setEventId(request.eventId());
        event.setUserId(request.userId());
        event.setProperties(request.properties());
        
        repository.save(event);
    }
}
```

## Дашборд события

```java
public record EventDashboard(
    UUID eventId,
    String eventTitle,
    
    // Общие метрики
    long totalViews,
    long uniqueViews,
    long totalRegistrations,
    long confirmedRegistrations,
    long checkedIn,
    
    // Конверсии
    double viewToRegistrationRate,
    double registrationToCheckinRate,
    
    // По дням
    List<DailyMetric> viewsByDay,
    List<DailyMetric> registrationsByDay,
    
    // По типам билетов
    List<TicketTypeMetric> byTicketType
) {}
```

```java
@Service
public class DashboardService {

    public EventDashboard getEventDashboard(UUID eventId) {
        // Общие метрики
        long totalViews = repository.countByEventIdAndType(eventId, "page.view");
        long uniqueViews = repository.countDistinctUsersByEventIdAndType(eventId, "page.view");
        long registrations = repository.countByEventIdAndType(eventId, "registration.completed");
        long checkedIn = repository.countByEventIdAndType(eventId, "checkin.completed");
        
        // По дням (TimescaleDB time_bucket)
        List<DailyMetric> viewsByDay = repository.getMetricsByDay(
            eventId, "page.view", 30
        );
        
        return new EventDashboard(
            eventId,
            eventTitle,
            totalViews,
            uniqueViews,
            registrations,
            registrations, // confirmed
            checkedIn,
            calculateRate(registrations, totalViews),
            calculateRate(checkedIn, registrations),
            viewsByDay,
            registrationsByDay,
            byTicketType
        );
    }
}
```

## Воронка регистраций

```java
public record RegistrationFunnel(
    long pageViews,
    long registrationStarted,
    long registrationCompleted,
    long checkedIn,
    
    double startRate,      // started / views
    double completionRate, // completed / started
    double attendanceRate  // checkedIn / completed
) {}
```

## События (RabbitMQ)

### Потребляемые

| Event | Действие |
|-------|----------|
| `event.published` | Начало трекинга |
| `registration.created` | registration.completed |
| `registration.cancelled` | registration.cancelled |
| `checkin.completed` | checkin.completed |
| `payment.completed` | payment.completed |

```java
@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = "analytics.registration.created")
    public void handleRegistrationCreated(RegistrationCreatedEvent event) {
        analyticsService.track(TrackRequest.builder()
            .eventType("registration.completed")
            .eventId(event.getEventId())
            .userId(event.getUserId())
            .properties(Map.of(
                "ticketTypeId", event.getTicketTypeId(),
                "registrationId", event.getRegistrationId()
            ))
            .build());
    }
}
```

## Экспорт

```java
@PostMapping("/export")
public ResponseEntity<Resource> export(@Valid @RequestBody ExportRequest request) {
    byte[] data = exportService.export(request);
    
    String filename = String.format("report_%s.%s", 
        LocalDate.now(), 
        request.format().getExtension()
    );
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .contentType(request.format().getMediaType())
        .body(new ByteArrayResource(data));
}
```

### Форматы экспорта

| Формат | MIME Type |
|--------|-----------|
| CSV | text/csv |
| XLSX | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |

## Retention

```sql
-- Автоматическое удаление старых данных (TimescaleDB)
SELECT add_retention_policy('analytics_events', INTERVAL '1 year');
```

## Дальнейшее чтение

- [Service Topology](../../../architecture/service-topology.md)
