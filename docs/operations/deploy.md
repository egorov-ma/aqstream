# Deploy

Процесс деплоя AqStream.

## Обзор

```mermaid
flowchart LR
    Dev["Development"] --> PR["Pull Request"]
    PR --> CI["CI Tests"]
    CI --> Main["Merge to main"]
    Main --> Prod["Deploy Production"]
```

## Docker Images

### Сборка

```dockerfile
# services/event-service/Dockerfile
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY build/libs/event-service.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Multi-stage build

```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew :services:event-service:bootJar -x test

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/services/event-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Tagging

```bash
# Format: ghcr.io/aqstream/{service}:{version}
ghcr.io/aqstream/event-service:1.0.0
ghcr.io/aqstream/event-service:1.0.0-sha-abc1234
ghcr.io/aqstream/event-service:latest
```

## CI/CD Pipeline

### Build & Test

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew test

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with:
          version: 9
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'pnpm'
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: cd frontend && pnpm install
      - run: cd frontend && pnpm test
      - run: cd frontend && pnpm build
```

### Deploy to Production

```yaml
# .github/workflows/deploy-production.yml
name: Deploy Production

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - run: ./gradlew bootJar
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          context: ./services/gateway
          push: true
          tags: ghcr.io/${{ github.repository }}/gateway:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /app
            docker compose pull
            docker compose up -d
```

## Deployment Strategy

### Rolling Update

> **TODO:** Rolling update будет настроен после добавления всех сервисов в production pipeline.

```yaml
# Пример конфигурации для Docker Swarm / Compose deploy
services:
  event-service:
    image: ghcr.io/aqstream/event-service:${VERSION}
    deploy:
      replicas: 2
      update_config:
        parallelism: 1
        delay: 10s
        failure_action: rollback
      rollback_config:
        parallelism: 1
        delay: 10s
```

### Health Checks

```yaml
services:
  event-service:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

## Database Migrations

### Автоматические

Миграции применяются автоматически при старте сервиса:

```yaml
spring:
  liquibase:
    enabled: true
```

### Ручные (для сложных миграций)

```bash
./gradlew :services:event-service:event-service-db:update -Penv=production
```

## Rollback

### Быстрый rollback

```bash
# Откатить к предыдущей версии
docker compose pull event-service:previous-version
docker compose up -d event-service
```

### Database rollback

```bash
# Откатить последнюю миграцию
./gradlew :services:event-service:liquibaseRollbackCount -PliquibaseCommandValue=1
```

## Secrets Management

### GitHub Secrets

```
GITHUB_TOKEN          # GitHub Container Registry
DATABASE_URL          # PostgreSQL connection string
REDIS_URL             # Redis connection string
JWT_SECRET            # JWT signing key
PAYMENT_API_KEY       # Payment provider API key
TELEGRAM_BOT_TOKEN    # Telegram Bot API token
```

### Environment injection

```yaml
services:
  event-service:
    environment:
      - DATABASE_URL=${DATABASE_URL}
      - JWT_SECRET=${JWT_SECRET}
```

## Мониторинг деплоя

### Health check после деплоя

```yaml
- name: Verify deployment
  run: |
    for i in {1..30}; do
      if curl -s http://aqstream.ru/actuator/health | grep -q "UP"; then
        echo "Deployment successful"
        exit 0
      fi
      sleep 10
    done
    echo "Deployment failed"
    exit 1
```

## Дальнейшее чтение

- [CI/CD](./ci-cd.md) — детали пайплайнов
- [Observability](./observability.md) — мониторинг
- [Runbook](./runbook.md) — операционные процедуры
