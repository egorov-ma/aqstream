# Документация AqStream

Добро пожаловать в техническую документацию платформы AqStream.

## Быстрые ссылки по ролям

### Разработчик

| Задача | Документ |
|--------|----------|
| Начать разработку | [Environments](./operations/environments.md) |
| Создать новый сервис | [Service Template](./tech-stack/backend/service-template.md) |
| Написать тесты | [Testing Strategy](./tech-stack/qa/testing-strategy.md) |
| Понять API conventions | [API Guidelines](./tech-stack/backend/api-guidelines.md) |

### QA

| Задача | Документ |
|--------|----------|
| Стратегия тестирования | [Testing Strategy](./tech-stack/qa/testing-strategy.md) |
| API документация | [API Documentation](./tech-stack/backend/api/README.md) |
| Окружения | [Environments](./operations/environments.md) |

### DevOps / Operations

| Задача | Документ |
|--------|----------|
| Настройка сервера | [Server Setup](./operations/server-setup.md) |
| Настроить CI/CD | [CI/CD](./operations/ci-cd.md) |
| Деплой | [Deploy](./operations/deploy.md) |
| Мониторинг | [Observability](./operations/observability.md) |
| Runbook | [Runbook](./operations/runbook.md) |

### Business / Product

| Задача | Документ |
|--------|----------|
| Видение продукта | [Vision](./business/vision.md) |
| Ролевая модель | [Role Model](./business/role-model.md) |
| План развития | [Roadmap](./business/roadmap.md) |
| Требования | [Functional Requirements](./business/functional-requirements.md) |
| Сценарии использования | [User Journeys](./business/user-journeys.md) |

---

## Навигация

### Архитектура

| Документ | Описание |
|----------|----------|
| [Overview](./architecture/overview.md) | Общий обзор архитектуры |
| [Service Topology](./architecture/service-topology.md) | Микросервисы и их взаимодействие |
| [Data Architecture](./architecture/data-architecture.md) | Архитектура данных, БД, кэширование |

### Бизнес

| Документ | Описание |
|----------|----------|
| [Vision](./business/vision.md) | Видение продукта и цели |
| [Role Model](./business/role-model.md) | Ролевая модель платформы |
| [User Journeys](./business/user-journeys.md) | Сценарии использования |
| [Functional Requirements](./business/functional-requirements.md) | Функциональные требования |
| [Roadmap](./business/roadmap.md) | План развития |

### Данные

| Документ | Описание |
|----------|----------|
| [Domain Model](./data/domain-model.md) | Модель предметной области |
| [Migrations](./data/migrations.md) | Управление миграциями БД |

### Tech Stack

| Документ | Описание |
|----------|----------|
| [Overview](./tech-stack/overview.md) | Обзор технологий |
| [Tooling](./tech-stack/tooling.md) | Инструменты разработки |

**Backend:**

| Документ | Описание |
|----------|----------|
| [Architecture](./tech-stack/backend/architecture.md) | Архитектура backend |
| [API Guidelines](./tech-stack/backend/api-guidelines.md) | Правила проектирования API |
| [Common Library](./tech-stack/backend/common-library.md) | Общие модули |
| [Service Template](./tech-stack/backend/service-template.md) | Шаблон сервиса |
| [API Documentation](./tech-stack/backend/api/README.md) | OpenAPI, Swagger, ReDoc |

**Сервисы:**

| Сервис | Описание |
|--------|----------|
| [Gateway](./tech-stack/backend/services/gateway.md) | API Gateway |
| [User Service](./tech-stack/backend/services/user-service.md) | Пользователи и организации |
| [Event Service](./tech-stack/backend/services/event-service.md) | События и регистрации |
| [Payment Service](./tech-stack/backend/services/payment-service.md) | Платежи |
| [Notification Service](./tech-stack/backend/services/notification-service.md) | Уведомления |
| [Media Service](./tech-stack/backend/services/media-service.md) | Файлы и изображения |
| [Analytics Service](./tech-stack/backend/services/analytics-service.md) | Аналитика |

**Frontend:**

| Документ | Описание |
|----------|----------|
| [Architecture](./tech-stack/frontend/architecture.md) | Архитектура frontend |
| [Components](./tech-stack/frontend/components.md) | Компоненты UI |

**QA:**

| Документ | Описание |
|----------|----------|
| [Testing Strategy](./tech-stack/qa/testing-strategy.md) | Стратегия тестирования |

### Operations

| Документ | Описание |
|----------|----------|
| [Environments](./operations/environments.md) | Окружения |
| [Server Setup](./operations/server-setup.md) | Настройка сервера |
| [Deploy](./operations/deploy.md) | Деплой |
| [CI/CD](./operations/ci-cd.md) | Пайплайны |
| [Observability](./operations/observability.md) | Мониторинг и логирование |

| [Runbook](./operations/runbook.md) | Операционные процедуры |

### Experience

| Документ | Описание |
|----------|----------|
| [Security](./experience/security.md) | Безопасность |
| [Community](./experience/community.md) | Контакты, лицензия |

### Задачи (To-Dos)

| Документ | Описание |
|----------|----------|
| [To-Dos](./to-dos/README.md) | Задачи проекта |
| [Template](./to-dos/_template.md) | Шаблон задачи |
