.PHONY: build test clean help build-docker up down logs infra-up infra-down infra-logs infra-ps infra-reset db-backup db-restore docs-install docs-serve docs-build docs-validate docs-openapi docs-redoc backend-gateway backend-user backend-event backend-notification backend-media

# Цвета для вывода
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RESET := \033[0m

help: ## Показать справку
	@echo "$(CYAN)AqStream - доступные команды:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(RESET) %s\n", $$1, $$2}'

# === Gradle ===

build: ## Собрать проект
	./gradlew build

test: ## Запустить тесты
	./gradlew test

clean: ## Очистить build артефакты
	./gradlew clean

check: ## Запустить все проверки (checkstyle, tests)
	./gradlew check

# === Docker Infrastructure ===

infra-up: ## Запустить инфраструктуру (PostgreSQL, Redis, RabbitMQ, MinIO)
	@echo "$(CYAN)Запуск инфраструктуры...$(RESET)"
	docker compose up -d postgres-shared postgres-user postgres-payment postgres-analytics redis rabbitmq minio minio-init
	@echo "$(GREEN)Инфраструктура запущена!$(RESET)"
	@echo ""
	@echo "$(YELLOW)Доступные сервисы:$(RESET)"
	@echo "  PostgreSQL (shared):    localhost:5432"
	@echo "  PostgreSQL (user):      localhost:5433"
	@echo "  PostgreSQL (payment):   localhost:5434"
	@echo "  PostgreSQL (analytics): localhost:5435"
	@echo "  Redis:                  localhost:6379"
	@echo "  RabbitMQ:             localhost:5672"
	@echo "  RabbitMQ UI:          http://localhost:15672 (guest/guest)"
	@echo "  MinIO API:            http://localhost:9000"
	@echo "  MinIO Console:        http://localhost:9001 (minioadmin/minioadmin)"

infra-down: ## Остановить инфраструктуру
	@echo "$(CYAN)Остановка инфраструктуры...$(RESET)"
	docker compose down
	@echo "$(GREEN)Инфраструктура остановлена$(RESET)"

infra-logs: ## Показать логи инфраструктуры
	docker compose logs -f postgres-shared postgres-user postgres-payment postgres-analytics redis rabbitmq minio

infra-ps: ## Показать статус контейнеров
	docker compose ps

infra-reset: ## Полный сброс инфраструктуры (удаление volumes)
	@echo "$(YELLOW)ВНИМАНИЕ: Это удалит все данные!$(RESET)"
	@read -p "Продолжить? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker compose down -v
	@echo "$(GREEN)Инфраструктура сброшена$(RESET)"

# === Database Backup/Restore ===

BACKUP_DIR := ./backups
BACKUP_DATE := $(shell date +%Y%m%d_%H%M%S)

