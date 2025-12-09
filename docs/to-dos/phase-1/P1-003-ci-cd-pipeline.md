# P1-003 CI/CD Pipeline (GitHub Actions)

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
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
- [ ] P1-001 завершена (структура репозитория)

## Acceptance Criteria

- [ ] Создан `.github/workflows/ci.yml` для проверок на PR
- [ ] CI workflow включает:
  - [ ] Backend lint (checkstyle)
  - [ ] Backend unit tests
  - [ ] Frontend lint (eslint)
  - [ ] Frontend type check
  - [ ] Frontend tests
  - [ ] Frontend build
- [ ] Создан `.github/workflows/deploy-staging.yml` для деплоя на staging
- [ ] Создан `.github/workflows/deploy-production.yml` для деплоя на production
- [ ] Настроен caching для Gradle и pnpm
- [ ] Настроены GitHub secrets (документация)
- [ ] Branch protection rules задокументированы
- [ ] Документация в `docs/operations/ci-cd.md` обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] CI workflow успешно проходит на тестовом PR
- [ ] Workflows соответствуют best practices GitHub Actions
- [ ] Code review пройден
- [ ] Чеклист в roadmap обновлён

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

### ci.yml (структура)

```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  backend-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      - name: Run Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest

  backend-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      - name: Run Tests
        run: ./gradlew test

  frontend-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v2
        with:
          version: 8
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'pnpm'
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: cd frontend && pnpm install
      - run: cd frontend && pnpm lint
      - run: cd frontend && pnpm typecheck

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v2
        with:
          version: 8
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'pnpm'
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: cd frontend && pnpm install
      - run: cd frontend && pnpm test
      - run: cd frontend && pnpm build
```

### deploy-staging.yml (структура)

```yaml
name: Deploy Staging

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build and push images
        run: |
          echo ${{ secrets.GITHUB_TOKEN }} | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          # Build and push logic

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to staging
        run: |
          # SSH and deploy logic
```

### deploy-production.yml (структура)

```yaml
name: Deploy Production

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to deploy'
        required: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production  # Requires approval
    steps:
      - name: Deploy to production
        run: |
          # Deploy logic
```

### GitHub Secrets (документация)

| Secret | Описание |
|--------|----------|
| `STAGING_SSH_KEY` | SSH ключ для staging сервера |
| `PRODUCTION_SSH_KEY` | SSH ключ для production сервера |
| `SLACK_WEBHOOK` | Webhook для уведомлений |
| `CODECOV_TOKEN` | Токен для coverage reports |

### Branch Protection Rules

Для ветки `main`:
- Require pull request reviews (1 approval)
- Require status checks to pass
- Require branches to be up to date
- Include administrators

### Caching Strategy

```yaml
# Gradle caching
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v3
  # Автоматически кеширует ~/.gradle

# pnpm caching
- uses: actions/setup-node@v4
  with:
    cache: 'pnpm'
    cache-dependency-path: frontend/pnpm-lock.yaml
```

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

- Используем GitHub Actions v4 для checkout и setup actions
- Gradle wrapper должен быть закоммичен в репозиторий
- pnpm version должна быть фиксированной для воспроизводимости
- Secrets никогда не логируются в workflow
- Для staging деплоя используем SSH (не показываем детали в этой задаче)
