# AqStream

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14-black.svg)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)

**Open-source платформа для организации и управления мероприятиями.**

Любые события — онлайн и офлайн, рабочие и неформальные. AqStream помогает организаторам создавать события, управлять регистрациями, принимать платежи и анализировать результаты.

## Возможности

| Модуль | Описание |
|--------|----------|
| **События** | Полный жизненный цикл, гибкие типы билетов, листы ожидания |
| **Платежи** | Интеграция с платёжными провайдерами, возвраты, финансовая отчётность |
| **Уведомления** | Telegram-бот, автоматические напоминания |
| **Аналитика** | Дашборды, воронки регистраций, экспорт |
| **Multi-tenancy** | Изоляция данных организаций, роли и права |

## Архитектура

```mermaid
graph TB
    subgraph Clients
        WEB[Web App<br/>aqstream.ru]
        API_CLIENT[External APIs<br/>api.aqstream.ru]
    end

    subgraph Edge["Reverse Proxy"]
        NGINX[Nginx<br/>TLS termination]
    end

    subgraph Gateway
        GW[API Gateway :8080]
    end

    subgraph Services
        US[User Service]
        ES[Event Service]
        PS[Payment Service]
        NS[Notification Service]
        MS[Media Service]
        AS[Analytics Service]
    end

    subgraph Infrastructure
        PG[(PostgreSQL)]
        RMQ[RabbitMQ]
        REDIS[(Redis)]
        MINIO[(MinIO)]
    end

    WEB --> NGINX
    API_CLIENT --> NGINX
    NGINX --> GW
    GW --> US & ES & PS & NS & MS & AS
    US & ES & PS --> PG
    US & ES & PS --> RMQ
    GW & US & ES --> REDIS
    MS --> MINIO
```

**Принципы:** микросервисы, event-driven (Outbox pattern), multi-tenant (Row Level Security), API-first.

## Быстрый старт

### Локальная разработка

```bash
git clone https://github.com/egorov-ma/aqstream.git && cd aqstream
cp .env.example .env
make infra-up     # Запустить инфраструктуру (PostgreSQL, Redis, RabbitMQ, MinIO)
```

| URL | Описание |
|-----|----------|
| http://localhost:3000 | Frontend |
| http://localhost:8080/swagger-ui.html | API Docs (Gateway) |
| http://localhost:15672 | RabbitMQ Management (guest/guest) |
| http://localhost:9001 | MinIO Console (minioadmin/minioadmin) |

### Production

| URL | Описание |
|-----|----------|
| https://aqstream.ru | Frontend |
| https://api.aqstream.ru | API Gateway |
| https://docs.aqstream.ru | Документация |
| https://egorov-ma.github.io/aqstream/allure/ | Allure Reports (тесты) |

## Технологии

| Backend | Frontend | Infrastructure |
|---------|----------|----------------|
| Java 25, Spring Boot 3.5 | Next.js 14, TypeScript 5 | Docker Compose |
| PostgreSQL 16, Redis 7 | Tailwind CSS, shadcn/ui | Nginx, MinIO |
| RabbitMQ, Liquibase | TanStack Query, Zustand | Prometheus, Grafana |

## Сервисы

| Сервис | Порт | Описание |
|--------|------|----------|
| Gateway | 8080 | API Gateway, JWT validation, rate limiting |
| User Service | 8081 | Пользователи, организации, роли |
| Event Service | 8082 | События, билеты, регистрации |
| Payment Service | 8083 | Платежи, возвраты |
| Notification Service | 8084 | Telegram-уведомления |
| Media Service | 8085 | Файлы, изображения |
| Analytics Service | 8086 | Метрики, отчёты |

## Полезные команды

```bash
make help           # Все доступные команды
make infra-up       # Запустить инфраструктуру
make infra-down     # Остановить инфраструктуру
make health         # Проверить доступность сервисов
make test           # Запустить тесты
make lint           # Проверка кода
```

## Документация

Полная документация: [`/docs`](docs/README.md)

| Раздел | Описание |
|--------|----------|
| [Architecture](docs/architecture/) | Микросервисы, топология, данные |
| [Tech Stack](docs/tech-stack/) | Backend, Frontend, API, тесты |
| [Operations](docs/operations/) | Окружения, деплой, CI/CD |
| [Server Setup](docs/operations/server-setup.md) | Подготовка сервера |

## Лицензия

[MIT](./LICENSE)

---

<p align="center">Made with ❤️ by AqStream</p>
