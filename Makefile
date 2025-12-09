.PHONY: build test clean help up down logs

# Цвета для вывода
CYAN := \033[36m
GREEN := \033[32m
RESET := \033[0m

help: ## Показать справку
	@echo "$(CYAN)AqStream - доступные команды:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-15s$(RESET) %s\n", $$1, $$2}'

# === Gradle ===

build: ## Собрать проект
	./gradlew build

test: ## Запустить тесты
	./gradlew test

clean: ## Очистить build артефакты
	./gradlew clean

check: ## Запустить все проверки (checkstyle, tests)
	./gradlew check

# === Docker ===

up: ## Запустить Docker Compose
	docker compose up -d

down: ## Остановить Docker Compose
	docker compose down

logs: ## Показать логи Docker Compose
	docker compose logs -f

# === Frontend ===

frontend-install: ## Установить зависимости frontend
	cd frontend && pnpm install

frontend-dev: ## Запустить frontend dev server
	cd frontend && pnpm dev

frontend-build: ## Собрать frontend
	cd frontend && pnpm build

# === Development ===

dev: up ## Запустить локальное окружение
	@echo "$(GREEN)Инфраструктура запущена!$(RESET)"
	@echo "PostgreSQL: localhost:5432"
	@echo "Redis: localhost:6379"
	@echo "RabbitMQ: localhost:5672 (UI: localhost:15672)"

stop: down ## Остановить локальное окружение
	@echo "$(GREEN)Инфраструктура остановлена$(RESET)"
