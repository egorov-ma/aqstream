# Подготовка сервера

Пошаговая инструкция по подготовке ВМ для развёртывания AqStream.

## Требования к ВМ

| Параметр | Минимум | Рекомендуется |
|----------|---------|---------------|
| CPU | 2 vCPU | 4 vCPU |
| RAM | 4 GB | 8 GB |
| Диск | 20 GB SSD | 40 GB SSD |
| ОС | Ubuntu 22.04+ | Ubuntu 24.04 LTS |

## Подключение к ВМ

### Получить публичный SSH ключ

```bash
# На локальной машине
cat ~/.ssh/id_ed25519.pub
# или
cat ~/.ssh/id_rsa.pub
```

### Создание ВМ

При создании ВМ (Yandex Cloud, AWS, etc.) указать публичный SSH ключ в настройках доступа.

### Подключение

```bash
ssh username@ip-address
```

Где:
- `username` — имя пользователя, указанное при создании ВМ
- `ip-address` — публичный IP-адрес ВМ

Пример:

```bash
ssh aqstream@51.250.123.45
```

### Настройка SSH config (опционально)

Для удобного подключения без запоминания IP:

```bash
# На локальной машине
cat >> ~/.ssh/config << 'EOF'
Host aqstream-prod
    HostName 51.250.123.45
    User aqstream
    IdentityFile ~/.ssh/id_ed25519
EOF
```

После этого подключение:

```bash
ssh aqstream-prod
```

## Шаг 1. Базовые инструменты

```bash
sudo apt update && sudo apt install -y \
    make \
    git \
    curl \
    unzip \
    zip
```

## Шаг 2. Docker

```bash
# Установка Docker
sudo apt install -y docker.io docker-compose-v2

# Добавить пользователя в группу docker
sudo usermod -aG docker $USER

# Применить изменения (перелогиниться)
exit
```

После повторного входа проверить:

```bash
docker --version
docker compose version
```

## Шаг 3. Java 25 (Eclipse Temurin)

```bash
# Установка SDKMAN
curl -s "https://get.sdkman.io" | bash

# Активировать SDKMAN (или перелогиниться)
source ~/.bashrc

# Установка Java 25 Temurin
sdk install java 25-tem
```

Проверить:

```bash
java -version
# openjdk version "25" ...
# OpenJDK Runtime Environment Temurin-25...
```

## Шаг 4. Node.js и pnpm

```bash
# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# pnpm
sudo npm install -g pnpm
```

Проверить:

```bash
node -v   # v20.x.x
pnpm -v   # 9.x.x
```

## Шаг 5. SSH ключ для GitHub

Создать Deploy Key для безопасного доступа к репозиторию:

```bash
# Генерация ключа (выполнить как есть, без изменений)
ssh-keygen -t ed25519 -C "aqstream-$(hostname)" -f ~/.ssh/github_deploy
# На запрос passphrase — просто нажать Enter (пустой пароль)

# Показать публичный ключ
cat ~/.ssh/github_deploy.pub
```

Добавить ключ в GitHub:

1. Открыть репозиторий → **Settings** → **Deploy keys** → **Add deploy key**
2. Title: название сервера (например `production-vm`)
3. Вставить публичный ключ
4. **Add key**

Настроить SSH:

```bash
cat >> ~/.ssh/config << 'EOF'
Host github.com
    IdentityFile ~/.ssh/github_deploy
    IdentitiesOnly yes
EOF
```

## Шаг 6. Клонирование репозитория

```bash
cd ~
git clone git@github.com:egorov-ma/aqstream.git
cd aqstream
```

## Шаг 7. Первый запуск

### Вариант A: Только инфраструктура (для разработки)

Запускает PostgreSQL, Redis, RabbitMQ, MinIO без backend-сервисов:

```bash
make infra-up
```

### Вариант B: Полный стек

Требуется предварительная сборка:

```bash
# Сборка всех сервисов
./gradlew build

# Запуск всего стека
make up
```

## Проверка

```bash
# Статус контейнеров
make infra-ps

# Health check инфраструктуры
make health
```

Ожидаемый вывод:

```
Проверка PostgreSQL...
accepting connections
Проверка Redis...
PONG
Проверка RabbitMQ...
RabbitMQ on node rabbit@... is fully booted and running
```

## Привязка домена (Reg.ru)

### Шаг 1. Узнать IP-адрес ВМ

```bash
# На ВМ
curl ifconfig.me
```

### Шаг 2. Настроить DNS-серверы

