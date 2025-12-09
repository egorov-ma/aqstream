# P1-004 Структура Gradle Multi-Module проекта

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
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
- [ ] P1-001 завершена

## Acceptance Criteria

- [ ] Создана структура директорий для всех сервисов и модулей
- [ ] `settings.gradle.kts` включает все модули
- [ ] Корневой `build.gradle.kts` содержит:
  - [ ] Общие плагины (Spring Boot, Spring Dependency Management)
  - [ ] Общие зависимости (Lombok, MapStruct)
  - [ ] Конфигурацию subprojects
- [ ] `gradle.properties` содержит версии всех зависимостей
- [ ] Каждый common модуль имеет базовый `build.gradle.kts`
- [ ] Каждый сервис имеет структуру подмодулей (api, service, db)
- [ ] Checkstyle plugin настроен
- [ ] JaCoCo для coverage настроен
- [ ] Команда `./gradlew build` успешно компилируется
- [ ] Команда `./gradlew test` проходит (пустые тесты)

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Структура соответствует документации `docs/tech-stack/backend/architecture.md`
- [ ] Gradle conventions применены консистентно
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

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

### gradle.properties

```properties
# Project
group=ru.aqstream
version=0.1.0-SNAPSHOT

# Java
javaVersion=25

# Spring
springBootVersion=3.5.8
springCloudVersion=2025.0.0

# Dependencies
lombokVersion=1.18.36
mapstructVersion=1.6.3
liquibaseVersion=4.31.0
springdocVersion=2.8.0
testcontainersVersion=1.20.4
junitVersion=5.12.0

# Gradle
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError
```

### build.gradle.kts (корневой)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "${springBootVersion}" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("checkstyle")
    id("jacoco")
}

allprojects {
    group = "${group}"
    version = "${version}"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")
    apply(plugin = "jacoco")

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    checkstyle {
        toolVersion = "10.21.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:${lombokVersion}")
        annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

        testImplementation("org.junit.jupiter:junit-jupiter:${junitVersion}")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}
```

### settings.gradle.kts

```kotlin
rootProject.name = "aqstream"

// Common modules
include("common:common-api")
include("common:common-security")
include("common:common-data")
include("common:common-messaging")
include("common:common-web")
include("common:common-test")

// Gateway (single module)
include("services:gateway")

// User Service
include("services:user-service:user-service-api")
include("services:user-service:user-service-service")
include("services:user-service:user-service-db")
include("services:user-service:user-service-client")

// Event Service
include("services:event-service:event-service-api")
include("services:event-service:event-service-service")
include("services:event-service:event-service-db")

// Payment Service
include("services:payment-service:payment-service-api")
include("services:payment-service:payment-service-service")
include("services:payment-service:payment-service-db")

// Notification Service
include("services:notification-service:notification-service-api")
include("services:notification-service:notification-service-service")
include("services:notification-service:notification-service-db")

// Media Service
include("services:media-service:media-service-api")
include("services:media-service:media-service-service")
include("services:media-service:media-service-db")

// Analytics Service
include("services:analytics-service:analytics-service-api")
include("services:analytics-service:analytics-service-service")
include("services:analytics-service:analytics-service-db")
```

### Пример common-api/build.gradle.kts

```kotlin
plugins {
    id("java-library")
}

dependencies {
    api("jakarta.validation:jakarta.validation-api")
    api("com.fasterxml.jackson.core:jackson-annotations")
}
```

### Пример service-api/build.gradle.kts

```kotlin
plugins {
    id("java-library")
}

dependencies {
    api(project(":common:common-api"))
}
```

### Пример service-service/build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common:common-security"))
    implementation(project(":common:common-web"))
    implementation(project(":common:common-messaging"))
    implementation(project(":services:event-service:event-service-api"))
    implementation(project(":services:event-service:event-service-db"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation(project(":common:common-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

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

- Gateway не имеет подмодулей (единственный сервис на WebFlux)
- client модуль создаётся только если сервис предоставляет API для других сервисов
- Используем `java-library` plugin для библиотечных модулей (api vs implementation)
- MapStruct processors будут добавлены в service модули
- Spring Dependency Management обеспечивает консистентные версии Spring зависимостей
