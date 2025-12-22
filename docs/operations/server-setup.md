# Подготовка сервера

Пошаговая инструкция по подготовке production-сервера для AqStream.

## Требования к ВМ

| Параметр | Минимум | Рекомендуется |
| ---------- | --------- | --------------- |
| CPU | 2 vCPU | 4 vCPU |
| RAM | 4 GB | 8 GB |
| Диск | 20 GB SSD | 40 GB SSD |
| ОС | Ubuntu 22.04+ | Ubuntu 24.04 LTS |

---

## Часть 1. Подготовка сервера

### Шаг 1. Подключение к ВМ

```bash
ssh root@IP_АДРЕС
```

### Шаг 2. Базовые инструменты

```bash
sudo apt update && sudo apt install -y make git curl unzip rsync
```

### Шаг 3. Docker

```bash
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
exit
```

После повторного входа:

```bash
docker --version
docker compose version
```

### Шаг 4. Java 25

```bash
curl -s "https://get.sdkman.io" | bash
source ~/.bashrc
sdk install java 25-tem
java -version
```

### Шаг 5. Node.js и pnpm

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
sudo npm install -g pnpm
node -v && pnpm -v
```

### Шаг 6. Клонирование репозитория

**Создать Deploy Key:**

```bash
ssh-keygen -t ed25519 -C "server-deploy" -f ~/.ssh/github_deploy -N ""
cat ~/.ssh/github_deploy.pub
```

Добавить ключ: GitHub → Settings → Deploy keys → Add deploy key

**Настроить SSH:**

```bash
cat >> ~/.ssh/config << 'EOF'
Host github.com
    IdentityFile ~/.ssh/github_deploy
    IdentitiesOnly yes
EOF
```

**Клонировать:**

```bash
git clone git@github.com:egorov-ma/aqstream.git ~/aqstream
cd ~/aqstream
```

### Шаг 7. Проверка инфраструктуры

```bash
make infra-up
make health
```

---

## Часть 2. DNS и домен

### Шаг 8. Узнать IP сервера

```bash
curl ifconfig.me
```

### Шаг 9. Добавить A-записи в DNS

| Subdomain | Тип | Значение |
| ----------- | ----- | ---------- |
| `@` | A | IP сервера |
| `www` | A | IP сервера |
| `api` | A | IP сервера |
| `docs` | A | IP сервера |

### Шаг 10. Проверить DNS

```bash
dig aqstream.ru +short
```

---

## Часть 3. Nginx и SSL

### Шаг 11. Установка Nginx

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
```

### Шаг 12. Конфигурация сайтов

**API Gateway:**

```bash
sudo tee /etc/nginx/sites-available/api.aqstream << 'EOF'
server {
    listen 80;
    server_name api.aqstream.ru;
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
```

**Документация:**

```bash
sudo mkdir -p /var/www/docs.aqstream.ru
sudo tee /etc/nginx/sites-available/docs.aqstream << 'EOF'
server {
    listen 80;
    server_name docs.aqstream.ru;
    root /var/www/docs.aqstream.ru;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF
```

**Frontend:**

```bash
sudo tee /etc/nginx/sites-available/aqstream << 'EOF'
server {
    listen 80;
    server_name aqstream.ru www.aqstream.ru;
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_cache_bypass $http_upgrade;
    }
}
EOF
```

### Шаг 13. Активация сайтов

```bash
sudo ln -sf /etc/nginx/sites-available/api.aqstream /etc/nginx/sites-enabled/
sudo ln -sf /etc/nginx/sites-available/docs.aqstream /etc/nginx/sites-enabled/
sudo ln -sf /etc/nginx/sites-available/aqstream /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

### Шаг 14. SSL сертификаты

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d aqstream.ru -d www.aqstream.ru -d api.aqstream.ru -d docs.aqstream.ru
sudo certbot renew --dry-run
```

---

## Часть 4. CI/CD (GitHub Actions)

### Шаг 15. Создать SSH ключ для деплоя

**На локальной машине** (не на сервере!):

```bash
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions -N ""
```

### Шаг 16. Добавить публичный ключ на сервер

```bash
# Локальная машина: показать публичный ключ
cat ~/.ssh/github_actions.pub

# Сервер: добавить ключ
ssh root@ВАШ_IP "echo 'ВСТАВИТЬ_ПУБЛИЧНЫЙ_КЛЮЧ' >> ~/.ssh/authorized_keys"
```

### Шаг 17. Проверить подключение

```bash
ssh -i ~/.ssh/github_actions root@ВАШ_IP "echo OK"
```

### Шаг 18. Добавить GitHub Secrets

GitHub → Settings → Secrets and variables → Actions → New repository secret

| Secret | Значение |
| -------- | ---------- |
| `SSH_HOST` | IP сервера |
| `SSH_USER` | `root` |
| `SSH_KEY` | Содержимое `cat ~/.ssh/github_actions` |

**Важно:** Копировать весь ключ включая `-----BEGIN` и `-----END-----`.

### Частые ошибки CI/CD

| Ошибка | Решение |
| -------- | --------- |
| `passphrase protected` | Создать ключ с `-N ""` |
| `no supported methods` | Добавить публичный ключ на сервер |
| `Permission denied` | Проверить `SSH_USER` |

---

## Проверка готовности

| URL | Ожидаемый результат |
| ----- | --------------------- |
| <https://api.aqstream.ru/actuator/health> | `{"status":"UP"}` |
| <https://docs.aqstream.ru> | Документация |
| <https://aqstream.ru> | Frontend |

---

## Полезные команды

| Команда | Описание |
| --------- | ---------- |
| `make infra-up` | Запустить инфраструктуру |
| `make infra-down` | Остановить инфраструктуру |
| `make health` | Проверить сервисы |
| `make logs` | Логи контейнеров |
| `docker ps` | Статус контейнеров |
| `sudo nginx -t` | Проверить конфиг Nginx |
| `sudo certbot certificates` | Статус SSL |

---

## Troubleshooting

### Docker permission denied

```bash
sudo usermod -aG docker $USER && exit
```

### SDKMAN не найден

```bash
source ~/.bashrc
```

### Порт занят

```bash
sudo lsof -i :5432
make down
```

---

## Чеклист

- [ ] Docker установлен
- [ ] Java 25 установлена
- [ ] Node.js 20 установлен
- [ ] Репозиторий склонирован
- [ ] `make health` проходит
- [ ] DNS настроен
- [ ] Nginx работает
- [ ] SSL сертификаты получены
- [ ] GitHub Secrets настроены
- [ ] CI/CD деплой работает

---

## Дальнейшее чтение

- [CI/CD](./ci-cd.md) — пайплайны
- [Environments](./environments.md) — окружения
- [Runbook](./runbook.md) — операционные процедуры
