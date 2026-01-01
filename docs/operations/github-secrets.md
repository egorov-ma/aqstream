# GitHub Secrets & Variables

Инструкция по настройке секретов и переменных для CI/CD pipeline.

## Обзор

Конфигурация хранится в GitHub и передаётся на сервер при деплое.

**Путь:** `Settings → Secrets and variables → Actions`

### Secrets vs Variables

| Тип | Видимость | Использовать для |
|-----|-----------|------------------|
| **Secrets** | Скрыты (показываются как `***`) | Пароли, токены, ключи |
| **Variables** | Видны в логах | Имена пользователей, хосты, URL |

**Пример:** `RABBITMQ_PASSWORD` — Secret (пароль), `RABBITMQ_USER` — Variable (имя пользователя)

---

## Нужен ли .env файл?

| Окружение | .env нужен? | Откуда значения? |
|-----------|-------------|------------------|
| **Локально (gradlew bootRun)** | ❌ Нет | `application-local.yml` |
| **Локально (docker compose)** | ❌ Нет | Дефолты в `docker-compose.yml` |
| **CI/CD тесты** | ❌ Нет | Testcontainers (авто) |
| **Production** | ✅ Да | Создаётся автоматически из GitHub Secrets |

**Вывод:** Локально `.env` не нужен. На production он создаётся автоматически при каждом деплое.

---

## Быстрый старт (5 минут)

Если у вас уже настроен сервер и получены все credentials:

```bash
# 1. Установите GitHub CLI
brew install gh

# 2. Авторизуйтесь
gh auth login

# 3. Добавьте секреты (замените значения на реальные)
gh secret set SSH_HOST --body "194.58.xxx.xxx"
gh secret set SSH_USER --body "deploy"
gh secret set SSH_KEY < ~/.ssh/github_deploy

gh secret set JWT_SECRET --body "$(openssl rand -base64 64 | tr -d '\n')"
gh secret set TELEGRAM_BOT_TOKEN --body "123456789:ABC..."

gh secret set DATABASE_PASSWORD --body "ваш_пароль_postgres"
gh secret set RABBITMQ_PASSWORD --body "ваш_пароль_rabbitmq"
gh secret set REDIS_PASSWORD --body "ваш_пароль_redis"

gh secret set MINIO_ACCESS_KEY --body "ваш_minio_user"
gh secret set MINIO_SECRET_KEY --body "ваш_minio_password"

gh secret set MAIL_HOST --body "smtp.gmail.com"
gh secret set MAIL_PORT --body "587"
gh secret set MAIL_USERNAME --body "your@gmail.com"
gh secret set MAIL_PASSWORD --body "xxxx xxxx xxxx xxxx"

gh secret set FRONTEND_URL --body "https://aqstream.ru"
gh secret set CORS_ALLOWED_ORIGINS --body "https://aqstream.ru"

# 4. Проверьте что всё добавлено
gh secret list
```

---

## Как получить существующие значения

Если секреты уже были настроены ранее:

### С сервера (если есть .env)

```bash
ssh deploy@your-server.ru "cat ~/aqstream/.env"
```

### Из контейнеров (если запущены)

```bash
# PostgreSQL пароль
ssh deploy@your-server.ru "docker exec aqstream-postgres-shared printenv POSTGRES_PASSWORD"

# RabbitMQ пароль
ssh deploy@your-server.ru "docker exec aqstream-rabbitmq printenv RABBITMQ_DEFAULT_PASS"

# MinIO credentials
ssh deploy@your-server.ru "docker exec aqstream-minio printenv MINIO_ROOT_USER"
ssh deploy@your-server.ru "docker exec aqstream-minio printenv MINIO_ROOT_PASSWORD"
```

### Telegram Bot Token

