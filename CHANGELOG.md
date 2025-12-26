# Changelog

Все значимые изменения платформы AqStream документируются в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
версионирование следует [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0]

Промежуточный релиз Phase 2 с основной backend функциональностью.

### Added - User Service
- Регистрация пользователей через Telegram
- Регистрация пользователей по email с верификацией
- JWT аутентификация (access + refresh tokens)
- Восстановление пароля (password reset)
- CRUD организаций с workflow одобрения
- Управление участниками организаций с ролями (Owner, Moderator)
- Группы для приватных событий с инвайт-кодами

### Added - Event Service
- CRUD событий с жизненным циклом (Draft → Published → Completed/Cancelled)
- Типы билетов (бесплатные)
- Регистрации с confirmation codes
- Генерация QR-кодов для билетов
- Check-in API для организаторов
- Настройки видимости участников (публичная/приватная)
- Приватные события с привязкой к группам

### Added - Notification Service
- Интеграция с Telegram Bot (long polling)
- Отправка билетов через Telegram с QR-кодом
- Система шаблонов сообщений
- Настройки уведомлений пользователей
- Напоминания о событиях

### Added - Infrastructure
- API Gateway с endpoint агрегации версий
- Multi-tenancy с Row Level Security (RLS)
- Outbox pattern для надёжной доставки событий
- Unified CI/CD pipeline (lint, test, build, deploy)
- Allure test reports на GitHub Pages
- Автоматизация production deployment
- Версионирование системы (console welcome message, version API)
- Doc-as-code инфраструктура (MkDocs, OpenAPI validation)

### Fixed
- Оптимизация памяти Gateway (160M → 280M)
- Оптимизация памяти Event Service (до 350M)
- Оптимизация памяти Notification Service (140M → 280M)
- Оптимизация памяти User Service (до 280M)
- Redis аутентификация для Gateway
- Условная конфигурация OutboxProcessor
- Component scanning для вложенных модулей
- Выделение памяти MinIO (256M)

### Changed
- Upgrade production сервера: 1vCPU/2GB RAM → 2vCPU/4GB RAM
- Оптимизация Docker build caching
- Улучшение health checks с diagnostics контейнеров
- Увеличение startup wait time в CI/CD для стабильности

## [0.1.0]

Initial foundation release.

### Added - Foundation
- Gradle multi-module структура монорепозитория
- Docker Compose для локальной разработки
- Common модули (api, security, data, messaging, web, test)
- PostgreSQL multi-tenancy setup
- RabbitMQ инфраструктура
- Next.js 14 frontend с App Router
- shadcn/ui компоненты
- GitHub Actions CI/CD workflows

---

[0.2.0]: https://github.com/egorov-ma/aqstream/releases/tag/v0.2.0
