# Версионирование

Система версионирования приложений AqStream.

## Обзор

Каждый микросервис предоставляет информацию о своей версии через:
- `/api/v1/system/version` — REST endpoint
- `/actuator/info` — Actuator endpoint

Gateway агрегирует версии всех сервисов в единый endpoint.

## Semantic Versioning

Используем [Semantic Versioning 2.0.0](https://semver.org/):

```
MAJOR.MINOR.PATCH[-QUALIFIER]
```

| Часть | Когда увеличивать |
|-------|-------------------|
| MAJOR | Breaking changes в API |
| MINOR | Новая функциональность, backward compatible |
| PATCH | Bug fixes, backward compatible |
| QUALIFIER | SNAPSHOT, RC1, BETA |

### Примеры

- `0.1.0-SNAPSHOT` — разработка
- `1.0.0-RC1` — релиз-кандидат
- `1.0.0` — первый стабильный релиз
- `1.1.0` — новые фичи
- `1.1.1` — исправление багов

## Backend

### Build Info

Gradle генерирует `META-INF/build-info.properties` автоматически через `springBoot.buildInfo()`:

```properties
build.artifact=user-service
build.group=ru.aqstream
build.name=user-service
build.version=0.1.0-SNAPSHOT
build.time=2024-01-15T10:30:00Z
```

### Git Properties (временно отключено)

> **Примечание:** Плагин `gradle-git-properties` временно отключён из-за несовместимости с Java 25.
> Git информация (commit, branch) пока недоступна в runtime. Когда плагин будет обновлён, его можно будет включить обратно.

При включении плагин генерирует `git.properties`:

```properties
git.branch=main
git.commit.id.abbrev=a1b2c3d
git.commit.time=2024-01-15T10:00:00Z
git.dirty=false
```

### API Endpoints

**Отдельный сервис:**
```json
GET /api/v1/system/version

{
  "name": "user-service",
  "version": "0.1.0-SNAPSHOT",
  "buildTime": "2024-01-15T10:30:00Z",
  "gitCommit": "a1b2c3d",
  "gitBranch": "main",
  "gitCommitTime": "2024-01-15T10:00:00Z",
  "javaVersion": "25",
  "springBootVersion": "3.5.8"
}
```

**Gateway (агрегация):**
```json
GET /api/v1/system/version

{
  "platform": "AqStream",
  "environment": "development",
  "timestamp": "2024-01-15T12:00:00Z",
  "gateway": { ... },
  "services": {
    "user-service": { ... },
    "event-service": { ... },
    "payment-service": { ... },
    "notification-service": { ... },
    "media-service": { ... },
    "analytics-service": { ... }
  },
  "infrastructure": {
    "redis": "7.2.5"
  }
}
```

### Actuator /info

Actuator endpoint `/actuator/info` также предоставляет информацию о версии:

```json
GET /actuator/info

{
  "build": {
    "artifact": "user-service",
    "name": "user-service",
    "version": "0.1.0-SNAPSHOT",
    "time": "2024-01-15T10:30:00Z"
  },
  "git": {
    "branch": "main",
    "commit": {
      "id": "a1b2c3d",
      "time": "2024-01-15T10:00:00Z"
    }
  },
  "java": {
    "version": "25"
  }
}
```

## Frontend

Версия доступна через environment переменные:

```typescript
const version = process.env.NEXT_PUBLIC_APP_VERSION; // "0.1.0"
const buildTime = process.env.NEXT_PUBLIC_BUILD_TIME; // ISO timestamp
const gitCommit = process.env.NEXT_PUBLIC_GIT_COMMIT; // "a1b2c3d"
```

### Console Welcome Message

При загрузке приложения в консоль браузера выводится:

```
   ___       _____ _
  / _ \     /  ___| |
 / /_\ \ __ \ `--.| |_ _ __ ___  __ _ _ __ ___
 |  _  |/ _\ `--. \ __| '__/ _ \/ _` | '_ ` _ \
 | | | | (_| /\__/ / |_| | |  __/ (_| | | | | | |
 \_| |_/\__, \____/ \__|_|  \___|\__,_|_| |_| |_|

AqStream Platform
Фронтенд: v0.1.0 (a1b2c3d)
Сборка: 15.01.2024, 12:00

Для информации о версиях сервисов: AqStream.versions()
```

### Команда AqStream.versions()

В консоли браузера доступна команда для просмотра версий всех сервисов:

```javascript
AqStream.versions()
```

Выводит таблицу с версиями всех компонентов системы.

## Конфигурация

### Gradle

Build info и git properties настроены глобально в корневом `build.gradle.kts`:

```kotlin
// build.gradle.kts
subprojects {
    plugins.withId("org.springframework.boot") {
        configure<SpringBootExtension> {
            buildInfo()
        }
    }

    plugins.withId("com.gorylenko.gradle-git-properties") {
        configure<GitPropertiesPluginExtension> {
            keys = listOf("git.branch", "git.commit.id.abbrev", ...)
        }
    }
}
```

### Application Properties

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  info:
    build:
      enabled: true
    git:
      enabled: true
      mode: full
    java:
      enabled: true
```

## Ключевые файлы

| Файл | Назначение |
|------|------------|
| `gradle.properties` | Единая версия проекта |
| `build.gradle.kts` | Конфигурация buildInfo/gitProperties |
| `common-api/.../version/` | DTO для версий |
| `common-web/.../version/` | VersionController для сервисов |
| `gateway/.../version/` | Агрегация версий |
| `frontend/lib/utils/welcome.ts` | Console welcome message |
