# Runbook

Операционные процедуры AqStream.

## Диагностика

```bash
# Статус сервисов
docker compose ps

# Health check
curl http://localhost:8080/actuator/health

# Логи сервиса
docker compose logs --tail=100 event-service

# Ресурсы
docker stats
```

## Перезапуск сервисов

```bash
# Graceful restart
docker compose restart event-service

# Force restart
docker compose stop -t 10 event-service && docker compose up -d event-service

# Все сервисы
docker compose restart
```

## Бэкап PostgreSQL

```bash
# Бэкап всех баз одной командой (рекомендуется)
make db-backup
# Бэкапы сохраняются в ./backups/ с timestamp

# Ручной бэкап одной базы
docker compose exec postgres-shared pg_dump -U aqstream -Fc -d shared_services_db > backup-$(date +%Y%m%d).dump
```

## Восстановление

```bash
# Восстановление из бэкапа (рекомендуется)
make db-restore BACKUP_DATE=20251223_120000
# Требуется указать timestamp бэкапа

# Ручное восстановление
docker compose stop event-service notification-service media-service
docker compose exec -T postgres-shared pg_restore -U aqstream -d shared_services_db < backup.dump
docker compose start event-service notification-service media-service
```

## Типичные проблемы

| Проблема | Диагностика | Решение |
| ---------- | ------------- | --------- |
| Сервис не отвечает | `docker compose logs [service]` | `docker compose restart [service]` |
| БД недоступна | `docker compose exec postgres-shared pg_isready -U aqstream` | `docker compose restart postgres-shared` |
| Out of Memory | `docker stats` | Restart + увеличить лимиты |
| Disk full | `docker system df` | `docker system prune -f` |

## Проверка после восстановления

```bash
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/events?page=0&size=1
```
