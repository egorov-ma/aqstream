# Runbook: Service Restart

Процедура перезапуска сервисов AqStream.

## Когда использовать

- Сервис не отвечает на health checks
- Memory leak (OOM)
- Зависание сервиса
- После ручного обновления конфигурации

## Предварительные проверки

```bash
# 1. Проверить статус сервиса
docker compose ps event-service

# 2. Проверить логи на ошибки
docker compose logs --tail=100 event-service

# 3. Проверить health endpoint
curl http://localhost:8082/actuator/health

# 4. Проверить метрики (если доступны)
curl http://localhost:8082/actuator/prometheus | grep jvm_memory
```

## Процедура перезапуска

### Graceful Restart (предпочтительно)

```bash
# Перезапуск с graceful shutdown (30 секунд на завершение запросов)
docker compose restart event-service

# Проверить что сервис поднялся
docker compose ps event-service

# Дождаться ready состояния
for i in {1..30}; do
  if curl -s http://localhost:8082/actuator/health/readiness | grep -q "UP"; then
    echo "Сервис готов"
    break
  fi
  echo "Ожидание... ($i)"
  sleep 2
done
```

### Force Restart (если graceful не работает)

```bash
# Остановка с таймаутом 10 секунд, затем kill
docker compose stop -t 10 event-service
docker compose up -d event-service
```

### Полная пересборка (при подозрении на corrupted state)

```bash
docker compose stop event-service
docker compose rm -f event-service
docker compose up -d event-service
```

## Перезапуск всех сервисов

```bash
# Последовательный перезапуск (безопаснее)
for service in gateway user-service event-service payment-service notification-service media-service analytics-service; do
  echo "Перезапуск $service..."
  docker compose restart $service
  sleep 30  # Дать время на startup
done

# Или параллельный (быстрее, но рискованнее)
docker compose restart
```

## После перезапуска

```bash
# 1. Проверить что все сервисы работают
docker compose ps

# 2. Проверить health checks
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8082/actuator/health  # Event Service

# 3. Проверить логи на ошибки при старте
docker compose logs --tail=50 event-service | grep -i error

# 4. Проверить базовую функциональность
curl http://localhost:8080/api/v1/events?page=0&size=1
```

## Особые случаи

### Gateway не отвечает

Gateway — критический компонент. При его недоступности:

```bash
# Проверить что downstream сервисы работают напрямую
curl http://localhost:8082/actuator/health

# Перезапустить gateway
docker compose restart gateway

# Если не помогает — проверить Redis (rate limiting)
docker compose logs redis
docker compose restart redis
```

### Payment Service

**Внимание:** Перезапуск payment-service может прервать активные транзакции.

```bash
# Проверить нет ли активных платежей
docker compose logs payment-service | grep -i "processing"

# Перезапуск в период низкой активности
docker compose restart payment-service
```

### База данных недоступна

```bash
# Проверить PostgreSQL
docker compose ps postgres-shared
docker compose logs postgres-shared

# Перезапустить БД (осторожно!)
docker compose restart postgres-shared

# Дождаться готовности
docker compose exec postgres-shared pg_isready -U aqstream
```

## Эскалация

Если после перезапуска проблема не решена:

1. Собрать логи: `docker compose logs > logs-$(date +%Y%m%d-%H%M%S).txt`
2. Проверить ресурсы: `docker stats`
3. Эскалировать инцидент (см. [Incident Response](./incident-response.md))

## Чеклист

- [ ] Проверен статус сервиса перед перезапуском
- [ ] Проверены логи на причину проблемы
- [ ] Выполнен graceful restart
- [ ] Сервис успешно прошёл health check
- [ ] Проверена базовая функциональность
- [ ] Зафиксирован инцидент (если был)
