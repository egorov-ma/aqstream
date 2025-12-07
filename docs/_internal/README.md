# Internal Documentation

Внутренняя документация для генерации сайта документации.

## MkDocs

Документация генерируется с помощью [MkDocs](https://www.mkdocs.org/) с темой [Material](https://squidfunk.github.io/mkdocs-material/).

### Установка

```bash
pip install mkdocs mkdocs-material
```

### Локальный запуск

```bash
# Из корня проекта
mkdocs serve

# Или из директории docs
cd docs && mkdocs serve -f _internal/mkdocs.yml
```

Документация будет доступна на http://localhost:8000

### Сборка

```bash
mkdocs build
```

Статические файлы будут в `site/`.

## Структура

```
docs/
├── _internal/          # Конфигурация MkDocs
│   ├── README.md       # Этот файл
│   └── mkdocs.yml      # Конфигурация
├── architecture/       # Архитектура
├── business/           # Бизнес-документация
├── data/               # Модели данных
├── experience/         # Community, Contributing
├── operations/         # DevOps
├── tech-stack/         # Технический стек
└── README.md           # Навигация
```

## Правила написания

### Формат

- Markdown
- UTF-8
- LF line endings

### Язык

- Документация на русском
- Технические термины на английском
- Код на английском

### Стиль

- Краткие предложения
- Практические примеры
- Минимум воды

### Ссылки

```markdown
# Относительные ссылки
[Architecture](../architecture/overview.md)

# Якоря
[JWT Tokens](#jwt-tokens)
```

## CI/CD

Документация автоматически деплоится при merge в main:

```yaml
# .github/workflows/docs.yml
name: Deploy Docs

on:
  push:
    branches: [main]
    paths:
      - 'docs/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      - run: pip install mkdocs mkdocs-material
      - run: mkdocs build
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./site
```

## Обновление документации

1. Внести изменения в markdown файлы
2. Проверить локально: `mkdocs serve`
3. Создать PR
4. После merge — автодеплой
