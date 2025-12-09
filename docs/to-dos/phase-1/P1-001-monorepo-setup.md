# P1-001 Настройка монорепозитория

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Инфраструктура](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

AqStream — микросервисная платформа с 7 backend-сервисами и frontend-приложением. Для эффективной разработки и CI/CD необходима структура монорепозитория, позволяющая:
- Атомарные изменения через несколько сервисов
- Единый code review процесс
- Шаринг общего кода между сервисами
- Консистентные версии зависимостей

### Технический контекст

Текущее состояние:
- Репозиторий инициализирован
- Документация создана
- Код сервисов отсутствует

Целевое состояние:
- Gradle multi-module проект
- Структура директорий для services/, common/, frontend/
- Базовые конфигурационные файлы

## Цель

Создать структуру монорепозитория с корректной конфигурацией Gradle для multi-module проекта.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

- [x] Создана корневая структура директорий согласно документации
- [x] Настроен корневой `settings.gradle.kts` с включением всех модулей
- [x] Настроен корневой `build.gradle.kts` с общими зависимостями и плагинами
- [x] Создан `gradle.properties` с версиями зависимостей
- [x] Gradle wrapper настроен на актуальную версию (9.2+)
- [x] Создан `.gitignore` для Java/Gradle/Node проектов
- [x] Создан `.editorconfig` для единого стиля
- [x] Создан `Makefile` с базовыми командами (build, test, clean)
- [x] Команда `./gradlew build` успешно выполняется (пустой проект)

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Код написан согласно code style проекта
- [x] Структура соответствует документации в `docs/`
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: корневая структура
- [x] Frontend: placeholder директория
- [ ] Database: не затрагивается
- [x] Infrastructure: Makefile, .gitignore

### Структура директорий

```
aqstream/
├── services/
│   ├── gateway/
│   ├── user-service/
│   ├── event-service/
│   ├── payment-service/
│   ├── notification-service/
│   ├── media-service/
│   └── analytics-service/
├── common/
│   ├── common-api/
│   ├── common-security/
│   ├── common-data/
│   ├── common-messaging/
│   ├── common-web/
│   └── common-test/
├── frontend/
├── docker/
├── config/
│   └── checkstyle/
├── docs/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── Makefile
├── .gitignore
├── .editorconfig
└── README.md
```

### Реализованные файлы

| Файл | Описание |
|------|----------|
| `settings.gradle.kts` | pluginManagement + включение всех модулей |
| `build.gradle.kts` | Java 25, checkstyle, jacoco, lombok |
| `gradle.properties` | Централизованные версии зависимостей |
| `Makefile` | build, test, clean, infra-*, health, dev |
| `.gitignore` | IDE, Java, Gradle, Node, secrets |
| `.editorconfig` | Code style по типам файлов |
| `config/checkstyle/checkstyle.xml` | Google Style с модификациями |

### Ключевые версии (реализованные)

| Компонент | Версия |
|-----------|--------|
| Java | 25 |
| Gradle | 9.2.1 |
| Spring Boot | 3.5.8 |
| Spring Cloud | 2025.0.0 |

## Зависимости

### Блокирует

- [P1-002] Docker Compose для локальной разработки
- [P1-003] CI/CD pipeline
- [P1-004] Gradle multi-module структура сервисов
- [P1-005] Common modules

### Зависит от

- Нет зависимостей (первая задача)

## Out of Scope

- Реализация кода сервисов (только структура)
- Docker конфигурация (отдельная задача P1-002)
- CI/CD настройка (отдельная задача P1-003)
- Содержимое common модулей (отдельная задача P1-005)

## Заметки

- Используем Kotlin DSL для Gradle (`.kts`) как более type-safe альтернативу Groovy
- Версии плагинов централизованы через `pluginManagement` в settings.gradle.kts
- Makefile расширен командами для инфраструктуры и health checks
