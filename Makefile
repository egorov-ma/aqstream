.PHONY: help build test clean check coverage \
        dev run front docker down preview up logs rebuild docker-logs \
        local-up local-down local-status local-reset local-services-stop \
        run-all run-gateway run-user run-event run-notification run-payment run-media run-analytics run-frontend \
        docker-up docker-up-full docker-down docker-build docker-rebuild docker-status \
        test-unit test-integration test-e2e test-frontend test-frontend-watch test-frontend-e2e \
        log-all log-infra log-backend log-gateway log-user log-event log-notification log-payment log-media log-analytics log-frontend \
        db-backup db-restore db-shell-shared db-shell-user db-shell-payment db-shell-analytics \
        frontend-install frontend-build frontend-lint frontend-typecheck frontend-format \
        docs-install docs-serve docs-build docs-validate docs-openapi docs-redoc \
        health

# Цвета для вывода
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
RESET := \033[0m
BOLD := \033[1m

# Переменные
BACKUP_DIR := ./backups
BACKUP_DATE := $(shell date +%Y%m%d_%H%M%S)

# Контейнеры инфраструктуры
INFRA_CONTAINERS := postgres-shared postgres-user postgres-payment postgres-analytics redis rabbitmq minio minio-init

# Backend сервисы
BACKEND_SERVICES := gateway user-service event-service notification-service

# ============================================
# HELP
# ============================================

help: ## Показать справку
	@echo ""
	@echo "$(BOLD)$(CYAN)AqStream Makefile$(RESET)"
	@echo ""
	@echo "$(BOLD)$(GREEN)Quick Start (простые команды):$(RESET)"
	@echo "  $(CYAN)make dev$(RESET)             Локальная разработка (инфраструктура)"
	@echo "  $(CYAN)make run$(RESET)             Запустить backend (все сервисы)"
	@echo "  $(CYAN)make front$(RESET)           Запустить frontend"
	@echo "  $(CYAN)make docker$(RESET)          Полный стек в Docker"
	@echo "  $(CYAN)make down$(RESET)            Остановить всё"
	@echo "  $(CYAN)make preview$(RESET)         Проверка перед коммитом (тесты + запуск)"
	@echo ""
	@echo "$(BOLD)$(GREEN)Детальные команды:$(RESET)"
	@echo ""
	@echo "$(BOLD)$(GREEN)Local Development:$(RESET)"
	@echo "  $(CYAN)local-up$(RESET)             Запустить инфраструктуру (PostgreSQL, Redis, RabbitMQ, MinIO)"
	@echo "  $(CYAN)local-down$(RESET)           Остановить инфраструктуру"
	@echo "  $(CYAN)local-status$(RESET)         Статус контейнеров инфраструктуры"
	@echo "  $(CYAN)local-reset$(RESET)          Сбросить инфраструктуру (удалить volumes)"
	@echo "  $(CYAN)local-services-stop$(RESET)  Остановить Docker сервисы (оставить инфраструктуру)"
	@echo ""
	@echo "$(BOLD)$(GREEN)Run Services (local):$(RESET)"
	@echo "  $(CYAN)run-all$(RESET)              Запустить все backend сервисы (в правильном порядке)"
	@echo "  $(CYAN)run-gateway$(RESET)          API Gateway (порт 8080)"
	@echo "  $(CYAN)run-user$(RESET)             User Service (порт 8081)"
	@echo "  $(CYAN)run-event$(RESET)            Event Service (порт 8082)"
	@echo "  $(CYAN)run-notification$(RESET)     Notification Service (порт 8084)"
	@echo "  $(CYAN)run-payment$(RESET)          Payment Service (порт 8083)"
	@echo "  $(CYAN)run-media$(RESET)            Media Service (порт 8085)"
	@echo "  $(CYAN)run-analytics$(RESET)        Analytics Service (порт 8086)"
	@echo "  $(CYAN)run-frontend$(RESET)         Frontend (порт 3000)"
	@echo ""
	@echo "$(BOLD)$(GREEN)Docker Stack:$(RESET)"
	@echo "  $(CYAN)docker-up$(RESET)            Запустить основной стек"
	@echo "  $(CYAN)docker-up-full$(RESET)       Запустить полный стек (все сервисы)"
	@echo "  $(CYAN)docker-down$(RESET)          Остановить стек"
	@echo "  $(CYAN)docker-build$(RESET)         Собрать Docker образы"
	@echo "  $(CYAN)docker-rebuild$(RESET)       Пересобрать образы (no-cache)"
	@echo "  $(CYAN)docker-status$(RESET)        Статус контейнеров"
	@echo ""
	@echo "$(BOLD)$(GREEN)Tests:$(RESET)"
	@echo "  $(CYAN)test$(RESET)                 Все тесты (unit + integration + e2e)"
	@echo "  $(CYAN)test-unit$(RESET)            Только unit тесты"
	@echo "  $(CYAN)test-integration$(RESET)     Только integration тесты"
	@echo "  $(CYAN)test-e2e$(RESET)             Только e2e тесты (backend)"
	@echo "  $(CYAN)test-frontend$(RESET)        Frontend unit тесты"
	@echo "  $(CYAN)test-frontend-watch$(RESET)  Frontend тесты в watch режиме"
	@echo "  $(CYAN)test-frontend-e2e$(RESET)    Frontend e2e тесты (Playwright)"
	@echo "  $(CYAN)coverage$(RESET)             Jacoco coverage report"
	@echo ""
	@echo "$(BOLD)$(GREEN)Logs:$(RESET)"
	@echo "  $(CYAN)log-all$(RESET)              Все логи"
	@echo "  $(CYAN)log-infra$(RESET)            Логи инфраструктуры"
	@echo "  $(CYAN)log-backend$(RESET)          Логи backend сервисов"
	@echo "  $(CYAN)log-gateway$(RESET)          Логи Gateway"
	@echo "  $(CYAN)log-user$(RESET)             Логи User Service"
	@echo "  $(CYAN)log-event$(RESET)            Логи Event Service"
	@echo "  $(CYAN)log-notification$(RESET)     Логи Notification Service"
	@echo "  $(CYAN)log-frontend$(RESET)         Логи Frontend"
	@echo ""
	@echo "$(BOLD)$(GREEN)Build:$(RESET)"
	@echo "  $(CYAN)build$(RESET)                Собрать проект (Gradle)"
	@echo "  $(CYAN)clean$(RESET)                Очистить build артефакты"
	@echo "  $(CYAN)check$(RESET)                Checkstyle + тесты"
	@echo ""
	@echo "$(BOLD)$(GREEN)Frontend:$(RESET)"
	@echo "  $(CYAN)frontend-install$(RESET)     Установить зависимости (pnpm install)"
	@echo "  $(CYAN)frontend-build$(RESET)       Собрать frontend"
	@echo "  $(CYAN)frontend-lint$(RESET)        ESLint"
	@echo "  $(CYAN)frontend-typecheck$(RESET)   TypeScript проверка"
	@echo "  $(CYAN)frontend-format$(RESET)      Prettier format"
	@echo ""
	@echo "$(BOLD)$(GREEN)Database:$(RESET)"
	@echo "  $(CYAN)db-backup$(RESET)            Бэкап всех баз данных"
	@echo "  $(CYAN)db-restore$(RESET)           Восстановление из бэкапа"
	@echo "  $(CYAN)db-shell-shared$(RESET)      psql в shared_services_db"
	@echo "  $(CYAN)db-shell-user$(RESET)        psql в user_service_db"
	@echo "  $(CYAN)db-shell-payment$(RESET)     psql в payment_service_db"
	@echo "  $(CYAN)db-shell-analytics$(RESET)   psql в analytics_service_db"
	@echo ""
	@echo "$(BOLD)$(GREEN)Documentation:$(RESET)"
	@echo "  $(CYAN)docs-install$(RESET)         Установить Python зависимости"
	@echo "  $(CYAN)docs-serve$(RESET)           Запустить MkDocs сервер"
	@echo "  $(CYAN)docs-build$(RESET)           Собрать документацию"
	@echo "  $(CYAN)docs-validate$(RESET)        Валидация документации"
	@echo "  $(CYAN)docs-openapi$(RESET)         Скачать OpenAPI specs"
	@echo "  $(CYAN)docs-redoc$(RESET)           Сгенерировать ReDoc"
	@echo ""
	@echo "$(BOLD)$(GREEN)Health:$(RESET)"
	@echo "  $(CYAN)health$(RESET)               Проверить health всех сервисов"
	@echo ""

# ============================================
# QUICK START (простые alias команды)
# ============================================

dev: local-up ## Локальная разработка (инфраструктура)
	@echo ""
	@echo "$(GREEN)✓ Инфраструктура запущена!$(RESET)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Следующий шаг:$(RESET)"
	@echo "  $(CYAN)make run$(RESET)             # Запустить backend (все сервисы)"
	@echo "  $(CYAN)make front$(RESET)           # Запустить frontend"
	@echo ""
	@echo "$(YELLOW)Или по отдельности:$(RESET)"
	@echo "  make run-gateway       # API Gateway (порт 8080)"
	@echo "  make run-user          # User Service (порт 8081)"
	@echo "  make run-event         # Event Service (порт 8082)"
	@echo "  make run-notification  # Notification Service (порт 8084)"
	@echo ""
	@echo "$(BOLD)Справка:$(RESET) $(CYAN)make help$(RESET)"

run: run-all ## Запустить backend (все сервисы) - alias для run-all

front: run-frontend ## Запустить frontend - alias для run-frontend

docker: docker-up ## Запустить полный стек в Docker - alias для docker-up

down: local-down ## Остановить всё - alias для local-down

preview: ## Проверка перед коммитом (unit тесты + запуск)
	@echo "$(CYAN)→ Запуск unit тестов (backend)...$(RESET)"
	@$(MAKE) test-unit
	@echo ""
	@echo "$(CYAN)→ Запуск unit тестов (frontend)...$(RESET)"
	@cd frontend && pnpm test
	@echo ""
	@echo "$(GREEN)✓ Все unit тесты прошли!$(RESET)"
	@echo ""
	@echo "$(YELLOW)Для ручной проверки запустите:$(RESET)"
	@echo "  make dev    # Инфраструктура"
	@echo "  make run    # Backend"
	@echo "  make front  # Frontend"

up: dev ## Инфра + backend - alias для dev

logs: log-infra ## Логи инфраструктуры - alias для log-infra

docker-logs: log-all ## Логи всех контейнеров - alias для log-all

# ============================================
# LOCAL DEVELOPMENT
# ============================================

local-up: ## Запустить инфраструктуру (PostgreSQL, Redis, RabbitMQ, MinIO)
	@echo "$(CYAN)Запуск инфраструктуры...$(RESET)"
	docker compose up -d $(INFRA_CONTAINERS)
	@echo "$(GREEN)Инфраструктура запущена!$(RESET)"
	@echo ""
	@echo "$(YELLOW)Доступные сервисы:$(RESET)"
	@echo "  PostgreSQL (shared):    localhost:5432"
	@echo "  PostgreSQL (user):      localhost:5433"
	@echo "  PostgreSQL (payment):   localhost:5434"
	@echo "  PostgreSQL (analytics): localhost:5435"
	@echo "  Redis:                  localhost:6379"
	@echo "  RabbitMQ:               localhost:5672"
	@echo "  RabbitMQ UI:            http://localhost:15672 (guest/guest)"
	@echo "  MinIO API:              http://localhost:9000"
	@echo "  MinIO Console:          http://localhost:9001 (minioadmin/minioadmin)"

local-down: ## Остановить инфраструктуру
	@echo "$(CYAN)Остановка инфраструктуры...$(RESET)"
	docker compose down
	@echo "$(GREEN)Инфраструктура остановлена$(RESET)"

local-status: ## Статус контейнеров инфраструктуры
	docker compose ps

local-reset: ## Сбросить инфраструктуру (удалить volumes)
	@echo "$(YELLOW)ВНИМАНИЕ: Это удалит все данные!$(RESET)"
	@read -p "Продолжить? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker compose down -v
	@echo "$(GREEN)Инфраструктура сброшена$(RESET)"

local-services-stop: ## Остановить Docker сервисы (оставить инфраструктуру)
	@echo "$(CYAN)Остановка Docker сервисов...$(RESET)"
	docker compose stop gateway user-service event-service notification-service payment-service media-service analytics-service frontend 2>/dev/null || true
	@echo "$(GREEN)Сервисы остановлены. Инфраструктура работает.$(RESET)"

# ============================================
# RUN SERVICES (LOCAL)
# ============================================

run-all: ## Запустить все backend сервисы (в правильном порядке)
	@echo "$(CYAN)Запуск всех backend сервисов...$(RESET)"
	@echo "$(YELLOW)Порядок: user-service -> event-service -> notification-service -> gateway$(RESET)"
	@echo "$(YELLOW)Нажмите Ctrl+C для остановки всех сервисов$(RESET)"
	@echo ""
	@trap 'echo ""; echo "$(RED)Остановка сервисов...$(RESET)"; kill 0' EXIT; \
	./gradlew :services:user-service:user-service-service:bootRun & \
	sleep 15 && ./gradlew :services:event-service:event-service-service:bootRun & \
	sleep 25 && ./gradlew :services:notification-service:notification-service-service:bootRun & \
	sleep 35 && ./gradlew :services:gateway:bootRun & \
	wait

run-gateway: ## Запустить API Gateway (порт 8080)
	./gradlew :services:gateway:bootRun

run-user: ## Запустить User Service (порт 8081)
	./gradlew :services:user-service:user-service-service:bootRun

run-event: ## Запустить Event Service (порт 8082)
	./gradlew :services:event-service:event-service-service:bootRun

run-notification: ## Запустить Notification Service (порт 8084)
	./gradlew :services:notification-service:notification-service-service:bootRun

run-payment: ## Запустить Payment Service (порт 8083)
	./gradlew :services:payment-service:payment-service-service:bootRun

run-media: ## Запустить Media Service (порт 8085)
	./gradlew :services:media-service:media-service-service:bootRun

run-analytics: ## Запустить Analytics Service (порт 8086)
	./gradlew :services:analytics-service:analytics-service-service:bootRun

run-frontend: ## Запустить Frontend (порт 3000)
	cd frontend && pnpm dev

# ============================================
# DOCKER STACK
# ============================================

docker-up: ## Запустить основной стек в Docker
	@echo "$(CYAN)Запуск основного стека...$(RESET)"
	docker compose up -d
	@echo "$(GREEN)Стек запущен!$(RESET)"

docker-up-full: ## Запустить полный стек (все сервисы включая stub)
	@echo "$(CYAN)Запуск полного стека (с profile=full)...$(RESET)"
	docker compose --profile full up -d
	@echo "$(GREEN)Полный стек запущен!$(RESET)"

docker-down: ## Остановить Docker стек
	@echo "$(CYAN)Остановка стека...$(RESET)"
	docker compose down
	@echo "$(GREEN)Стек остановлен$(RESET)"

docker-build: build ## Собрать Docker образы (сначала Gradle build)
	@echo "$(CYAN)Сборка Docker образов...$(RESET)"
	docker compose build
	@echo "$(GREEN)Образы собраны$(RESET)"

docker-rebuild: build ## Пересобрать Docker образы (no-cache)
	@echo "$(CYAN)Пересборка Docker образов (no-cache)...$(RESET)"
	docker compose build --no-cache
	@echo "$(GREEN)Образы пересобраны$(RESET)"

docker-status: ## Статус Docker контейнеров
	docker compose ps

# ============================================
# TESTS
# ============================================

test: ## Запустить все тесты
	./gradlew test

test-unit: ## Запустить только unit тесты
	./gradlew unit

test-integration: ## Запустить только integration тесты
	./gradlew integration

test-e2e: ## Запустить только e2e тесты (backend)
	./gradlew e2e

test-frontend: ## Запустить frontend unit тесты
	cd frontend && pnpm test

test-frontend-watch: ## Запустить frontend тесты в watch режиме
	cd frontend && pnpm test:watch

test-frontend-e2e: ## Запустить frontend e2e тесты (Playwright)
	cd frontend && pnpm test:e2e

coverage: ## Jacoco coverage report
	./gradlew jacocoTestReport
	@echo "$(GREEN)Coverage report: build/reports/jacoco/test/html/index.html$(RESET)"

# ============================================
# LOGS
# ============================================

log-all: ## Показать все логи
	docker compose logs -f

log-infra: ## Показать логи инфраструктуры
	docker compose logs -f $(INFRA_CONTAINERS)

log-backend: ## Показать логи backend сервисов
	docker compose logs -f $(BACKEND_SERVICES)

log-gateway: ## Показать логи Gateway
	docker compose logs -f gateway

log-user: ## Показать логи User Service
	docker compose logs -f user-service

log-event: ## Показать логи Event Service
	docker compose logs -f event-service

log-notification: ## Показать логи Notification Service
	docker compose logs -f notification-service

log-payment: ## Показать логи Payment Service
	docker compose logs -f payment-service

log-media: ## Показать логи Media Service
	docker compose logs -f media-service

log-analytics: ## Показать логи Analytics Service
	docker compose logs -f analytics-service

log-frontend: ## Показать логи Frontend
	docker compose logs -f frontend

# ============================================
# BUILD
# ============================================

build: ## Собрать проект (Gradle)
	./gradlew build

rebuild: ## Clean + build - полная пересборка
	@echo "$(CYAN)→ Очистка...$(RESET)"
	@./gradlew clean
	@echo "$(CYAN)→ Сборка...$(RESET)"
	@./gradlew build
	@echo "$(GREEN)✓ Пересборка завершена$(RESET)"

clean: ## Полная очистка (volumes + build артефакты)
	@echo "$(CYAN)→ Остановка контейнеров и удаление volumes...$(RESET)"
	@docker compose down -v
	@echo "$(CYAN)→ Очистка Gradle build...$(RESET)"
	@./gradlew clean
	@echo "$(GREEN)✓ Полная очистка завершена$(RESET)"

check: ## Полная проверка (все тесты + Docker сборка)
	@echo "$(CYAN)→ Запуск всех backend тестов...$(RESET)"
	@$(MAKE) test
	@echo ""
	@echo "$(CYAN)→ Запуск frontend тестов...$(RESET)"
	@cd frontend && pnpm test
	@echo ""
	@echo "$(CYAN)→ Сборка Docker образов...$(RESET)"
	@$(MAKE) docker-build
	@echo ""
	@echo "$(GREEN)✓ Полная проверка завершена успешно!$(RESET)"

# ============================================
# FRONTEND
# ============================================

frontend-install: ## Установить зависимости frontend
	cd frontend && pnpm install

frontend-build: ## Собрать frontend
	cd frontend && pnpm build

frontend-lint: ## Проверить код frontend (ESLint)
	cd frontend && pnpm lint

frontend-typecheck: ## Проверить типы frontend (TypeScript)
	cd frontend && pnpm typecheck

frontend-format: ## Форматировать код frontend (Prettier)
	cd frontend && pnpm format

# ============================================
# DATABASE
# ============================================

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

db-shell-shared: ## Открыть psql в shared_services_db
	docker compose exec postgres-shared psql -U $${POSTGRES_USER:-aqstream} -d shared_services_db

db-shell-user: ## Открыть psql в user_service_db
	docker compose exec postgres-user psql -U $${POSTGRES_USER:-aqstream} -d user_service_db

db-shell-payment: ## Открыть psql в payment_service_db
	docker compose exec postgres-payment psql -U $${POSTGRES_USER:-aqstream} -d payment_service_db

db-shell-analytics: ## Открыть psql в analytics_service_db
	docker compose exec postgres-analytics psql -U $${POSTGRES_USER:-aqstream} -d analytics_service_db

# ============================================
# DOCUMENTATION
# ============================================

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

# ============================================
# HEALTH
# ============================================

health: ## Проверить health всех сервисов
	@echo "$(CYAN)Проверка PostgreSQL...$(RESET)"
	@docker compose exec -T postgres-shared pg_isready -U aqstream || echo "$(RED)postgres-shared: недоступен$(RESET)"
	@docker compose exec -T postgres-user pg_isready -U aqstream || echo "$(RED)postgres-user: недоступен$(RESET)"
	@docker compose exec -T postgres-payment pg_isready -U aqstream || echo "$(RED)postgres-payment: недоступен$(RESET)"
	@docker compose exec -T postgres-analytics pg_isready -U aqstream || echo "$(RED)postgres-analytics: недоступен$(RESET)"
	@echo "$(CYAN)Проверка Redis...$(RESET)"
	@docker compose exec -T redis redis-cli -a $${REDIS_PASSWORD:-aqstream-redis-secret} ping 2>/dev/null || echo "$(RED)redis: недоступен$(RESET)"
	@echo "$(CYAN)Проверка RabbitMQ...$(RESET)"
	@docker compose exec -T rabbitmq rabbitmq-diagnostics check_running || echo "$(RED)rabbitmq: недоступен$(RESET)"
