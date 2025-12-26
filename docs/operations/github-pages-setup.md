# Настройка GitHub Pages для Allure Reports

Allure отчёты о тестировании автоматически публикуются на GitHub Pages после каждого успешного прогона тестов в ветке `main`.

## Автоматическая настройка

GitHub Pages настраивается **автоматически** через CI/CD workflow (`actions/deploy-pages`). После первого деплоя необходимо один раз **включить** Pages в настройках репозитория.

## Настройка через GitHub UI

### Шаг 1: Открыть Settings

1. Перейти в репозиторий: https://github.com/egorov-ma/aqstream
2. Нажать **Settings** в верхнем меню
3. В левом меню выбрать **Pages**

### Шаг 2: Выбрать Source

В разделе **Build and deployment**:

1. **Source:** выбрать `GitHub Actions`
2. Нажать **Save** (если кнопка доступна)

![GitHub Pages Settings](https://docs.github.com/assets/cb-47267/mw-1440/images/help/pages/publishing-source-drop-down.webp)

### Шаг 3: Проверить статус

После настройки GitHub автоматически задеплоит страницы при следующем push в `main`.

Статус деплоя можно проверить в Actions: https://github.com/egorov-ma/aqstream/actions

## Доступ к Allure Reports

После успешного деплоя отчёты доступны по адресу:

**https://egorov-ma.github.io/aqstream/allure/**

## Как это работает

1. **Backend Tests** (`.github/workflows/cicd.yml`) запускает тесты и собирает результаты в артефакт `allure-results`
2. **Allure Report** job:
   - Скачивает результаты тестов
   - Устанавливает Allure CLI
   - Генерирует HTML отчёт в `_site/allure/`
   - Загружает артефакт через `actions/upload-pages-artifact@v3`
   - Деплоит на GitHub Pages через `actions/deploy-pages@v4`
3. **GitHub Pages** автоматически публикует отчёт

## Дополнительная информация

- **GitHub Pages Docs:** https://docs.github.com/en/pages
- **Allure Framework:** https://docs.qameta.io/allure/
- **Actions Workflow:** [`.github/workflows/cicd.yml`](../../.github/workflows/cicd.yml)
