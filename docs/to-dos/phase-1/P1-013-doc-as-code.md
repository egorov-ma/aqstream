# P1-013 Doc-as-Code Infrastructure

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
| Приоритет | `high` |
| Связь с roadmap | [Инфраструктура - Базовая документация](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Документация — критическая часть open-source проекта:
- Помогает новым контрибьюторам быстро разобраться
- Снижает барьер входа для пользователей
- Обеспечивает единый источник истины
- Автоматизация гарантирует актуальность

Doc-as-code подход:
- Документация версионируется вместе с кодом
- Изменения проходят review
- Автоматическая публикация при merge
- Валидация на CI

### Технический контекст

Стек:
- **MkDocs** — генератор статических сайтов
- **Material for MkDocs** — тема с rich features
- **GitHub Pages** — хостинг документации
- **Mermaid** — диаграммы из кода
- **OpenAPI/Swagger** — API документация

Структура уже существует в `docs/_internal/`, но требует:
- Настройка генераторов
- Настройка валидаторов
- CI workflow для деплоя
- Локальная разработка документации

## Цель

Развернуть полную doc-as-code инфраструктуру с автоматической генерацией и публикацией документации.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [ ] P1-001 завершена (структура репозитория)

## Acceptance Criteria

### MkDocs Setup

- [ ] `mkdocs.yml` финализирован с полной навигацией
- [ ] Material theme настроена (dark/light mode)
- [ ] Поиск на русском языке работает
- [ ] Mermaid диаграммы рендерятся
- [ ] Code highlighting настроен
- [ ] `mkdocs serve` работает локально

### Generators

- [ ] `generate-openapi.sh` — скачивание OpenAPI specs из сервисов
- [ ] `generate-redoc.sh` — генерация ReDoc HTML из specs
- [ ] `generate-changelog.sh` — генерация changelog из commits (опционально)

### Validators

- [ ] `validate-markdown.sh` — проверка markdown (markdownlint)
- [ ] `validate-openapi.sh` — валидация OpenAPI specs (spectral)
- [ ] `validate-links.sh` — проверка битых ссылок

### CI/CD

- [ ] `.github/workflows/docs.yml` — деплой на GitHub Pages
- [ ] Валидация документации на PR
- [ ] Автодеплой при merge в main
- [ ] Preview deployments для PR (опционально)

### Developer Experience

- [ ] Makefile команды для работы с документацией
- [ ] Pre-commit hooks для валидации
- [ ] Инструкции в CONTRIBUTING.md

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Документация доступна на GitHub Pages
- [ ] Валидация проходит на CI
- [ ] Локальная разработка документации работает
- [ ] Code review пройден
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается напрямую
- [ ] Frontend: не затрагивается
- [ ] Database: не затрагивается
- [x] Infrastructure: CI/CD, скрипты, конфигурации

### Структура файлов

```
docs/
├── _internal/
│   ├── README.md              # Документация инфраструктуры
│   ├── mkdocs.yml             # MkDocs конфигурация
│   ├── generators/
│   │   ├── generate-openapi.sh
│   │   ├── generate-redoc.sh
│   │   └── generate-changelog.sh
│   ├── validators/
│   │   ├── validate-markdown.sh
│   │   ├── validate-openapi.sh
│   │   ├── validate-links.sh
│   │   └── spectral.yaml      # Spectral rules
│   └── doc-as-code/
│       ├── pre-commit-config.yaml
│       └── requirements.txt   # Python dependencies
├── tech-stack/backend/api/
│   ├── specs/                 # OpenAPI YAML files
│   └── redoc/                 # Generated ReDoc HTML
└── ...

.github/
└── workflows/
    └── docs.yml               # Documentation CI/CD

Makefile                       # Команды для документации
```

### MkDocs Configuration (финальная)

```yaml
# docs/_internal/mkdocs.yml
site_name: AqStream Documentation
site_description: Документация платформы управления мероприятиями AqStream
site_url: https://docs.aqstream.ru
repo_url: https://github.com/aqstream/aqstream
repo_name: aqstream/aqstream
docs_dir: ..

theme:
  name: material
  language: ru
  palette:
    - scheme: default
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-7
        name: Тёмная тема
    - scheme: slate
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-4
        name: Светлая тема
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.top
    - navigation.indexes
    - search.highlight
    - search.suggest
    - content.code.copy
    - content.tabs.link

plugins:
  - search:
      lang: ru

markdown_extensions:
  - pymdownx.highlight:
      anchor_linenums: true
      line_spans: __span
      pygments_lang_class: true
  - pymdownx.inlinehilite
  - pymdownx.snippets
  - pymdownx.superfences:
      custom_fences:
        - name: mermaid
          class: mermaid
          format: !!python/name:pymdownx.superfences.fence_code_format
  - pymdownx.tabbed:
      alternate_style: true
  - pymdownx.details
  - pymdownx.mark
  - admonition
  - tables
  - attr_list
  - md_in_html
  - toc:
      permalink: true
      toc_depth: 3

nav:
  - Home: README.md
  - Архитектура:
    - architecture/overview.md
    - architecture/service-topology.md
    - architecture/data-architecture.md
  - Бизнес:
    - business/vision.md
    - business/user-journeys.md
    - business/functional-requirements.md
    - business/roadmap.md
  - Данные:
    - data/domain-model.md
    - data/migrations.md
  - Tech Stack:
    - tech-stack/overview.md
    - tech-stack/tooling.md
    - Backend:
      - tech-stack/backend/architecture.md
      - tech-stack/backend/api-guidelines.md
      - tech-stack/backend/common-library.md
      - tech-stack/backend/service-template.md
      - API: tech-stack/backend/api/README.md
      - Сервисы:
        - tech-stack/backend/services/gateway.md
        - tech-stack/backend/services/user-service.md
        - tech-stack/backend/services/event-service.md
        - tech-stack/backend/services/payment-service.md
        - tech-stack/backend/services/notification-service.md
        - tech-stack/backend/services/media-service.md
        - tech-stack/backend/services/analytics-service.md
    - Frontend:
      - tech-stack/frontend/architecture.md
      - tech-stack/frontend/components.md
    - QA:
      - tech-stack/qa/testing-strategy.md
  - Operations:
    - operations/environments.md
    - operations/deploy.md
    - operations/ci-cd.md
    - operations/observability.md
    - Runbooks:
      - operations/runbooks/service-restart.md
      - operations/runbooks/incident-response.md
      - operations/runbooks/backup-restore.md
  - Community:
    - experience/security.md
    - experience/contributing.md
    - experience/community.md
  - To-Dos:
    - to-dos/README.md

extra:
  social:
    - icon: fontawesome/brands/github
      link: https://github.com/aqstream/aqstream
    - icon: fontawesome/brands/telegram
      link: https://t.me/aqstream
```

### generate-openapi.sh

```bash
#!/bin/bash
# docs/_internal/generators/generate-openapi.sh
# Скачивает OpenAPI спецификации из работающих сервисов

set -e

SERVICES=(
    "gateway:8080"
    "user-service:8081"
    "event-service:8082"
    "payment-service:8083"
    "notification-service:8084"
    "media-service:8085"
    "analytics-service:8086"
)

OUTPUT_DIR="$(dirname "$0")/../../tech-stack/backend/api/specs"
mkdir -p "$OUTPUT_DIR"

echo "Скачивание OpenAPI спецификаций..."

for service in "${SERVICES[@]}"; do
    name="${service%%:*}"
    port="${service##*:}"

    echo "  → $name (порт $port)..."

    if curl -s --fail "http://localhost:$port/v3/api-docs.yaml" > "$OUTPUT_DIR/$name.yaml" 2>/dev/null; then
        echo "    ✓ Сохранено: $OUTPUT_DIR/$name.yaml"
    else
        echo "    ✗ Сервис недоступен"
        rm -f "$OUTPUT_DIR/$name.yaml"
    fi
done

echo ""
echo "Готово! Спецификации в $OUTPUT_DIR"
```

### generate-redoc.sh

```bash
#!/bin/bash
# docs/_internal/generators/generate-redoc.sh
# Генерирует ReDoc HTML из OpenAPI specs

set -e

SPECS_DIR="$(dirname "$0")/../../tech-stack/backend/api/specs"
OUTPUT_DIR="$(dirname "$0")/../../tech-stack/backend/api/redoc"
mkdir -p "$OUTPUT_DIR"

echo "Генерация ReDoc HTML..."

# Проверяем наличие redocly CLI
if ! command -v npx &> /dev/null; then
    echo "Ошибка: npx не найден. Установите Node.js"
    exit 1
fi

for spec in "$SPECS_DIR"/*.yaml; do
    if [ -f "$spec" ]; then
        name=$(basename "$spec" .yaml)
        echo "  → $name..."
        npx @redocly/cli build-docs "$spec" \
            --output "$OUTPUT_DIR/$name.html" \
            --title "AqStream $name API" \
            2>/dev/null
        echo "    ✓ Сохранено: $OUTPUT_DIR/$name.html"
    fi
done

echo ""
echo "Готово! HTML файлы в $OUTPUT_DIR"
```

### validate-markdown.sh

```bash
#!/bin/bash
# docs/_internal/validators/validate-markdown.sh
# Валидация markdown файлов

set -e

DOCS_DIR="$(dirname "$0")/../.."

echo "Валидация Markdown..."

# Проверяем наличие markdownlint
if ! command -v npx &> /dev/null; then
    echo "Ошибка: npx не найден"
    exit 1
fi

# Запускаем markdownlint
npx markdownlint-cli2 "$DOCS_DIR/**/*.md" \
    --ignore "$DOCS_DIR/_internal/**" \
    --ignore "$DOCS_DIR/**/node_modules/**"

echo "✓ Markdown валидация пройдена"
```

### validate-openapi.sh

```bash
#!/bin/bash
# docs/_internal/validators/validate-openapi.sh
# Валидация OpenAPI спецификаций с помощью Spectral

set -e

SPECS_DIR="$(dirname "$0")/../../tech-stack/backend/api/specs"
RULES_FILE="$(dirname "$0")/spectral.yaml"

echo "Валидация OpenAPI спецификаций..."

if [ ! -d "$SPECS_DIR" ] || [ -z "$(ls -A "$SPECS_DIR" 2>/dev/null)" ]; then
    echo "⚠ Нет спецификаций для валидации"
    exit 0
fi

ERRORS=0
for spec in "$SPECS_DIR"/*.yaml; do
    if [ -f "$spec" ]; then
        name=$(basename "$spec")
        echo "  → $name..."

        if npx @stoplight/spectral-cli lint "$spec" --ruleset "$RULES_FILE" 2>/dev/null; then
            echo "    ✓ OK"
        else
            echo "    ✗ Ошибки найдены"
            ERRORS=$((ERRORS + 1))
        fi
    fi
done

if [ $ERRORS -gt 0 ]; then
    echo ""
    echo "✗ Найдены ошибки в $ERRORS файлах"
    exit 1
fi

echo ""
echo "✓ OpenAPI валидация пройдена"
```

### spectral.yaml

```yaml
# docs/_internal/validators/spectral.yaml
extends: [[spectral:oas, recommended]]

rules:
  # Обязательные
  operation-operationId:
    severity: error
  operation-tags:
    severity: error
  info-contact:
    severity: warn

  # Описания
  operation-description:
    severity: warn
  oas3-schema-description:
    severity: hint

  # Пути и параметры
  path-params:
    severity: error
  path-keys-no-trailing-slash:
    severity: warn

  # Отключаем слишком строгие правила
  info-license:
    severity: off
```

### GitHub Actions Workflow

```yaml
# .github/workflows/docs.yml
name: Documentation

on:
  push:
    branches: [main]
    paths:
      - 'docs/**'
      - '.github/workflows/docs.yml'
  pull_request:
    branches: [main]
    paths:
      - 'docs/**'

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Validate Markdown
        run: |
          npx markdownlint-cli2 "docs/**/*.md" \
            --ignore "docs/_internal/**" \
            --ignore "**/node_modules/**"
        continue-on-error: true

      - name: Validate OpenAPI (if specs exist)
        run: |
          if [ -d "docs/tech-stack/backend/api/specs" ] && [ -n "$(ls -A docs/tech-stack/backend/api/specs 2>/dev/null)" ]; then
            npx @stoplight/spectral-cli lint "docs/tech-stack/backend/api/specs/*.yaml" \
              --ruleset "docs/_internal/validators/spectral.yaml"
          else
            echo "No OpenAPI specs to validate"
          fi
        continue-on-error: true

  build:
    runs-on: ubuntu-latest
    needs: validate
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'

      - name: Install dependencies
        run: |
          pip install mkdocs mkdocs-material pymdown-extensions

      - name: Build documentation
        run: |
          cd docs
          mkdocs build -f _internal/mkdocs.yml -d ../site

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: site

  deploy:
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    needs: build
    permissions:
      pages: write
      id-token: write
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

### Makefile команды

```makefile
# Добавить в корневой Makefile

# Documentation
.PHONY: docs-serve docs-build docs-validate docs-openapi docs-redoc

docs-serve:
	cd docs && mkdocs serve -f _internal/mkdocs.yml

docs-build:
	cd docs && mkdocs build -f _internal/mkdocs.yml -d ../site

docs-validate:
	./docs/_internal/validators/validate-markdown.sh
	./docs/_internal/validators/validate-openapi.sh

docs-openapi:
	./docs/_internal/generators/generate-openapi.sh

docs-redoc:
	./docs/_internal/generators/generate-redoc.sh
```

### requirements.txt

```
# docs/_internal/doc-as-code/requirements.txt
mkdocs>=1.5.0
mkdocs-material>=9.5.0
pymdown-extensions>=10.0
```

### .markdownlint.json (в корне проекта)

```json
{
  "default": true,
  "MD013": false,
  "MD033": false,
  "MD041": false,
  "MD024": {
    "siblings_only": true
  }
}
```

## Зависимости

### Блокирует

- Публикация документации
- Onboarding новых контрибьюторов

### Зависит от

- [P1-001] Настройка монорепозитория
- [P1-003] CI/CD pipeline (частично, для docs workflow)

## Out of Scope

- Перевод документации на английский
- Versioned docs (несколько версий документации)
- API mocking из OpenAPI
- Автогенерация SDK из OpenAPI
- Интеграция с Confluence/Notion

## Заметки

- MkDocs Material — самая популярная тема для технической документации
- Mermaid диаграммы позволяют версионировать диаграммы вместе с кодом
- Spectral — стандарт для линтинга OpenAPI
- GitHub Pages бесплатен для open-source
- Валидация на PR предотвращает merge сломанной документации
- Скрипты генерации требуют запущенных сервисов
