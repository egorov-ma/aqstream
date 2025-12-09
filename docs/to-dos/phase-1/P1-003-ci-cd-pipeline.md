# P1-003 CI/CD Pipeline (GitHub Actions)

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Инфраструктура](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Автоматизированный CI/CD критичен для:
- Обеспечения качества кода через автоматические проверки
- Быстрой обратной связи для разработчиков
- Автоматического деплоя на staging после merge в main
- Предотвращения регрессий

### Технический контекст

Стек для CI/CD:
- GitHub Actions как платформа
- Gradle для backend сборки и тестов
- pnpm для frontend сборки и тестов
- Docker для интеграционных тестов
- GitHub Container Registry для образов

Требования:
- CI проверки на каждый PR
- Автодеплой на staging при merge в main
- Manual approval для production

## Цель

Настроить GitHub Actions workflows для автоматической сборки, тестирования и деплоя.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-001 завершена (структура репозитория)

## Acceptance Criteria

- [x] Создан `.github/workflows/ci.yml` для проверок на PR
- [x] CI workflow включает:
  - [x] Backend lint (checkstyle)
  - [x] Backend unit tests
  - [x] Frontend lint (eslint)
  - [x] Frontend type check
  - [x] Frontend tests
  - [x] Frontend build
- [x] Создан `.github/workflows/deploy-staging.yml` для деплоя на staging
- [x] Создан `.github/workflows/deploy-production.yml` для деплоя на production
- [x] Настроен caching для Gradle и pnpm
- [x] Настроены GitHub secrets (документация)
- [x] Branch protection rules задокументированы
- [x] Документация в `docs/operations/ci-cd.md` обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] CI workflow успешно проходит
- [x] Workflows соответствуют best practices GitHub Actions
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: CI проверки
- [x] Frontend: CI проверки
- [ ] Database: не затрагивается
- [x] Infrastructure: GitHub Actions workflows

### Структура файлов

```
.github/
├── workflows/
│   ├── ci.yml                 # PR checks
│   ├── deploy-staging.yml     # Auto-deploy to staging
│   └── deploy-production.yml  # Manual deploy to production
└── CODEOWNERS                 # Code owners для review
```

### Реализованные файлы

| Файл | Описание |
|------|----------|
| `.github/workflows/ci.yml` | Backend lint/test/build + Frontend lint/test/build |
| `.github/workflows/deploy-staging.yml` | Build images → Push GHCR → Deploy |
| `.github/workflows/deploy-production.yml` | Manual workflow с подтверждением |

### CI Jobs

| Job | Описание | Services |
|-----|----------|----------|
| `backend-lint` | Checkstyle | — |
| `backend-test` | JUnit 5 | PostgreSQL 16, Redis 7 |
| `backend-build` | Gradle build | — |
| `frontend-lint` | ESLint + TypeCheck | — |
| `frontend-test` | Vitest | — |
| `frontend-build` | Next.js build | — |

### Особенности реализации

| Feature | Реализация |
|---------|-----------|
| Concurrency | `cancel-in-progress: true` для CI |
| Frontend check | Проверка наличия `package.json` |
| Caching | `gradle/actions/setup-gradle@v4`, `actions/setup-node@v4` |
| Versioning | Commit SHA (7 символов) |
| Production safety | Input `confirm: deploy` + environment approval |

## Зависимости

### Блокирует

- Все последующие задачи (CI нужен для review)

### Зависит от

- [P1-001] Настройка монорепозитория

## Out of Scope

- E2E тесты в CI (будут добавлены позже)
- Performance тесты
- Security scanning (SAST/DAST)
- Dependency vulnerability scanning

## Заметки

- GitHub Actions v4 для всех actions
- Frontend jobs умно пропускаются если `package.json` не существует
- `continue-on-error: true` для сервисов без Dockerfile (до их создания)
- Документация в `docs/operations/ci-cd.md` с troubleshooting
