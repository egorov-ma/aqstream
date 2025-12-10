# P1-013 Doc-as-Code Infrastructure

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
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

## Цель

Развернуть полную doc-as-code инфраструктуру с автоматической генерацией и публикацией документации.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-001 завершена (структура репозитория)

## Acceptance Criteria

### MkDocs Setup

- [x] `mkdocs.yml` финализирован с полной навигацией
- [x] Material theme настроена (dark/light mode)
- [x] Поиск на русском языке работает
- [x] Mermaid диаграммы рендерятся
- [x] Code highlighting настроен
- [x] `mkdocs serve` работает локально

### Generators

- [x] `generate-openapi.sh` — скачивание OpenAPI specs из сервисов
- [x] `generate-redoc.sh` — генерация ReDoc HTML из specs
- [ ] `generate-changelog.sh` — генерация changelog из commits (опционально, не реализовано)

### Validators

- [x] `validate-markdown.sh` — проверка markdown (markdownlint)
- [x] `validate-openapi.sh` — валидация OpenAPI specs (spectral)
- [x] `validate-links.sh` — проверка битых ссылок

### CI/CD

- [x] `.github/workflows/docs.yml` — деплой на GitHub Pages
- [x] Валидация документации на PR
- [x] Автодеплой при merge в main
- [ ] Preview deployments для PR (опционально, не реализовано)

### Developer Experience

- [x] Makefile команды для работы с документацией
- [ ] Pre-commit hooks для валидации (опционально, не реализовано)
- [x] Инструкции в `_internal/README.md`

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Документация собирается локально
- [x] Валидация настроена на CI
- [x] Локальная разработка документации работает
- [x] Code review пройден
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается напрямую
- [ ] Frontend: не затрагивается
- [ ] Database: не затрагивается
- [x] Infrastructure: CI/CD, скрипты, конфигурации

### Реализованные файлы

| Файл | Описание |
|------|----------|
| `docs/_internal/mkdocs.yml` | MkDocs конфигурация (обновлена) |
| `docs/_internal/README.md` | Документация инфраструктуры (обновлена) |
| `docs/_internal/generators/generate-openapi.sh` | Скачивание OpenAPI specs |
| `docs/_internal/generators/generate-redoc.sh` | Генерация ReDoc HTML |
| `docs/_internal/validators/validate-markdown.sh` | Валидация Markdown |
| `docs/_internal/validators/validate-openapi.sh` | Валидация OpenAPI |
| `docs/_internal/validators/validate-links.sh` | Проверка ссылок |
| `docs/_internal/validators/spectral.yaml` | Правила Spectral |
| `docs/_internal/validators/link-check-config.json` | Конфигурация проверки ссылок |
| `docs/_internal/doc-as-code/requirements.txt` | Python зависимости |
| `.github/workflows/docs.yml` | CI/CD для документации |
| `.markdownlint.json` | Конфигурация markdownlint |
| `Makefile` | Команды docs-* добавлены |

### Структура docs/_internal/

```
docs/_internal/
├── README.md
├── mkdocs.yml
├── generators/
│   ├── generate-openapi.sh
│   └── generate-redoc.sh
├── validators/
│   ├── validate-markdown.sh
│   ├── validate-openapi.sh
│   ├── validate-links.sh
│   ├── spectral.yaml
│   └── link-check-config.json
└── doc-as-code/
    └── requirements.txt
```

### Makefile команды

| Команда | Описание |
|---------|----------|
| `make docs-serve` | Локальный сервер на http://localhost:8000 |
| `make docs-build` | Сборка документации в site/ |
| `make docs-validate` | Валидация Markdown и OpenAPI |
| `make docs-openapi` | Скачать OpenAPI specs из сервисов |
| `make docs-redoc` | Сгенерировать ReDoc HTML |

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
