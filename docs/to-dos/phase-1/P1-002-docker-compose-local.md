# P1-002 Docker Compose для локальной разработки

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Инфраструктура](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Разработчики должны иметь возможность запустить полное окружение AqStream одной командой. Это критично для:
- Быстрого onboarding новых разработчиков
- Локального тестирования интеграций между сервисами
- Воспроизведения багов с production

### Технический контекст

Инфраструктурные зависимости платформы:
- PostgreSQL 16+ (3 инстанса: shared, user, payment)
- Redis 7+
- RabbitMQ 3.13+
- MinIO (S3-compatible storage)

Требования:
- Все сервисы должны запускаться в Docker
- Hot reload для backend (Spring DevTools)
- Hot reload для frontend (Next.js Fast Refresh)
- Персистентные volumes для данных

## Цель

Создать Docker Compose конфигурацию для запуска полного стека локальной разработки.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-001 завершена (структура репозитория)

## Acceptance Criteria

- [x] Создан `docker-compose.yml` для инфраструктуры (PostgreSQL, Redis, RabbitMQ, MinIO)
- [x] Создан `docker-compose.override.yml` для development режима
- [x] PostgreSQL настроен с тремя базами данных и схемами
- [x] Redis доступен на порту 6379
- [x] RabbitMQ доступен на портах 5672 (AMQP) и 15672 (Management UI)
- [x] MinIO доступен на портах 9000 (API) и 9001 (Console)
- [x] Volumes настроены для персистентности данных
- [x] Health checks настроены для всех сервисов
- [x] `.env.example` создан с переменными окружения
- [x] Команда `make infra-up` запускает инфраструктуру
- [x] Команда `make infra-down` останавливает инфраструктуру
- [x] Документация в `docs/operations/environments.md` обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] `docker compose up -d` успешно запускает все контейнеры
- [x] Все health checks проходят
- [x] Данные сохраняются между перезапусками
- [x] Code review пройден
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается напрямую
- [ ] Frontend: не затрагивается напрямую
- [x] Database: PostgreSQL конфигурация
- [x] Infrastructure: Docker Compose, Redis, RabbitMQ, MinIO

### Структура файлов

```
aqstream/
├── docker/
│   ├── postgres/
│   │   └── init/
│   │       └── 01-init-databases.sql
│   └── rabbitmq/
│       └── definitions.json
├── docker-compose.yml
├── docker-compose.override.yml
├── .env.example
└── Makefile (обновить)
```

### Порты

| Сервис | Порт | Описание |
|--------|------|----------|
| PostgreSQL (shared) | 5432 | Event, Notification, Media |
| PostgreSQL (user) | 5433 | User Service |
| PostgreSQL (payment) | 5434 | Payment Service |
| Redis | 6379 | Cache |
| RabbitMQ AMQP | 5672 | Messaging |
| RabbitMQ Management | 15672 | UI (guest/guest) |
| MinIO API | 9000 | S3-compatible API |
| MinIO Console | 9001 | UI |

### docker-compose.yml (структура)

```yaml
version: '3.8'

services:
  postgres-shared:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: aqstream
      POSTGRES_PASSWORD: aqstream
      POSTGRES_DB: shared_services_db
    ports:
      - "5432:5432"
    volumes:
      - postgres-shared-data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U aqstream"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres-user:
    image: postgres:16-alpine
    # ... аналогично

  postgres-payment:
    image: postgres:16-alpine
    # ... аналогично

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_running"]

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio-data:/data

volumes:
  postgres-shared-data:
  postgres-user-data:
  postgres-payment-data:
  redis-data:
  rabbitmq-data:
  minio-data:
```

### PostgreSQL init script

```sql
-- docker/postgres/init/01-init-databases.sql

-- Создание схем для shared database
CREATE SCHEMA IF NOT EXISTS event_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS media_service;

-- Настройка search_path
ALTER DATABASE shared_services_db SET search_path TO public, event_service, notification_service, media_service;
```

### Makefile команды

```makefile
.PHONY: infra-up infra-down infra-logs infra-ps

infra-up:
	docker compose up -d postgres-shared postgres-user postgres-payment redis rabbitmq minio

infra-down:
	docker compose down

infra-logs:
	docker compose logs -f

infra-ps:
	docker compose ps
```

### .env.example

```bash
# PostgreSQL
POSTGRES_USER=aqstream
POSTGRES_PASSWORD=aqstream

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=aqstream-media

# JWT
JWT_SECRET=your-super-secret-jwt-key-change-in-production
```

## Зависимости

### Блокирует

- [P1-006] API Gateway setup
- [P1-007] PostgreSQL с multi-tenancy (RLS)
- [P1-008] RabbitMQ setup

### Зависит от

- [P1-001] Настройка монорепозитория

## Out of Scope

- Dockerfile для backend сервисов (будут в задачах сервисов)
- Dockerfile для frontend (будет в P1-009)
- Production docker-compose конфигурация

## Заметки

- Используем Alpine образы для минимального размера
- Health checks критичны для правильного порядка запуска
- MinIO используется как локальная замена S3/R2
- RabbitMQ Management UI полезен для отладки очередей
- Volumes именованные для удобства управления