1. Войти в [личный кабинет Reg.ru](https://www.reg.ru/user/account)
2. Перейти в **Домены** → выбрать нужный домен
3. В блоке **DNS-серверы** убедиться, что указаны:
   ```
   ns1.reg.ru
   ns2.reg.ru
   ```

### Шаг 3. Добавить A-записи

В блоке **DNS-серверы и управление зоной** нажать **Изменить** и добавить записи:

| Subdomain | Тип | IP Address | Назначение |
|-----------|-----|------------|------------|
| `@` | A | IP вашей ВМ | Основной домен (aqstream.ru) |
| `www` | A | IP вашей ВМ | www.aqstream.ru |
| `api` | A | IP вашей ВМ | api.aqstream.ru (API Gateway) |
| `docs` | A | IP вашей ВМ | docs.aqstream.ru (Документация) |

Для каждой записи:
1. Выбрать тип записи **A**
2. Заполнить **Subdomain** и **IP Address**
3. Нажать **Добавить**

### Шаг 4. Дождаться обновления DNS

Обновление занимает от 15 минут до 24 часов.

### Проверка DNS

```bash
# С локальной машины
dig aqstream.ru +short
# Должен вернуть IP-адрес ВМ

# Или через nslookup
nslookup aqstream.ru
```

## Настройка Nginx (reverse proxy)

Nginx нужен для:
- Проксирования запросов на внутренние сервисы
- SSL/HTTPS (сертификаты Let's Encrypt)
- Работа без указания порта в URL

### Схема работы

```
aqstream.ru:80/443      → Nginx → localhost:3000 (frontend)
api.aqstream.ru:80/443  → Nginx → localhost:8080 (gateway)
docs.aqstream.ru:80/443 → Nginx → /var/www/docs.aqstream.ru (static)
```

### Шаг 1. Установка Nginx

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### Шаг 2. Конфигурация для frontend

```bash
sudo nano /etc/nginx/sites-available/aqstream
```

**Работа с nano:**
1. Откроется текстовый редактор
2. Вставить текст (правой кнопкой мыши или Ctrl+Shift+V)
3. Сохранить: `Ctrl+O`, затем `Enter`
4. Выйти: `Ctrl+X`

Вставить:

```nginx
server {
    listen 80;
    server_name aqstream.ru www.aqstream.ru;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

### Шаг 3. Конфигурация для API

```bash
sudo nano /etc/nginx/sites-available/api.aqstream
```

Вставить:

```nginx
server {
    listen 80;
    server_name api.aqstream.ru;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Шаг 4. Конфигурация для документации

```bash
# Создать директорию для документации
sudo mkdir -p /var/www/docs.aqstream.ru

# Создать конфигурацию
sudo nano /etc/nginx/sites-available/docs.aqstream
```

Вставить:

```nginx
server {
    listen 80;
    server_name docs.aqstream.ru;

    root /var/www/docs.aqstream.ru;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Кэширование статики
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### Шаг 5. Активировать конфигурации

```bash
# Создать символические ссылки
sudo ln -s /etc/nginx/sites-available/aqstream /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/api.aqstream /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/docs.aqstream /etc/nginx/sites-enabled/

# Удалить дефолтный сайт
sudo rm /etc/nginx/sites-enabled/default

# Проверить конфигурацию
sudo nginx -t

# Перезагрузить Nginx
sudo systemctl reload nginx
```

### Шаг 6. SSL сертификаты (Let's Encrypt)

```bash
# Установить Certbot
sudo apt install -y certbot python3-certbot-nginx

# Получить сертификаты (выполнить как есть)
sudo certbot --nginx -d aqstream.ru -d www.aqstream.ru -d api.aqstream.ru -d docs.aqstream.ru
```

Certbot:
- Запросит email для уведомлений
- Автоматически настроит HTTPS
- Добавит автообновление сертификатов

### Шаг 7. Проверка

```bash
# Статус Nginx
sudo systemctl status nginx

# Проверить сертификаты
sudo certbot certificates

# Тест автообновления
sudo certbot renew --dry-run
```

### Проверка в браузере

- `https://aqstream.ru` — должен открыться frontend
- `https://api.aqstream.ru/actuator/health` — должен вернуть статус API
- `https://docs.aqstream.ru` — должна открыться документация

### Шаг 8. Деплой документации

Сборка и деплой MkDocs документации:

```bash
# Установить Python зависимости (первый раз)
make docs-install

# Собрать документацию
make docs-build

# Скопировать в директорию Nginx
sudo cp -r site/* /var/www/docs.aqstream.ru/
```

Или через CI/CD (`.github/workflows/docs.yml`):

```yaml
name: Deploy Docs

on:
  push:
    branches: [main]
    paths: ['docs/**']

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.12'

      - name: Install dependencies
        run: pip install -r docs/_internal/doc-as-code/requirements.txt

      - name: Build docs
        run: make docs-build

      - name: Deploy to server
        uses: appleboy/scp-action@master
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_KEY }}
          source: "site/*"
          target: "/var/www/docs.aqstream.ru"
          strip_components: 1
```

### Настройка GitHub Secrets

Для работы CI/CD деплоя необходимо добавить secrets в GitHub:

1. Открыть репозиторий на GitHub
2. Перейти в **Settings** → **Secrets and variables** → **Actions**
3. Нажать **New repository secret**
4. Добавить следующие secrets:

#### Необходимые secrets

Все workflows используют одни и те же 3 secrets:

| Secret | Описание | Откуда взять |
|--------|----------|--------------|
| `SSH_HOST` | IP-адрес или домен сервера | `curl ifconfig.me` на сервере |
| `SSH_USER` | Имя пользователя SSH | Имя из команды `ssh user@host` |
| `SSH_KEY` | Приватный SSH ключ (без пароля!) | См. инструкцию ниже |

#### Как получить значения

**SSH_HOST:**

```bash
# На сервере — узнать публичный IP
curl ifconfig.me
# Пример вывода: 51.250.123.45
```

Можно использовать IP-адрес (`51.250.123.45`) или домен (`aqstream.ru`).

**SSH_USER:**

Это имя пользователя, которое вы используете при SSH-подключении:

```bash
ssh aqstream@51.250.123.45
#     ^^^^^^^^ — это SSH_USER
```

**SSH_KEY:**

GitHub Actions **не поддерживает** ключи с паролем. Создайте отдельный ключ без пароля.

**Шаг 1. Создать ключ (на локальной машине):**

```bash
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions -N ""
```

**Шаг 2. Добавить публичный ключ на сервер:**

```bash
# Локальная машина: показать публичный ключ
cat ~/.ssh/github_actions.pub
# Скопировать вывод (ssh-ed25519 AAAA... github-actions)

# Сервер: добавить ключ
ssh root@ВАШ_IP
echo 'ВСТАВИТЬ_ПУБЛИЧНЫЙ_КЛЮЧ' >> ~/.ssh/authorized_keys
exit
```

**Шаг 3. Проверить подключение (на локальной машине):**

```bash
ssh -i ~/.ssh/github_actions root@ВАШ_IP "echo OK"
# Должно вывести: OK
```

**Шаг 4. Скопировать приватный ключ в GitHub Secret:**

```bash
# Локальная машина
cat ~/.ssh/github_actions
```

Скопировать **весь** вывод (включая `-----BEGIN` и `-----END-----`) в GitHub Secret `SSH_KEY`.

**Частые ошибки:**

| Ошибка | Причина | Решение |
|--------|---------|---------|
| `passphrase protected` | Ключ с паролем | Создать новый ключ с `-N ""` |
| `no supported methods` | Публичный ключ не на сервере | Добавить в `authorized_keys` |
| `Permission denied` | Неверный пользователь | Проверить `SSH_USER` |

## Полезные команды

| Команда | Описание |
|---------|----------|
| `make infra-up` | Запустить инфраструктуру |
| `make infra-down` | Остановить инфраструктуру |
| `make up` | Запустить весь стек |
| `make down` | Остановить весь стек |
| `make logs` | Логи всех контейнеров |
| `make health` | Проверить доступность сервисов |
| `./gradlew build` | Собрать Java-проект |
| `make docs-install` | Установить Python зависимости для документации |
| `make docs-serve` | Локальный сервер документации |
| `make docs-build` | Собрать документацию в site/ |

## Troubleshooting

### Docker permission denied

```bash
# Проверить группу
groups
# Должен быть docker в списке

# Если нет — добавить и перелогиниться
sudo usermod -aG docker $USER
exit
```

### SDKMAN не найден после установки

```bash
source ~/.bashrc
# или перелогиниться
```

### Ошибка сборки "COPY build/libs/*.jar"

JAR-файлы не собраны. Выполнить:

```bash
./gradlew build
```

### Порт занят

```bash
# Найти процесс
sudo lsof -i :5432

# Остановить контейнеры
make down
```

## Чеклист готовности

- [ ] Docker и Docker Compose установлены
- [ ] Java 25 (Temurin) установлена
- [ ] Node.js 20 и pnpm установлены
- [ ] Deploy Key добавлен в GitHub
- [ ] Репозиторий склонирован
- [ ] `make health` проходит успешно
- [ ] DNS записи настроены (aqstream.ru, api.aqstream.ru, docs.aqstream.ru)
- [ ] Nginx установлен и настроен
- [ ] GitHub Secrets настроены (SSH_HOST, SSH_USER, SSH_KEY)
- [ ] Документация задеплоена в /var/www/docs.aqstream.ru
- [ ] SSL сертификаты получены

## Дальнейшее чтение

- [Deploy](./deploy.md) — процесс деплоя и CI/CD
- [Environments](./environments.md) — конфигурация окружений
- [Runbook](./runbook.md) — операционные процедуры
