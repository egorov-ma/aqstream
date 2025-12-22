# Observability

Мониторинг, логирование и алертинг.

## Стек

| Компонент | Технология | Порт |
| ----------- | ------------ | ------ |
| Metrics | Prometheus | 9090 |
| Visualization | Grafana | 3001 |
| Logs | Loki + Promtail | 3100 |
| Alerting | Alertmanager | 9093 |

## Метрики (Spring Actuator)

**Endpoint:** `/actuator/prometheus`

| Метрика | Описание |
| --------- | ---------- |
| `http_server_requests_seconds` | Latency HTTP |
| `jvm_memory_used_bytes` | Memory |
| `hikaricp_connections_active` | DB connections |
| `rabbitmq_consumed_total` | Messages |

**Custom метрики:**

```java
@Component
public class RegistrationMetrics {
    private final Counter created = Counter.builder("registrations.created")
        .register(registry);
    public void incrementCreated() { created.increment(); }
}
```

## Логирование

**Формат:** `%d [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n`

```java
// На русском с context
log.info("Событие создано: eventId={}, tenantId={}", event.getId(), tenantId);
log.error("Ошибка: registrationId={}, error={}", regId, e.getMessage(), e);
```

## Correlation ID

X-Correlation-ID прокидывается через все сервисы и добавляется в MDC для логов.

```java
MDC.put("correlationId", correlationId);
response.setHeader("X-Correlation-ID", correlationId);
```

## Health Checks

| Endpoint | Назначение |
| ---------- | ------------ |
| `/actuator/health` | Общий статус |
| `/actuator/health/liveness` | Проверка что приложение живо |
| `/actuator/health/readiness` | Проверка что приложение готово принимать трафик |

## Alerting Rules

```yaml
# Примеры alerts
- alert: HighErrorRate
  expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 0.1

- alert: ServiceDown
  expr: up == 0

- alert: HighLatency
  expr: histogram_quantile(0.95, ...) > 1
```

## Grafana Dashboards

- **Service Overview:** Request rate, P95 latency, Error rate
- **JVM:** Heap, GC, Threads
- **Database:** Connection pool, Query latency
