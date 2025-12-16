# Environments

Окружения разработки и деплоя AqStream.

## Обзор окружений

| Environment | Назначение | URL |
|-------------|-----------|-----|
| Local | Локальная разработка | localhost |
| Production | Продакшен | aqstream.ru |

**Production URLs:**
- Frontend: `https://aqstream.ru`
- API Gateway: `https://api.aqstream.ru`
- Documentation: `https://docs.aqstream.ru`

## Local Development

### Требования

- Docker 24+ и Docker Compose v2
- JDK 25
- Node.js 20 LTS
- pnpm 8+
- 16 GB RAM (рекомендуется)

### Быстрый старт

```bash
# Клонировать репозиторий
git clone https://github.com/egorov-ma/aqstream.git
cd aqstream

# Скопировать конфигурацию
cp .env.example .env

# Запустить инфраструктуру
make infra-up

# Запустить backend (в отдельном терминале)
./gradlew bootRun

# Запустить frontend (в отдельном терминале)
cd frontend && pnpm dev
```

### Порты

| Сервис | Порт |
|--------|------|
| Frontend | 3000 |
| Gateway | 8080 |
| User Service | 8081 |
| Event Service | 8082 |
| Payment Service | 8083 |
| Notification Service | 8084 |
| Media Service | 8085 |
| Analytics Service | 8086 |
| PostgreSQL (shared) | 5432 |
| PostgreSQL (user) | 5433 |
| PostgreSQL (payment) | 5434 |
| PostgreSQL (analytics) | 5435 |
| Redis | 6379 |
| RabbitMQ | 5672 |
| RabbitMQ Management | 15672 |
| MinIO | 9000 |
| MinIO Console | 9001 |

### Запуск отдельных сервисов

```bash
# Только инфраструктура (БД, Redis, RabbitMQ)
make infra-up

# Один backend сервис
./gradlew :services:event-service:bootRun

# Все backend сервисы
./gradlew bootRun --parallel

# Frontend
cd frontend && pnpm dev
```

### Hot Reload

Backend: Spring DevTools (автоматический restart при изменениях).

Frontend: Next.js Fast Refresh.

### Управление инфраструктурой

```bash
# Запустить инфраструктуру
make infra-up

# Остановить инфраструктуру
make infra-down

# Посмотреть логи
make infra-logs

# Статус контейнеров
make infra-ps

# Проверить health сервисов
make health

# Полный сброс (удаление данных)
make infra-reset
```

### Docker Compose файлы

| Файл | Назначение |
|------|-----------|
| `docker-compose.yml` | Основная конфигурация инфраструктуры |
| `docker-compose.override.yml` | Dev-специфичные настройки (verbose logging) |

### Полезные URL

| URL | Описание | Credentials |
|-----|----------|-------------|
| http://localhost:3000 | Frontend | — |
| http://localhost:8080/swagger-ui.html | API Docs (Gateway) | — |
| http://localhost:8082/swagger-ui.html | Event Service API | — |
| http://localhost:15672 | RabbitMQ Management | guest/guest |
| http://localhost:9001 | MinIO Console | minioadmin/minioadmin |

## Production

### Характеристики

- High availability
- Автоматическое масштабирование
- Мониторинг и алертинг
- Бэкапы

### Деплой

Автоматический деплой при push в `main`:

```yaml
# .github/workflows/deploy-production.yml
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        run: |
          ssh deploy@aqstream.ru "cd /app && docker compose pull && docker compose up -d"
```

## Конфигурация

### Environment Variables

```bash
# Общие
NODE_ENV=development|production
LOG_LEVEL=debug|info|warn|error

# Database
DATABASE_URL=jdbc:postgresql://host:port/db
DATABASE_USERNAME=user
DATABASE_PASSWORD=secret

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# JWT
JWT_SECRET=your-secret-key

# External Services
PAYMENT_API_KEY=...
PAYMENT_WEBHOOK_SECRET=...
TELEGRAM_BOT_TOKEN=...
TELEGRAM_BOT_USERNAME=...
```

### Profiles

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

---
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:postgresql://localhost:5432/event_service

---
spring:
  config:
    activate:
      on-profile: production
  datasource:
    url: ${DATABASE_URL}
    hikari:
      maximum-pool-size: 20
```

## Troubleshooting

### Порт занят

```bash
# Найти процесс
lsof -i :8080

# Или убить все Docker контейнеры
docker compose down
```

### База не запускается

```bash
# Проверить логи
docker compose logs postgres-shared

# Удалить volume и пересоздать
docker compose down -v
docker compose up -d postgres-shared
```

### Миграции не применяются

```bash
# Применить вручную
./gradlew :services:event-service:liquibaseUpdate

# Проверить статус
docker compose exec postgres-shared psql -U aqstream -d shared_services_db \
  -c "SELECT * FROM event_service.databasechangelog ORDER BY orderexecuted DESC LIMIT 5;"
```

## Дальнейшее чтение

- [Deploy](./deploy.md) — процесс деплоя
- [Tooling](../tech-stack/tooling.md) — инструменты
