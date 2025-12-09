# AqStream

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14-black.svg)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)

**Open-source платформа для организации и управления мероприятиями.**

AqStream — это современная event management система, которая помогает организаторам создавать, продвигать и проводить мероприятия любого масштаба: от небольших митапов до крупных конференций.

## Для кого этот проект

**Организаторы мероприятий** получают инструменты для:
- Создания страниц событий с гибкими типами билетов
- Управления регистрациями и листами ожидания
- Приёма платежей через популярные платёжные системы
- Рассылки уведомлений участникам
- Анализа статистики и эффективности

**Участники** получают:
- Удобный поиск и открытие мероприятий
- Простую регистрацию и оплату
- Персональный кабинет с историей
- Уведомления о важных обновлениях

**Разработчики** найдут:
- Чистую микросервисную архитектуру
- Современный стек технологий
- Подробную документацию
- Примеры best practices

## Ключевые возможности

### Управление событиями
- Полный жизненный цикл события: черновик → публикация → проведение → архив
- Гибкие типы билетов с лимитами и периодами продаж
- Промокоды и скидки
- Групповые регистрации
- Листы ожидания с автоматическим оповещением

### Платежи
- Интеграция со Stripe и ЮKassa
- Полный и частичный возврат средств
- Отложенные платежи
- Финансовая отчётность

### Коммуникации
- Email-уведомления (подтверждения, напоминания, изменения)
- Telegram-бот для участников
- Push-уведомления (roadmap)

### Аналитика
- Дашборды для организаторов
- Воронка регистраций
- Финансовые отчёты
- Экспорт данных

### Корпоративные возможности
- Multi-tenancy: полная изоляция данных организаций
- Роли и права доступа
- Брендирование страниц событий
- API для интеграций

## Архитектура

AqStream построен на микросервисной архитектуре с event-driven коммуникацией:

```
┌──────────────────────────────────────────────────────────────────┐
│                          Clients                                  │
│              (Web App, Mobile App, External APIs)                 │
└─────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                         Nginx                                     │
│              (TLS termination, static files, routing)             │
└─────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                    API Gateway                                    │
│         (Authentication, Rate Limiting, Request Routing)          │
└───┬─────────┬─────────┬─────────┬─────────┬─────────┬────────────┘
    │         │         │         │         │         │
    ▼         ▼         ▼         ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
│ User  │ │ Event │ │Payment│ │Notif- │ │ Media │ │Analy- │
│Service│ │Service│ │Service│ │ication│ │Service│ │tics   │
└───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘
    │         │         │         │         │         │
    └─────────┴─────────┴────┬────┴─────────┴─────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                        RabbitMQ                                   │
│              (Event Bus, Async Communication)                     │
└──────────────────────────────────────────────────────────────────┘
                             │
    ┌────────────────────────┼────────────────────────┐
    │                        │                        │
    ▼                        ▼                        ▼
┌───────┐              ┌───────┐              ┌───────┐
│ Post- │              │ Redis │              │ MinIO │
│ greSQL│              │       │              │       │
└───────┘              └───────┘              └───────┘
```

### Сервисы

| Сервис | Назначение |
|--------|------------|
| **API Gateway** | Точка входа, аутентификация, rate limiting, маршрутизация |
| **User Service** | Пользователи, организации, роли, авторизация |
| **Event Service** | События, типы билетов, регистрации, check-in |
| **Payment Service** | Платежи, возвраты, финансовые отчёты |
| **Notification Service** | Email, Telegram, шаблоны сообщений |
| **Media Service** | Загрузка и обработка изображений |
| **Analytics Service** | Сбор метрик, дашборды, отчёты |

### Принципы архитектуры

- **Автономность сервисов** — каждый сервис независим и имеет собственную базу данных
- **Event-driven** — асинхронная коммуникация через RabbitMQ с гарантией доставки (Outbox pattern)
- **Multi-tenancy** — изоляция данных на уровне БД через Row Level Security
- **API-first** — все взаимодействия через REST API с OpenAPI документацией

## Технологический стек

### Backend

| Технология | Версия | Назначение |
|------------|--------|------------|
| **Java** | 25 LTS | Основной язык, virtual threads |
| **Spring Boot** | 3.5 | Framework (Spring MVC, servlet-based) |
| **Spring Security** | 6.5.x | Аутентификация, авторизация |
| **Spring Data JPA** | 2025.0.x | ORM, работа с базой данных |
| **PostgreSQL** | 16+ | Основная СУБД, Row Level Security |
| **Redis** | 7.x | Кэширование, сессии, rate limiting |
| **RabbitMQ** | 3.13+ | Message broker, event bus |
| **Liquibase** | 4.31 | Миграции базы данных |
| **MapStruct** | 1.6 | DTO mapping |