Если забыли токен — получите новый от [@BotFather](https://t.me/BotFather):
```
/mybots → @YourBot → API Token
```

### JWT Secret

Если забыли — сгенерируйте новый (все пользователи будут разлогинены):
```bash
openssl rand -base64 64 | tr -d '\n'
```

---

## Как секреты работают в разных окружениях

### Локальная разработка (без Docker)

При запуске сервисов через `./gradlew bootRun` используются значения из `application-local.yml`:

```yaml
# services/user-service/user-service-service/src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_service_db
    username: aqstream
    password: aqstream  # Простой пароль для локальной разработки
```

Инфраструктура (PostgreSQL, Redis, RabbitMQ) запускается через `make local-up` с паролями по умолчанию.

### Docker Desktop (локально)

При запуске через `docker compose up` используются значения по умолчанию из `docker-compose.yml`:

```yaml
# docker-compose.yml
services:
  postgres-shared:
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-aqstream}  # По умолчанию "aqstream"
```

Синтаксис `${VAR:-default}` означает: использовать переменную `VAR`, или `default` если не задана.

**Не нужно** создавать `.env` файл локально — всё работает с дефолтами.

### CI/CD (GitHub Actions)

При сборке и тестах в GitHub Actions:

1. **Unit тесты** — не требуют реальных сервисов, моки
2. **Integration тесты** — используют Testcontainers (поднимают PostgreSQL, Redis в Docker)
3. **Testcontainers** — создают изолированные контейнеры с автоматическими credentials

Секреты GitHub не используются на этапе тестирования.

### Production (деплой)

При деплое на production сервер:

1. GitHub Actions получает секреты из `Settings → Secrets → Actions`
2. Подключается к серверу по SSH (используя `SSH_HOST`, `SSH_USER`, `SSH_KEY`)
3. Генерирует `.env` файл на сервере с реальными значениями
4. Docker Compose читает `.env` и передаёт переменные в контейнеры

```mermaid
flowchart LR
    GH[GitHub Secrets] --> SSH[SSH to Server]
    SSH --> ENV[".env file"]
    ENV --> DC[Docker Compose]
    DC --> S[Services]
```

**Важно:** `.env` файл создаётся автоматически при каждом деплое и никогда не хранится в git.

---

## Список Secrets (скрытые)

| Secret | Обязательный | Описание |
|--------|--------------|----------|
| `SSH_KEY` | Да | Приватный SSH ключ |
| `JWT_SECRET` | Да | Секрет для подписи JWT токенов |
| `TELEGRAM_BOT_TOKEN` | Да | Токен Telegram бота |
| `DATABASE_PASSWORD` | Да | Пароль PostgreSQL |
| `RABBITMQ_PASSWORD` | Да | Пароль RabbitMQ |
| `REDIS_PASSWORD` | Да | Пароль Redis |
| `MAIL_PASSWORD` | Нет | SMTP пароль |
| `MINIO_SECRET_KEY` | Да | MinIO secret key |

## Список Variables (видимые)

| Variable | Обязательный | Описание |
|----------|--------------|----------|
| `SSH_HOST` | Да | Адрес production сервера |
| `SSH_USER` | Да | SSH пользователь |
| `RABBITMQ_USER` | Да | Имя пользователя RabbitMQ (default: `aqstream`) |
| `MAIL_HOST` | Нет | SMTP сервер |
| `MAIL_PORT` | Нет | SMTP порт |
| `MAIL_USERNAME` | Нет | SMTP логин |
| `MINIO_ACCESS_KEY` | Да | MinIO access key |
| `FRONTEND_URL` | Да | URL фронтенда |
| `CORS_ALLOWED_ORIGINS` | Да | Разрешённые CORS origins |
| `TELEGRAM_BOT_USERNAME` | Нет | Username Telegram бота |
| `TELEGRAM_WEBHOOK_URL` | Нет | URL вебхука Telegram |

---

## Инструкции по получению

### SSH_HOST, SSH_USER, SSH_KEY

**SSH_HOST** — IP-адрес или домен сервера:

```
194.58.xxx.xxx
```

или

```
server.aqstream.ru
```

**SSH_USER** — имя пользователя на сервере:

```
deploy
```

**SSH_KEY** — приватный ключ для подключения:

```bash
# Генерация нового ключа
ssh-keygen -t ed25519 -C "github-actions@aqstream.ru" -f ~/.ssh/github_deploy -N ""

# Добавление публичного ключа на сервер
ssh-copy-id -i ~/.ssh/github_deploy.pub deploy@server.aqstream.ru

# Получение приватного ключа для GitHub
cat ~/.ssh/github_deploy
```

Скопируйте весь вывод включая `-----BEGIN OPENSSH PRIVATE KEY-----` и `-----END OPENSSH PRIVATE KEY-----`.

---

### JWT_SECRET

Секретный ключ для подписи JWT токенов. Минимум 32 символа, рекомендуется 64.

```bash
# Генерация
openssl rand -base64 64 | tr -d '\n'

# Пример вывода:
# kX7vH3mF9pQ2wR5tY8uI1oP4aS6dF9gH2jK5lZ8xC1vB4nM7qW0eR3tY6uI9oP2a
```

---

### TELEGRAM_BOT_TOKEN

Токен получается от @BotFather в Telegram.

1. Откройте [@BotFather](https://t.me/BotFather) в Telegram
2. Отправьте `/newbot`
3. Введите имя бота: `AqStream`
4. Введите username: `AqStreamBot` (должен заканчиваться на `bot` или `Bot`)
5. Скопируйте токен из ответа

```
123456789:ABCdefGHIjklMNOpqrsTUVwxyz-1234567
```

**Дополнительные настройки:**

```
/setcommands
@AqStreamBot
start - Начать работу
help - Помощь
```

---

### DATABASE_PASSWORD

Пароль для PostgreSQL. Используется всеми сервисами.

```bash
# Генерация
openssl rand -base64 32 | tr -d '\n'

# Пример:
# Hj7kL9mN2pQ4rS6tU8vW0xY1zA3bC5dE
```

**Важно:** После изменения пароля нужно обновить его в PostgreSQL:

```sql
ALTER USER aqstream WITH PASSWORD 'новый_пароль';
```

---

### RABBITMQ_PASSWORD

Пароль для RabbitMQ.

```bash
# Генерация
openssl rand -base64 32 | tr -d '\n'
```

**Важно:** Пароль задаётся в `docker-compose.yml` через переменную окружения.

---

### REDIS_PASSWORD

Пароль для Redis.

```bash
# Генерация
openssl rand -base64 32 | tr -d '\n'
```

---

### MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD

Настройки SMTP для отправки email (верификация, сброс пароля).

#### Gmail

```
MAIL_HOST: smtp.gmail.com
MAIL_PORT: 587
MAIL_USERNAME: your-email@gmail.com
MAIL_PASSWORD: <app-password>
```

**Получение App Password для Gmail:**

1. Перейдите в [Google Account Security](https://myaccount.google.com/security)
2. Включите 2-Step Verification
3. Перейдите в App Passwords
4. Создайте пароль для "Mail" + "Other (Custom name)"
5. Скопируйте 16-символьный пароль

#### Yandex

```
MAIL_HOST: smtp.yandex.ru
MAIL_PORT: 587
MAIL_USERNAME: your-email@yandex.ru
MAIL_PASSWORD: <пароль-приложения>
```

#### SendGrid

```
MAIL_HOST: smtp.sendgrid.net
MAIL_PORT: 587
MAIL_USERNAME: apikey
MAIL_PASSWORD: <sendgrid-api-key>
```

#### Mailgun

```
MAIL_HOST: smtp.mailgun.org
MAIL_PORT: 587
MAIL_USERNAME: postmaster@your-domain.mailgun.org
MAIL_PASSWORD: <mailgun-password>
```

---

### MINIO_ACCESS_KEY, MINIO_SECRET_KEY

Ключи для MinIO (S3-совместимое хранилище).

```bash
# Access Key (16 символов)
openssl rand -base64 16 | tr -d '\n/+=' | head -c 16

# Secret Key (32 символа)
openssl rand -base64 32 | tr -d '\n'
```

---

### FRONTEND_URL

URL фронтенда для формирования ссылок в email и уведомлениях.

```
https://aqstream.ru
```

---

### CORS_ALLOWED_ORIGINS

Список доменов, с которых разрешены запросы к API.

```
https://aqstream.ru,https://www.aqstream.ru
```

Для разработки можно добавить:

```
https://aqstream.ru,https://www.aqstream.ru,http://localhost:3000
```

---

## Добавление секретов в GitHub

### Через веб-интерфейс

1. Откройте репозиторий на GitHub
2. `Settings` → `Secrets and variables` → `Actions`
3. Нажмите `New repository secret`
4. Введите имя секрета (например `JWT_SECRET`)
5. Вставьте значение
6. Нажмите `Add secret`

### Через GitHub CLI

```bash
# Установка gh cli
brew install gh

# Авторизация
gh auth login

# Добавление секрета
gh secret set JWT_SECRET --body "your-secret-value"

# Добавление из файла
gh secret set SSH_KEY < ~/.ssh/github_deploy

# Просмотр списка секретов
gh secret list
```

### Скрипт для массового добавления

```bash
#!/bin/bash
# setup-secrets.sh

# Проверяем авторизацию
gh auth status || { echo "Авторизуйтесь: gh auth login"; exit 1; }

echo "Добавление секретов..."

# SSH
read -p "SSH_HOST: " SSH_HOST
gh secret set SSH_HOST --body "$SSH_HOST"

read -p "SSH_USER: " SSH_USER
gh secret set SSH_USER --body "$SSH_USER"

read -p "Путь к SSH ключу [~/.ssh/github_deploy]: " SSH_KEY_PATH
SSH_KEY_PATH=${SSH_KEY_PATH:-~/.ssh/github_deploy}
gh secret set SSH_KEY < "$SSH_KEY_PATH"

# JWT
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
gh secret set JWT_SECRET --body "$JWT_SECRET"
echo "JWT_SECRET сгенерирован"

# Telegram
read -p "TELEGRAM_BOT_TOKEN: " TELEGRAM_BOT_TOKEN
gh secret set TELEGRAM_BOT_TOKEN --body "$TELEGRAM_BOT_TOKEN"

# Passwords
DATABASE_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
gh secret set DATABASE_PASSWORD --body "$DATABASE_PASSWORD"
echo "DATABASE_PASSWORD сгенерирован: $DATABASE_PASSWORD"

RABBITMQ_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
gh secret set RABBITMQ_PASSWORD --body "$RABBITMQ_PASSWORD"
echo "RABBITMQ_PASSWORD сгенерирован"

REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
gh secret set REDIS_PASSWORD --body "$REDIS_PASSWORD"
echo "REDIS_PASSWORD сгенерирован"

# Email (опционально)
read -p "MAIL_HOST [smtp.gmail.com]: " MAIL_HOST
MAIL_HOST=${MAIL_HOST:-smtp.gmail.com}
gh secret set MAIL_HOST --body "$MAIL_HOST"

read -p "MAIL_PORT [587]: " MAIL_PORT
MAIL_PORT=${MAIL_PORT:-587}
gh secret set MAIL_PORT --body "$MAIL_PORT"

read -p "MAIL_USERNAME: " MAIL_USERNAME
gh secret set MAIL_USERNAME --body "$MAIL_USERNAME"

read -sp "MAIL_PASSWORD: " MAIL_PASSWORD
echo
gh secret set MAIL_PASSWORD --body "$MAIL_PASSWORD"

# MinIO
MINIO_ACCESS_KEY=$(openssl rand -base64 16 | tr -d '\n/+=' | head -c 16)
gh secret set MINIO_ACCESS_KEY --body "$MINIO_ACCESS_KEY"
echo "MINIO_ACCESS_KEY сгенерирован"

MINIO_SECRET_KEY=$(openssl rand -base64 32 | tr -d '\n')
gh secret set MINIO_SECRET_KEY --body "$MINIO_SECRET_KEY"
echo "MINIO_SECRET_KEY сгенерирован"

# URLs
read -p "FRONTEND_URL [https://aqstream.ru]: " FRONTEND_URL
FRONTEND_URL=${FRONTEND_URL:-https://aqstream.ru}
gh secret set FRONTEND_URL --body "$FRONTEND_URL"

read -p "CORS_ALLOWED_ORIGINS [https://aqstream.ru]: " CORS_ALLOWED_ORIGINS
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-https://aqstream.ru}
gh secret set CORS_ALLOWED_ORIGINS --body "$CORS_ALLOWED_ORIGINS"

echo ""
echo "=== Секреты добавлены ==="
gh secret list
```

---

## Проверка

После добавления всех секретов:

```bash
gh secret list
```

Ожидаемый вывод:

```
CORS_ALLOWED_ORIGINS  Updated 2025-01-01
DATABASE_PASSWORD     Updated 2025-01-01
FRONTEND_URL          Updated 2025-01-01
JWT_SECRET            Updated 2025-01-01
MAIL_HOST             Updated 2025-01-01
MAIL_PASSWORD         Updated 2025-01-01
MAIL_PORT             Updated 2025-01-01
MAIL_USERNAME         Updated 2025-01-01
MINIO_ACCESS_KEY      Updated 2025-01-01
MINIO_SECRET_KEY      Updated 2025-01-01
RABBITMQ_PASSWORD     Updated 2025-01-01
REDIS_PASSWORD        Updated 2025-01-01
SSH_HOST              Updated 2025-01-01
SSH_KEY               Updated 2025-01-01
SSH_USER              Updated 2025-01-01
TELEGRAM_BOT_TOKEN    Updated 2025-01-01
```

---

## Ротация секретов

### Когда ротировать

- При компрометации секрета
- При увольнении сотрудника с доступом
- Периодически (рекомендуется раз в 90 дней для критичных секретов)

### Процедура

1. Сгенерируйте новое значение
2. Обновите секрет в GitHub
3. Запустите деплой вручную: `Actions → CI/CD Pipeline → Run workflow`
4. Проверьте работоспособность сервисов

### Особенности

- **JWT_SECRET**: После смены все пользователи будут разлогинены
- **DATABASE_PASSWORD**: Нужно сначала изменить в PostgreSQL, потом в GitHub
- **TELEGRAM_BOT_TOKEN**: Получите новый через @BotFather → `/revoke`

---

## Troubleshooting

### Деплой падает с ошибкой "Permission denied"

Проверьте SSH_KEY:

```bash
# Тест подключения локально
ssh -i ~/.ssh/github_deploy deploy@server.aqstream.ru echo "OK"
```

### Сервис не стартует после деплоя

Проверьте логи:

```bash
ssh deploy@server.aqstream.ru "cd ~/aqstream && docker compose logs user-service --tail 50"
```

### Telegram бот не отвечает

Проверьте TELEGRAM_BOT_TOKEN:

```bash
curl "https://api.telegram.org/bot<TOKEN>/getMe"
```

---

## Дальнейшее чтение

- [CI/CD Pipeline](./ci-cd.md)
- [Deploy](./deploy.md)
- [Environments](./environments.md)
