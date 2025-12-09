# Runbook: Backup & Restore

Процедуры резервного копирования и восстановления данных AqStream.

## Стратегия бэкапов

| Тип | Частота | Retention | RPO |
|-----|---------|-----------|-----|
| Full backup | Ежедневно (03:00 UTC) | 30 дней | 24 часа |
| WAL archiving | Continuous | 7 дней | ~1 минута |

## Создание бэкапа

### Ручной бэкап PostgreSQL

```bash
# Бэкап одной базы
docker compose exec postgres-shared pg_dump -U aqstream -d shared_services_db > backup-shared-$(date +%Y%m%d-%H%M%S).sql

# Бэкап всех баз
for db in shared_services_db user_service_db payment_service_db analytics_service_db; do
  docker compose exec postgres-shared pg_dump -U aqstream -d $db > backup-$db-$(date +%Y%m%d-%H%M%S).sql
done

# Сжатый бэкап
docker compose exec postgres-shared pg_dump -U aqstream -d shared_services_db | gzip > backup-shared-$(date +%Y%m%d-%H%M%S).sql.gz
```

### Бэкап с кастомным форматом (рекомендуется)

```bash
# Custom format — поддерживает параллельное восстановление
docker compose exec postgres-shared pg_dump -U aqstream -Fc -d shared_services_db > backup-shared-$(date +%Y%m%d-%H%M%S).dump
```

### Бэкап MinIO (файлы)

```bash
# Используя mc (MinIO Client)
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mirror local/aqstream-media ./backup-media-$(date +%Y%m%d)
```

## Восстановление

### Восстановление PostgreSQL

```bash
# Из SQL файла
docker compose exec -T postgres-shared psql -U aqstream -d shared_services_db < backup-shared-20240115.sql

# Из сжатого файла
gunzip -c backup-shared-20240115.sql.gz | docker compose exec -T postgres-shared psql -U aqstream -d shared_services_db

# Из custom format (быстрее)
docker compose exec -T postgres-shared pg_restore -U aqstream -d shared_services_db < backup-shared-20240115.dump
```

### Восстановление в чистую базу

```bash
# 1. Остановить сервисы
docker compose stop event-service notification-service media-service

# 2. Удалить и пересоздать базу
docker compose exec postgres-shared psql -U aqstream -c "DROP DATABASE shared_services_db;"
docker compose exec postgres-shared psql -U aqstream -c "CREATE DATABASE shared_services_db;"

# 3. Восстановить
docker compose exec -T postgres-shared pg_restore -U aqstream -d shared_services_db < backup.dump

# 4. Запустить сервисы
docker compose start event-service notification-service media-service
```

### Восстановление MinIO

```bash
mc mirror ./backup-media-20240115 local/aqstream-media
```

## Point-in-Time Recovery (PITR)

### Настройка WAL archiving

```yaml
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'cp %p /var/lib/postgresql/wal_archive/%f'
```

### Восстановление на момент времени

```bash
# 1. Остановить PostgreSQL
docker compose stop postgres-shared

# 2. Очистить data directory
rm -rf /var/lib/postgresql/data/*

# 3. Восстановить base backup
pg_restore -D /var/lib/postgresql/data base_backup.tar

# 4. Создать recovery.signal и настроить recovery_target_time
echo "recovery_target_time = '2024-01-15 14:30:00 UTC'" >> postgresql.conf
touch recovery.signal

# 5. Запустить PostgreSQL
docker compose start postgres-shared
```

## Автоматические бэкапы

### Cron job

```bash
# /etc/cron.d/aqstream-backup
0 3 * * * root /opt/aqstream/scripts/backup.sh >> /var/log/aqstream-backup.log 2>&1
```

### Скрипт бэкапа

```bash
#!/bin/bash
# /opt/aqstream/scripts/backup.sh

set -e

BACKUP_DIR=/backups/aqstream
DATE=$(date +%Y%m%d-%H%M%S)
RETENTION_DAYS=30

# Создать директорию
mkdir -p $BACKUP_DIR

# Бэкап каждой базы
for db in shared_services_db user_service_db payment_service_db; do
  echo "Бэкап $db..."
  docker compose exec -T postgres-shared pg_dump -U aqstream -Fc -d $db > $BACKUP_DIR/$db-$DATE.dump
done

# Бэкап MinIO
echo "Бэкап MinIO..."
mc mirror local/aqstream-media $BACKUP_DIR/media-$DATE

# Удалить старые бэкапы
find $BACKUP_DIR -type f -mtime +$RETENTION_DAYS -delete

echo "Бэкап завершён: $DATE"
```

## Проверка бэкапов

### Ежемесячная проверка

```bash
# 1. Поднять тестовый PostgreSQL
docker run -d --name postgres-test -e POSTGRES_PASSWORD=test postgres:16

# 2. Восстановить бэкап
docker exec -i postgres-test psql -U postgres -c "CREATE DATABASE test_restore;"
docker exec -i postgres-test pg_restore -U postgres -d test_restore < backup.dump

# 3. Проверить данные
docker exec postgres-test psql -U postgres -d test_restore -c "SELECT COUNT(*) FROM event_service.events;"

# 4. Удалить тестовый контейнер
docker rm -f postgres-test
```

## Disaster Recovery

### Полное восстановление системы

```bash
# 1. Развернуть инфраструктуру
docker compose up -d postgres-shared postgres-user postgres-payment redis rabbitmq minio

# 2. Дождаться готовности
sleep 30

# 3. Восстановить базы данных
for db in shared_services_db user_service_db payment_service_db; do
  docker compose exec -T postgres-shared pg_restore -U aqstream -d $db < $BACKUP_DIR/$db-latest.dump
done

# 4. Восстановить файлы
mc mirror $BACKUP_DIR/media-latest local/aqstream-media

# 5. Запустить сервисы
docker compose up -d

# 6. Проверить health
docker compose ps
curl http://localhost:8080/actuator/health
```

### RTO/RPO

| Метрика | Target | Текущий |
|---------|--------|---------|
| RPO (Recovery Point Objective) | 1 час | ~24 часа (daily backup) |
| RTO (Recovery Time Objective) | 4 часа | ~1 час |

## Чеклист

### Перед восстановлением

- [ ] Определён объём данных для восстановления
- [ ] Выбран правильный бэкап (дата, время)
- [ ] Сервисы остановлены
- [ ] Текущие данные сохранены (если нужно)

### После восстановления

- [ ] Данные восстановлены
- [ ] Сервисы запущены
- [ ] Health checks проходят
- [ ] Базовая функциональность проверена
- [ ] Команда уведомлена

## Контакты

При проблемах с восстановлением — эскалация через [Incident Response](./incident-response.md).
