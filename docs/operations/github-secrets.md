# GitHub Secrets

Инструкция по настройке секретов для CI/CD pipeline.

## Обзор

Все секреты хранятся в GitHub и передаются на сервер при деплое.

**Путь:** `Settings → Secrets and variables → Actions → Repository secrets`

## Список секретов

| Secret | Обязательный | Описание |
|--------|--------------|----------|
| `SSH_HOST` | Да | Адрес production сервера |
| `SSH_USER` | Да | SSH пользователь |
| `SSH_KEY` | Да | Приватный SSH ключ |
| `JWT_SECRET` | Да | Секрет для подписи JWT токенов |
| `TELEGRAM_BOT_TOKEN` | Да | Токен Telegram бота |
| `DATABASE_PASSWORD` | Да | Пароль PostgreSQL |
| `RABBITMQ_PASSWORD` | Да | Пароль RabbitMQ |
| `REDIS_PASSWORD` | Да | Пароль Redis |
| `MAIL_HOST` | Нет | SMTP сервер |
| `MAIL_PORT` | Нет | SMTP порт |
| `MAIL_USERNAME` | Нет | SMTP логин |
| `MAIL_PASSWORD` | Нет | SMTP пароль |
| `MINIO_ACCESS_KEY` | Да | MinIO access key |
| `MINIO_SECRET_KEY` | Да | MinIO secret key |
| `FRONTEND_URL` | Да | URL фронтенда |
| `CORS_ALLOWED_ORIGINS` | Да | Разрешённые CORS origins |

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
