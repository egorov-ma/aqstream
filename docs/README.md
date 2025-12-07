# Документация AqStream

Добро пожаловать в техническую документацию платформы AqStream.

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
| [Deploy](./operations/deploy.md) | Деплой |
| [CI/CD](./operations/ci-cd.md) | Пайплайны |
| [Observability](./operations/observability.md) | Мониторинг и логирование |

**Runbooks:**

| Документ | Описание |
|----------|----------|
| [Service Restart](./operations/runbooks/service-restart.md) | Перезапуск сервисов |
| [Incident Response](./operations/runbooks/incident-response.md) | Реагирование на инциденты |
| [Backup & Restore](./operations/runbooks/backup-restore.md) | Бэкапы |

### Experience

| Документ | Описание |
|----------|----------|
| [Security](./experience/security.md) | Безопасность |
| [Contributing](./experience/contributing.md) | Как контрибьютить |
| [Community](./experience/community.md) | Сообщество |

### Задачи (To-Dos)

| Директория | Описание |
|------------|----------|
| [to-dos/](./to-dos/) | Задачи проекта |
| [to-dos/_template.md](./to-dos/_template.md) | Шаблон задачи |
| [to-dos/phase-1/](./to-dos/phase-1/) | Задачи Phase 1: Foundation |
| [to-dos/phase-2/](./to-dos/phase-2/) | Задачи Phase 2: Core |
| [to-dos/phase-3/](./to-dos/phase-3/) | Задачи Phase 3: Growth |
| [to-dos/phase-4/](./to-dos/phase-4/) | Задачи Phase 4: Scale |

## Быстрые ссылки

- **Начать разработку:** [Environments](./operations/environments.md)
- **Создать сервис:** [Service Template](./tech-stack/backend/service-template.md)
- **Понять архитектуру:** [Overview](./architecture/overview.md)
- **Написать тесты:** [Testing Strategy](./tech-stack/qa/testing-strategy.md)