**Почему Spring MVC, а не WebFlux:**  
Осознанный выбор в пользу классического blocking I/O для простоты отладки, предсказуемого поведения и лучшей совместимости с JPA. Производительности Spring MVC достаточно для целевой нагрузки платформы.

### Frontend

| Технология | Версия | Назначение |
|------------|--------|------------|
| **Next.js** | 14 | React framework, App Router, SSR |
| **TypeScript** | 5.x | Типизация |
| **Tailwind CSS** | 3.x | Utility-first CSS |
| **shadcn/ui** | — | UI компоненты (единственная UI библиотека) |
| **TanStack Query** | 5.x | Server state management |
| **Zustand** | 4.x | Client state management |
| **React Hook Form** | 7.x | Формы |
| **Zod** | 3.x | Валидация |

**Почему shadcn/ui:**  
Компоненты копируются в проект и полностью контролируются. Это обеспечивает консистентный UI и позволяет кастомизировать без борьбы с библиотекой.

### Infrastructure

| Технология | Назначение |
|------------|------------|
| **Docker Compose** | Локальная оркестрация |
| **Nginx** | Reverse proxy, TLS, static files |
| **MinIO** | S3-compatible хранилище файлов |
| **Prometheus** | Сбор метрик |
| **Grafana** | Визуализация, дашборды |
| **Loki** | Агрегация логов |

## Быстрый старт

### Требования

- Docker 24+ и Docker Compose v2
- 16 GB RAM (рекомендуется)
- 50 GB свободного места

### Запуск

```bash
# Клонировать репозиторий
git clone https://github.com/aqstream/aqstream.git
cd aqstream

# Скопировать конфигурацию
cp .env.example .env

# Запустить платформу
make up

# Проверить статус
make status
```

После запуска:
- **Frontend**: http://localhost:3000
- **API**: http://localhost:8080
- **API Docs (Swagger)**: http://localhost:8080/swagger-ui.html
- **API Docs (ReDoc)**: http://localhost:8080/redoc
- **RabbitMQ UI**: http://localhost:15672 (guest/guest)
- **Grafana**: http://localhost:3001

## Документация

Полная документация находится в директории [`/docs`](docs1/README.md):

| Раздел | Описание |
|--------|----------|
| [Architecture](docs1/architecture/) | Микросервисная архитектура, топология, данные |
| [Business](docs1/business/) | Видение продукта, user journeys, требования |
| [Tech Stack](docs1/tech-stack/) | Backend, Frontend, API, тестирование |
| [Operations](docs1/operations/) | Окружения, деплой, CI/CD, мониторинг |
| [Experience](docs1/experience/) | Безопасность, contributing, community |

## Разработка

```bash
# Установить pre-commit hooks
make setup

# Запустить только инфраструктуру (PostgreSQL, Redis, RabbitMQ)
make infra-up

# Backend: запустить сервис
./gradlew :event-service:bootRun

# Frontend: запустить dev server
cd frontend && pnpm dev

# Запустить тесты
make test

# Проверить код
make lint
```

### Структура репозитория

```
aqstream/
├── services/           # Backend микросервисы
│   ├── gateway/
│   ├── user-service/
│   ├── event-service/
│   ├── payment-service/
│   ├── notification-service/
│   ├── media-service/
│   └── analytics-service/
├── common/             # Общие модули (DTO, security, data)
├── frontend/           # Next.js приложение
├── docs/               # Документация
├── docker/             # Docker конфигурации
└── config/             # Общие конфигурации (checkstyle, etc.)
```

Подробнее в [документации для разработчиков](docs1/operations/environments.md).

## Contributing

Мы приветствуем вклад в развитие проекта!

1. Прочитайте [Contributing Guide](docs1/experience/contributing.md)
2. Изучите [архитектуру](docs1/architecture/overview.md)
3. Посмотрите [открытые issues](https://github.com/aqstream/aqstream/issues)
4. Найдите задачи с меткой `good first issue`

## Лицензия

Проект распространяется под лицензией [MIT](./LICENSE).

## Контакты

- **Issues**: [GitHub Issues](https://github.com/aqstream/aqstream/issues)
- **Discussions**: [GitHub Discussions](https://github.com/aqstream/aqstream/discussions)

---

<p align="center">Made with ❤️ by AqStream Community</p>
