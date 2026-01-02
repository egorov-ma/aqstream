.PHONY: help \
        up run front front-build build rebuild logs down \
        d-up d-build d-rebuild d-front d-logs d-down \
        test t-unit t-int t-e2e \
        status health

# Telegram bot для локальной разработки
export TELEGRAM_BOT_TOKEN ?= 8260512040:AAHj0f3YmMKQMZ3PAKvvuL007i8uI1wdvfs
export TELEGRAM_BOT_USERNAME ?= aqstreamdev_bot

# ============================================
# ЛОКАЛЬНАЯ РАЗРАБОТКА
# ============================================

up: _infra run             ## Инфра + backend

run: _check-infra _kill    ## Backend сервисы
	@echo "Запуск: user-service, event-service, notification-service, gateway"
	@echo "Ctrl+C для остановки"
	@trap 'kill 0' EXIT; \
	./gradlew :services:user-service:user-service-service:bootRun & \
	./gradlew :services:event-service:event-service-service:bootRun & \
	./gradlew :services:notification-service:notification-service-service:bootRun & \
	./gradlew :services:gateway:bootRun & \
	wait

front:                     ## Frontend dev
	cd frontend && pnpm dev

front-build:               ## Собрать frontend
	cd frontend && pnpm build

build:                     ## Собрать jar
	./gradlew build -x test

rebuild:                   ## Clean + build
	./gradlew clean build -x test

logs:                      ## Логи инфраструктуры
	docker compose logs -f postgres-shared postgres-user redis rabbitmq minio

down:                      ## Остановить всё
	docker compose down
	@$(MAKE) _kill

# ============================================
# DOCKER
# ============================================

d-up: d-build              ## Собрать и запустить
	docker compose up -d
	@$(MAKE) status

d-build: build             ## Собрать образы
	docker compose build

d-rebuild:                 ## Пересобрать с нуля
	./gradlew clean build -x test
	docker compose build --no-cache

d-front:                   ## Frontend в Docker
	docker compose up -d frontend

d-logs:                    ## Логи всех контейнеров
	docker compose logs -f

d-down:                    ## Остановить Docker
	docker compose down

# ============================================
# ТЕСТЫ
# ============================================

test:                      ## Все тесты
	./gradlew test

t-unit:                    ## Unit
	./gradlew unit

t-int:                     ## Integration
	./gradlew integration

t-e2e:                     ## E2E
	./gradlew e2e

# ============================================
# УТИЛИТЫ
# ============================================

status:                    ## Статус контейнеров
	docker compose ps

health:                    ## Health check
	@docker compose exec -T postgres-shared pg_isready -U aqstream || echo "postgres: down"
	@docker compose exec -T redis redis-cli -a aqstream-redis-secret ping 2>/dev/null || echo "redis: down"
	@docker compose exec -T rabbitmq rabbitmq-diagnostics check_running 2>/dev/null || echo "rabbitmq: down"

help:                      ## Справка
	@echo ""
	@echo "=== СЦЕНАРИИ ==="
	@echo ""
	@echo "Локальная разработка:"
	@echo "  make up          # инфра + backend"
	@echo "  make front       # frontend (в другом терминале)"
	@echo "  make logs        # логи инфраструктуры"
	@echo "  make down        # остановить"
	@echo ""
	@echo "Docker:"
	@echo "  make d-up        # собрать и запустить"
	@echo "  make d-logs      # логи"
	@echo "  make d-down      # остановить"
	@echo ""
	@echo "=== ВСЕ КОМАНДЫ ==="
	@echo ""
	@echo "LOCAL:  up, run, front, front-build, build, rebuild, logs, down"
	@echo "DOCKER: d-up, d-build, d-rebuild, d-front, d-logs, d-down"
	@echo "TESTS:  test, t-unit, t-int, t-e2e"
	@echo "UTILS:  status, health"
	@echo ""

# ============================================
# INTERNAL
# ============================================

_infra:
	docker compose up -d postgres-shared postgres-user postgres-payment postgres-analytics redis rabbitmq minio

_check-infra:
	@docker compose ps --format '{{.Name}}' 2>/dev/null | grep -q postgres-shared || \
		(echo "Ошибка: запустите make up" && exit 1)

_kill:
	@for p in 8080 8081 8082 8084; do \
		pid=$$(lsof -ti:$$p 2>/dev/null); \
		[ -n "$$pid" ] && kill -9 $$pid 2>/dev/null || true; \
	done
