# CI/CD Pipeline

GitHub Actions для AqStream.

## Обзор

```mermaid
flowchart LR
    PR["Pull Request"] --> CI["CI Checks"]
    CI --> Main["Merge to main"]
    Main --> Prod["Deploy Production"]
```

## Workflows

| Файл | Триггер | Действие |
| ------ | --------- | ---------- |
| `ci.yml` | PR, Push to main | Lint, Test, Build |
| `deploy-production.yml` | Push to main | Build, Deploy |
| `docs.yml` | Push (docs/**) | Validate, Build, Deploy |

Все workflows поддерживают ручной запуск через `workflow_dispatch`.

## CI Workflow

### Backend Jobs

```yaml
backend-lint:
  # Checkstyle проверки
  ./gradlew checkstyleMain checkstyleTest

backend-test:
  # Unit и интеграционные тесты
  # Services: PostgreSQL 16, Redis 7
  ./gradlew test

backend-build:
  # Сборка JAR файлов
  # Запускается после успешных lint и test
  ./gradlew build -x test
```

### Frontend Jobs

```yaml
frontend-lint:
  # ESLint и TypeScript проверки
  pnpm lint
  pnpm typecheck

frontend-test:
  # Unit тесты (Vitest)
  pnpm test

frontend-build:
  # Production сборка
  # Запускается после успешных lint и test
  pnpm build
```

### Concurrency

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

Отменяет предыдущие запуски при новых коммитах в том же PR.

## Quality Gates

| Check | Required | Description |
| ------- | ---------- | ------------- |
| Backend Lint (Checkstyle) | Yes | Code style |
| Backend Tests | Yes | JUnit 5 |
| Frontend Lint (ESLint) | Yes | Code style |
| Frontend TypeCheck | Yes | TypeScript |
| Frontend Tests | Yes | Vitest |

## Deploy to Production

Автоматический деплой при push в `main`:

### Build Job

1. **Build JARs** — `./gradlew bootJar`
2. **Build Docker image** — Gateway (остальные сервисы будут добавлены позже)
3. **Push to GHCR** — `ghcr.io/<repo>/gateway:<sha>`

### Deploy Job

1. **SSH** — подключение к production серверу
2. **Pull images** — `docker compose pull`
3. **Restart** — `docker compose up -d`
4. **Health Check** — проверка `https://api.aqstream.ru/actuator/health`

### Image Tags

```text
ghcr.io/egorov-ma/aqstream/gateway:abc1234
ghcr.io/egorov-ma/aqstream/gateway:latest
```

## Deploy Documentation

Автоматический деплой документации при изменениях в `docs/`:

1. **Trigger** — push to main с изменениями в `docs/**`
2. **Validate** — Markdownlint, OpenAPI Spectral (не блокирует деплой)
3. **Build** — MkDocs + Material theme
4. **Deploy** — rsync на production сервер в `/var/www/docs.aqstream.ru`

```yaml
on:
  push:
    branches: [main]
    paths: ['docs/**']

jobs:
  validate:
    # Markdownlint + Spectral (continue-on-error: true)
  build:
    steps:
      - uses: actions/setup-python@v5
      - run: pip install -r docs/_internal/doc-as-code/requirements.txt
      - run: mkdocs build
  deploy:
    steps:
      - run: rsync -avz --delete site/ $SSH_USER@$SSH_HOST:/var/www/docs.aqstream.ru/
```

См. полный пример в [Server Setup](./server-setup.md).

## Allure Reports

Автоматическая генерация и публикация Allure отчётов о тестировании:

1. **Trigger** — каждый push в `main` после успешных тестов
2. **Collect** — Backend Tests собирает результаты в артефакт `allure-results`
3. **Generate** — Allure Report job генерирует HTML из результатов
4. **Deploy** — публикация на GitHub Pages через `actions/deploy-pages`

**URL:** https://egorov-ma.github.io/aqstream/allure/

Подробнее: [GitHub Pages Setup](./github-pages-setup.md)

## GitHub Secrets

| Secret | Description | Required For |
| -------- | ------------- | -------------- |
| `GITHUB_TOKEN` | Автоматически предоставляется | GHCR push |
| `SSH_HOST` | IP-адрес или домен сервера | deploy-production, docs |
| `SSH_USER` | Пользователь SSH | deploy-production, docs |
| `SSH_KEY` | Приватный SSH ключ | deploy-production, docs |

## Branch Protection Rules

Для ветки `main`:

- [x] Require pull request reviews (1 approval)
- [x] Require status checks to pass
  - `backend-lint`
  - `backend-test`
  - `backend-build`
  - `frontend-lint`
  - `frontend-test`
  - `frontend-build`
- [x] Require branches to be up to date
- [x] Include administrators

## Caching

### Gradle

```yaml
- uses: gradle/actions/setup-gradle@v4
  # Автоматически кэширует:
  # - ~/.gradle/caches
  # - ~/.gradle/wrapper
```

### pnpm

```yaml
- uses: actions/setup-node@v4
  with:
    cache: 'pnpm'
    cache-dependency-path: frontend/pnpm-lock.yaml
```

## Troubleshooting

### CI падает на checkstyle

```bash
# Локально проверить
./gradlew checkstyleMain checkstyleTest

# Посмотреть отчёт
open build/reports/checkstyle/main.html
```

### Тесты падают в CI но проходят локально

1. Проверить environment variables в workflow
2. Убедиться что PostgreSQL/Redis доступны (services)
3. Проверить таймауты health checks

### Docker build падает

1. Убедиться что Dockerfile существует в сервисе
2. Проверить что JAR собрался (`./gradlew bootJar`)
3. Проверить права на GHCR

## Дальнейшее чтение

- [Deploy](./deploy.md) — процесс деплоя
- [Environments](./environments.md) — окружения
- [Runbook](./runbook.md) — операционные процедуры
