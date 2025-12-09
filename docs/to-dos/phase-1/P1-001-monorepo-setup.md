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
- [ ] CI pipeline проходит (если настроен)
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

### Ключевые версии

| Компонент | Версия |
|-----------|--------|
| Java | 25 (LTS) |
| Gradle | 9.2.x |
| Spring Boot | 3.5.x |
| Kotlin DSL | для Gradle scripts |

### settings.gradle.kts (пример)

```kotlin
rootProject.name = "aqstream"

// Common modules
include("common:common-api")
include("common:common-security")
include("common:common-data")
include("common:common-messaging")
include("common:common-web")
include("common:common-test")

// Services
include("services:gateway")
include("services:user-service")
include("services:event-service")
include("services:payment-service")
include("services:notification-service")
include("services:media-service")
include("services:analytics-service")
```

### Makefile команды

```makefile
.PHONY: build test clean

build:
	./gradlew build

test:
	./gradlew test

clean:
	./gradlew clean
```

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
- Checkstyle конфигурация будет добавлена в `config/checkstyle/`
- README.md в корне должен содержать quick start инструкции
