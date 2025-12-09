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
- [x] CI pipeline проходит
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

### Реализованные файлы

| Файл | Описание |
|------|----------|
| `docker-compose.yml` | PostgreSQL×3, Redis, RabbitMQ, MinIO, Gateway |
| `docker-compose.override.yml` | Verbose logging для development |
| `docker-compose.override.example.yml` | Шаблон override файла |
| `.env.example` | Переменные окружения с defaults |
| `docker/postgres/init/01-init-shared-db.sql` | Схемы: event, notification, media |
| `docker/postgres/init/02-init-user-db.sql` | Схема: user_service |
| `docker/postgres/init/03-init-payment-db.sql` | Схема: payment_service |
| `docker/rabbitmq/definitions.json` | Exchanges, queues, bindings |
| `docker/rabbitmq/rabbitmq.conf` | Конфигурация RabbitMQ |

### Реализованные сервисы

| Сервис | Image | Health Check |
|--------|-------|--------------|
| postgres-shared | postgres:16-alpine | pg_isready |
| postgres-user | postgres:16-alpine | pg_isready |
| postgres-payment | postgres:16-alpine | pg_isready |
| redis | redis:7-alpine | redis-cli ping |
| rabbitmq | rabbitmq:3.13-management-alpine | rabbitmq-diagnostics |
| minio | minio/minio:latest | curl /minio/health/live |
| minio-init | minio/mc:latest | Создание bucket |

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

- Alpine образы для минимального размера
- Health checks с `start_period` для корректного порядка запуска
- MinIO bucket создаётся автоматически через `minio-init` контейнер
- RabbitMQ definitions.json загружает exchanges/queues при старте
- Именованные volumes с префиксом `aqstream-` для удобства управления
