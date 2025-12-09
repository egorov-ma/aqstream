# P1-004 Структура Gradle Multi-Module проекта

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Backend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Backend AqStream состоит из 7 микросервисов и 6 common модулей. Правильная структура Gradle проекта обеспечивает:
- Переиспользование кода через common модули
- Независимую сборку сервисов
- Консистентные версии зависимостей
- Эффективное использование Gradle cache

### Технический контекст

Каждый сервис состоит из подмодулей:
- `{service}-api` — публичные DTO, events, exceptions
- `{service}-service` — business logic, controllers
- `{service}-db` — entities, repositories, migrations
- `{service}-client` — Feign client (опционально)

Common модули:
- `common-api` — базовые DTO, exceptions
- `common-security` — JWT, TenantContext
- `common-data` — BaseEntity, auditing
- `common-messaging` — Outbox pattern
- `common-web` — exception handlers, filters
- `common-test` — test utilities

## Цель

Создать полную структуру Gradle multi-module проекта с настроенными зависимостями и плагинами.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-001 завершена

## Acceptance Criteria

- [x] Создана структура директорий для всех сервисов и модулей
- [x] `settings.gradle.kts` включает все модули
- [x] Корневой `build.gradle.kts` содержит:
  - [x] Общие плагины (Spring Boot, Spring Dependency Management)
  - [x] Общие зависимости (Lombok, MapStruct)
  - [x] Конфигурацию subprojects
- [x] `gradle.properties` содержит версии всех зависимостей
- [x] Каждый common модуль имеет базовый `build.gradle.kts`
- [x] Каждый сервис имеет структуру подмодулей (api, service, db)
- [x] Checkstyle plugin настроен
- [x] JaCoCo для coverage настроен
- [x] Команда `./gradlew build` успешно компилируется
- [x] Команда `./gradlew test` проходит (пустые тесты)

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Структура соответствует документации `docs/tech-stack/backend/architecture.md`
- [x] Gradle conventions применены консистентно
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: полная структура модулей
- [ ] Frontend: не затрагивается
- [ ] Database: не затрагивается (только структура модулей db)
- [ ] Infrastructure: не затрагивается

### Структура проекта

```
aqstream/
├── common/
│   ├── common-api/
│   │   ├── src/main/java/
│   │   └── build.gradle.kts
│   ├── common-security/
│   │   ├── src/main/java/
│   │   └── build.gradle.kts
│   ├── common-data/
│   │   ├── src/main/java/
│   │   └── build.gradle.kts
│   ├── common-messaging/
│   │   ├── src/main/java/
│   │   └── build.gradle.kts
│   ├── common-web/
│   │   ├── src/main/java/
│   │   └── build.gradle.kts
│   └── common-test/
│       ├── src/main/java/
│       └── build.gradle.kts
├── services/
│   ├── gateway/
│   │   ├── src/main/java/
│   │   ├── src/main/resources/
│   │   └── build.gradle.kts
│   ├── user-service/
│   │   ├── user-service-api/
│   │   ├── user-service-service/
│   │   ├── user-service-db/
│   │   └── user-service-client/
│   ├── event-service/
│   │   ├── event-service-api/
│   │   ├── event-service-service/
│   │   └── event-service-db/
│   ├── payment-service/
│   │   ├── payment-service-api/
│   │   ├── payment-service-service/
│   │   └── payment-service-db/
│   ├── notification-service/
│   │   ├── notification-service-api/
│   │   ├── notification-service-service/
│   │   └── notification-service-db/
│   ├── media-service/
│   │   ├── media-service-api/
│   │   ├── media-service-service/
│   │   └── media-service-db/
│   └── analytics-service/
│       ├── analytics-service-api/
│       ├── analytics-service-service/
│       └── analytics-service-db/
├── config/
│   └── checkstyle/
│       └── checkstyle.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### Реализованные модули

| Тип | Количество | Примеры |
|-----|------------|---------|
| Common | 6 | common-api, common-security, common-data, common-messaging, common-web, common-test |
| Gateway | 1 | services/gateway (WebFlux) |
| Service modules | 18 | user-service/{api,service,db}, event-service/{api,service,db}, ... |
| Client modules | 1 | user-service-client |

### Паттерны build.gradle.kts

| Тип модуля | Plugin | Основные зависимости |
|------------|--------|---------------------|
| `*-api` | `java-library` | `api(project(":common:common-api"))` |
| `*-service` | `spring-boot` + `dependency-management` | api + db + common-* + MapStruct |
| `*-db` | `java-library` + `dependency-management` | common-data + JPA + Liquibase + Testcontainers |
| `*-client` | `java-library` | api + OpenFeign |

### Конфигурация Gradle

| Файл | Содержимое |
|------|-----------|
| `settings.gradle.kts` | pluginManagement + 25 модулей |
| `build.gradle.kts` | subprojects: checkstyle, jacoco, lombok, junit |
| `gradle.properties` | Версии: Spring Boot 3.5.8, Java 25, Lombok 1.18.42 |

## Зависимости

### Блокирует

- [P1-005] Common modules implementation
- [P1-006] API Gateway setup
- Все задачи сервисов

### Зависит от

- [P1-001] Настройка монорепозитория

## Out of Scope

- Реализация кода в модулях (только структура и build.gradle.kts)
- Liquibase миграции
- Dockerfile для сервисов
- application.yml конфигурация

## Заметки

- Gateway — единственный WebFlux сервис (без подмодулей)
- `java-library` plugin для api/db модулей (`api()` vs `implementation()`)
- MapStruct processors в service модулях
- Версии централизованы в gradle.properties + pluginManagement
