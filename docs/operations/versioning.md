# Версионирование в Operations

## Мониторинг версий

### Health Check с версией

Endpoint `/actuator/info` включает информацию о версии:

```bash
# Получить версию отдельного сервиса
curl http://localhost:8081/actuator/info | jq '.build'

# Получить все версии через Gateway
curl http://localhost:8080/api/v1/system/version | jq '.services'
```

### Docker образы

Версии Docker образов следуют semantic versioning:

```
ghcr.io/aqstream/user-service:0.1.0-SNAPSHOT
ghcr.io/aqstream/user-service:1.0.0
ghcr.io/aqstream/user-service:latest
```

## Релизный процесс

### Обновление версии

1. Обновить `gradle.properties`:
   ```properties
   version=1.0.0
   ```

2. Обновить `frontend/package.json`:
   ```json
   "version": "1.0.0"
   ```

3. Создать tag:
   ```bash
   git tag -a v1.0.0 -m "Release 1.0.0"
   git push origin v1.0.0
   ```

### CI/CD Pipeline

GitHub Actions автоматически:
1. Собирает артефакты с версией из tag
2. Публикует Docker images с tag версии
3. Обновляет Kubernetes deployments

## Проверка версий на production

### Через браузер

Откройте консоль браузера (F12) и введите:

```javascript
AqStream.versions()
```

### Через API

```bash
# Агрегированные версии всех сервисов
curl https://api.aqstream.ru/api/v1/system/version

# Версия конкретного сервиса
curl https://api.aqstream.ru/api/v1/system/version/gateway
```

### Через Actuator

```bash
# User Service
curl http://user-service:8081/actuator/info

# Event Service
curl http://event-service:8082/actuator/info
```

## Troubleshooting

### Сервис показывает "unknown" версию

1. Проверить что `springBoot.buildInfo()` настроен в `build.gradle.kts`
2. Проверить что `build-info.properties` генерируется:
   ```bash
   ls services/user-service/user-service-service/build/resources/main/META-INF/
   ```
3. Убедиться что приложение собрано через Gradle, не IDE

### Git информация не отображается

1. Проверить что плагин `com.gorylenko.gradle-git-properties` добавлен
2. Проверить что `git.properties` генерируется:
   ```bash
   cat services/user-service/user-service-service/build/resources/main/git.properties
   ```

### Frontend показывает неправильную версию

1. Проверить `package.json` — поле `version`
2. Пересобрать приложение: `pnpm build`
3. Проверить environment переменные в runtime

## Environment переменные

| Переменная | Описание | Default |
|------------|----------|---------|
| `AQSTREAM_ENVIRONMENT` | Окружение (development/staging/production) | development |
| `NEXT_PUBLIC_APP_VERSION` | Версия фронтенда | из package.json |
| `NEXT_PUBLIC_BUILD_TIME` | Время сборки | текущее время |
| `NEXT_PUBLIC_GIT_COMMIT` | Git commit hash | local |
