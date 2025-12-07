# Development Tooling

Инструменты разработки AqStream.

## Обязательные инструменты

| Инструмент | Версия | Назначение |
|------------|--------|------------|
| Docker | 24+ | Контейнеризация |
| Docker Compose | 2.x | Локальная оркестрация |
| JDK | 21 | Java runtime |
| Node.js | 20 LTS | Frontend runtime |
| pnpm | 8+ | Package manager |
| Git | 2.40+ | Version control |

## IDE

### IntelliJ IDEA (Backend)

Рекомендуемые плагины:
- Lombok
- MapStruct Support
- Spring Boot Assistant
- Database Tools

### VS Code (Frontend)

Рекомендуемые расширения:
- ESLint
- Prettier
- Tailwind CSS IntelliSense
- TypeScript Hero

## Makefile

```makefile
# Основные команды
.PHONY: up down restart status logs

up:
	docker compose up -d

down:
	docker compose down

restart:
	docker compose restart

status:
	docker compose ps

logs:
	docker compose logs -f

# Инфраструктура
.PHONY: infra-up infra-down

infra-up:
	docker compose up -d postgres-shared postgres-user postgres-payment redis rabbitmq minio

infra-down:
	docker compose down postgres-shared postgres-user postgres-payment redis rabbitmq minio

# Backend
.PHONY: backend-build backend-test

backend-build:
	./gradlew build -x test

backend-test:
	./gradlew test

# Frontend
.PHONY: frontend-install frontend-dev frontend-build frontend-test

frontend-install:
	cd frontend && pnpm install

frontend-dev:
	cd frontend && pnpm dev

frontend-build:
	cd frontend && pnpm build

frontend-test:
	cd frontend && pnpm test

# Quality
.PHONY: lint format

lint:
	./gradlew checkstyleMain
	cd frontend && pnpm lint

format:
	cd frontend && pnpm format

# Database
.PHONY: db-migrate db-rollback

db-migrate:
	./gradlew liquibaseUpdate

db-rollback:
	./gradlew liquibaseRollbackCount -PliquibaseCommandValue=1
```

## Code Quality

### Checkstyle (Backend)

```xml
<!-- config/checkstyle/checkstyle.xml -->
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <module name="TreeWalker">
        <module name="Indentation">
            <property name="basicOffset" value="4"/>
        </module>
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
        <module name="MethodLength">
            <property name="max" value="50"/>
        </module>
    </module>
</module>
```

### ESLint (Frontend)

```json
// .eslintrc.json
{
  "extends": [
    "next/core-web-vitals",
    "prettier"
  ],
  "rules": {
    "@typescript-eslint/no-explicit-any": "error",
    "@typescript-eslint/explicit-function-return-type": "off",
    "react/jsx-no-literals": "off"
  }
}
```

### Prettier (Frontend)

```json
// .prettierrc
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100
}
```

## Git Hooks (Husky)

```bash
# .husky/pre-commit
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# Frontend lint
cd frontend && pnpm lint-staged

# Backend checkstyle
./gradlew checkstyleMain --daemon
```

```json
// frontend/package.json
{
  "lint-staged": {
    "*.{ts,tsx}": ["eslint --fix", "prettier --write"],
    "*.{json,md}": ["prettier --write"]
  }
}
```

## Environment Variables

### .env.example

```bash
# Database
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_USERNAME=aqstream
DATABASE_PASSWORD=aqstream

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# JWT
JWT_SECRET=your-super-secret-key-change-in-production

# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Docker Compose

```yaml
# docker-compose.yml
services:
  # Databases
  postgres-shared:
    image: postgres:15
    environment:
      POSTGRES_USER: aqstream
      POSTGRES_PASSWORD: aqstream
      POSTGRES_DB: shared_services_db
    ports:
      - "5432:5432"
    volumes:
      - postgres-shared-data:/var/lib/postgresql/data

  postgres-user:
    image: postgres:15
    environment:
      POSTGRES_USER: aqstream
      POSTGRES_PASSWORD: aqstream
      POSTGRES_DB: user_service_db
    ports:
      - "5433:5432"
    volumes:
      - postgres-user-data:/var/lib/postgresql/data

  postgres-payment:
    image: postgres:15
    environment:
      POSTGRES_USER: aqstream
      POSTGRES_PASSWORD: aqstream
      POSTGRES_DB: payment_service_db
    ports:
      - "5434:5432"
    volumes:
      - postgres-payment-data:/var/lib/postgresql/data

  # Cache
  redis:
    image: redis:7
    ports:
      - "6379:6379"

  # Messaging
  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"

  # Storage
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data

volumes:
  postgres-shared-data:
  postgres-user-data:
  postgres-payment-data:
  minio-data:
```

## Полезные команды

```bash
# Проверить что всё работает
make status

# Посмотреть логи конкретного сервиса
docker compose logs -f event-service

# Зайти в контейнер
docker compose exec postgres-shared psql -U aqstream -d shared_services_db

# Перезапустить один сервис
docker compose restart event-service

# Пересобрать с нуля
docker compose build --no-cache event-service
docker compose up -d event-service

# Очистить всё
docker compose down -v
```

## Дальнейшее чтение

- [Environments](../operations/environments.md) — окружения
- [CI/CD](../operations/ci-cd.md) — пайплайны
