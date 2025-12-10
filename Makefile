.PHONY: build test clean help up down logs infra-up infra-down infra-logs infra-ps infra-reset docs-serve docs-build docs-validate docs-openapi docs-redoc

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
	docker compose up -d postgres-shared postgres-user postgres-payment redis rabbitmq minio minio-init
	@echo "$(GREEN)Инфраструктура запущена!$(RESET)"
	@echo ""
	@echo "$(YELLOW)Доступные сервисы:$(RESET)"
	@echo "  PostgreSQL (shared):  localhost:5432"
	@echo "  PostgreSQL (user):    localhost:5433"
	@echo "  PostgreSQL (payment): localhost:5434"
	@echo "  Redis:                localhost:6379"
	@echo "  RabbitMQ:             localhost:5672"
	@echo "  RabbitMQ UI:          http://localhost:15672 (guest/guest)"
	@echo "  MinIO API:            http://localhost:9000"
	@echo "  MinIO Console:        http://localhost:9001 (minioadmin/minioadmin)"

infra-down: ## Остановить инфраструктуру
	@echo "$(CYAN)Остановка инфраструктуры...$(RESET)"
	docker compose down
	@echo "$(GREEN)Инфраструктура остановлена$(RESET)"

infra-logs: ## Показать логи инфраструктуры
	docker compose logs -f postgres-shared postgres-user postgres-payment redis rabbitmq minio

infra-ps: ## Показать статус контейнеров
	docker compose ps

infra-reset: ## Полный сброс инфраструктуры (удаление volumes)
	@echo "$(YELLOW)ВНИМАНИЕ: Это удалит все данные!$(RESET)"
	@read -p "Продолжить? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker compose down -v
	@echo "$(GREEN)Инфраструктура сброшена$(RESET)"

# === Docker Full Stack ===

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

# === Development ===

dev: infra-up ## Запустить локальное окружение для разработки
	@echo ""
	@echo "$(GREEN)Готово к разработке!$(RESET)"
	@echo "Запустите сервисы через IDE или ./gradlew :services:<service>:bootRun"

stop: infra-down ## Остановить локальное окружение

# === Health Checks ===

health: ## Проверить health всех сервисов
	@echo "$(CYAN)Проверка PostgreSQL...$(RESET)"
	@docker compose exec -T postgres-shared pg_isready -U aqstream || echo "postgres-shared: недоступен"
	@docker compose exec -T postgres-user pg_isready -U aqstream || echo "postgres-user: недоступен"
	@docker compose exec -T postgres-payment pg_isready -U aqstream || echo "postgres-payment: недоступен"
	@echo "$(CYAN)Проверка Redis...$(RESET)"
	@docker compose exec -T redis redis-cli ping || echo "redis: недоступен"
	@echo "$(CYAN)Проверка RabbitMQ...$(RESET)"
	@docker compose exec -T rabbitmq rabbitmq-diagnostics check_running || echo "rabbitmq: недоступен"

# === Documentation ===

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