db-backup: ## Создать бэкап всех баз данных
	@echo "$(CYAN)Создание бэкапа баз данных...$(RESET)"
	@mkdir -p $(BACKUP_DIR)
	@docker compose exec -T postgres-shared pg_dump -U $${POSTGRES_USER:-aqstream} shared_services_db > $(BACKUP_DIR)/shared_services_db_$(BACKUP_DATE).sql
	@docker compose exec -T postgres-user pg_dump -U $${POSTGRES_USER:-aqstream} user_service_db > $(BACKUP_DIR)/user_service_db_$(BACKUP_DATE).sql
	@docker compose exec -T postgres-payment pg_dump -U $${POSTGRES_USER:-aqstream} payment_service_db > $(BACKUP_DIR)/payment_service_db_$(BACKUP_DATE).sql
	@docker compose exec -T postgres-analytics pg_dump -U $${POSTGRES_USER:-aqstream} analytics_service_db > $(BACKUP_DIR)/analytics_service_db_$(BACKUP_DATE).sql
	@echo "$(GREEN)Бэкапы сохранены в $(BACKUP_DIR)/$(RESET)"
	@ls -la $(BACKUP_DIR)/*_$(BACKUP_DATE).sql

db-restore: ## Восстановить базы из бэкапа (требует BACKUP_DATE=YYYYMMDD_HHMMSS)
	@echo "$(YELLOW)ВНИМАНИЕ: Это перезапишет текущие данные!$(RESET)"
	@if [ -z "$(BACKUP_DATE)" ]; then echo "$(YELLOW)Укажите BACKUP_DATE, например: make db-restore BACKUP_DATE=20251223_120000$(RESET)"; exit 1; fi
	@read -p "Продолжить восстановление из $(BACKUP_DATE)? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	@echo "$(CYAN)Восстановление баз данных...$(RESET)"
	@docker compose exec -T postgres-shared psql -U $${POSTGRES_USER:-aqstream} -d shared_services_db < $(BACKUP_DIR)/shared_services_db_$(BACKUP_DATE).sql
	@docker compose exec -T postgres-user psql -U $${POSTGRES_USER:-aqstream} -d user_service_db < $(BACKUP_DIR)/user_service_db_$(BACKUP_DATE).sql
	@docker compose exec -T postgres-payment psql -U $${POSTGRES_USER:-aqstream} -d payment_service_db < $(BACKUP_DIR)/payment_service_db_$(BACKUP_DATE).sql
	@docker compose exec -T postgres-analytics psql -U $${POSTGRES_USER:-aqstream} -d analytics_service_db < $(BACKUP_DIR)/analytics_service_db_$(BACKUP_DATE).sql
	@echo "$(GREEN)Базы данных восстановлены$(RESET)"

# === Docker Full Stack ===

build-docker: build ## Собрать Docker образы (сначала Gradle build)
	docker compose build

up: ## Запустить весь стек (инфраструктура + сервисы)
	docker compose up -d

down: ## Остановить весь стек
	docker compose down

logs: ## Показать логи всех контейнеров
	docker compose logs -f

# === Frontend ===

frontend-install: ## Установить зависимости frontend
	cd frontend && pnpm install

frontend-dev: ## Запустить frontend dev server
	cd frontend && pnpm dev

frontend-build: ## Собрать frontend
	cd frontend && pnpm build

frontend-lint: ## Проверить код frontend
	cd frontend && pnpm lint

frontend-typecheck: ## Проверить типы frontend
	cd frontend && pnpm typecheck

# === Backend Services ===

backend-gateway: ## Запустить API Gateway
	./gradlew :services:gateway:bootRun

backend-user: ## Запустить User Service
	./gradlew :services:user-service:bootRun

backend-event: ## Запустить Event Service
	./gradlew :services:event-service:bootRun

backend-notification: ## Запустить Notification Service
	./gradlew :services:notification-service:bootRun

backend-media: ## Запустить Media Service
	./gradlew :services:media-service:bootRun

# === Development ===

dev: infra-up ## Запустить локальное окружение для разработки
	@echo ""
	@echo "$(GREEN)Готово к разработке!$(RESET)"
	@echo ""
	@echo "$(YELLOW)Запуск сервисов (в отдельных терминалах):$(RESET)"
	@echo "  make backend-gateway       # API Gateway (порт 8080)"
	@echo "  make backend-user          # User Service (порт 8081)"
	@echo "  make backend-event         # Event Service (порт 8082)"
	@echo "  make backend-notification  # Notification Service (порт 8084)"
	@echo "  make backend-media         # Media Service (порт 8085)"
	@echo "  make frontend-dev          # Frontend (порт 3000)"

stop: infra-down ## Остановить локальное окружение

# === Health Checks ===

health: ## Проверить health всех сервисов
	@echo "$(CYAN)Проверка PostgreSQL...$(RESET)"
	@docker compose exec -T postgres-shared pg_isready -U aqstream || echo "postgres-shared: недоступен"
	@docker compose exec -T postgres-user pg_isready -U aqstream || echo "postgres-user: недоступен"
	@docker compose exec -T postgres-payment pg_isready -U aqstream || echo "postgres-payment: недоступен"
	@docker compose exec -T postgres-analytics pg_isready -U aqstream || echo "postgres-analytics: недоступен"
	@echo "$(CYAN)Проверка Redis...$(RESET)"
	@docker compose exec -T redis redis-cli -a $${REDIS_PASSWORD:-aqstream-redis-secret} ping 2>/dev/null || echo "redis: недоступен"
	@echo "$(CYAN)Проверка RabbitMQ...$(RESET)"
	@docker compose exec -T rabbitmq rabbitmq-diagnostics check_running || echo "rabbitmq: недоступен"

# === Documentation ===

docs-install: ## Установить Python зависимости для документации
	@echo "$(CYAN)Установка зависимостей для документации...$(RESET)"
	pip3 install -r docs/_internal/doc-as-code/requirements.txt
	@echo "$(GREEN)Зависимости установлены$(RESET)"

docs-serve: ## Запустить локальный сервер документации
	@echo "$(CYAN)Запуск MkDocs на http://localhost:8000$(RESET)"
	cd docs/_internal && python3 -m mkdocs serve

docs-build: ## Собрать документацию
	@echo "$(CYAN)Сборка документации...$(RESET)"
	cd docs/_internal && python3 -m mkdocs build -d ../../site
	@echo "$(GREEN)Документация собрана в site/$(RESET)"

docs-validate: ## Валидация документации
	@echo "$(CYAN)Валидация Markdown...$(RESET)"
	./docs/_internal/validators/validate-markdown.sh
	@echo "$(CYAN)Валидация OpenAPI...$(RESET)"
	./docs/_internal/validators/validate-openapi.sh

docs-openapi: ## Скачать OpenAPI спецификации из сервисов
	./docs/_internal/generators/generate-openapi.sh

docs-redoc: ## Сгенерировать ReDoc HTML
	./docs/_internal/generators/generate-redoc.sh
