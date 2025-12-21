# P2-006 CRUD организаций и управление членами

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `review` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Организации](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Организация — это tenant в системе AqStream. Все события, регистрации и данные принадлежат организации. Пользователь может быть членом нескольких организаций с разными ролями. Приглашение новых членов происходит через Telegram.

### Технический контекст

- Organization.id используется как tenant_id во всех бизнес-таблицах
- Row Level Security изолирует данные организаций
- Роли: OWNER (один), MODERATOR (много)
- Приглашение через Telegram deeplink

**Связанные документы:**
- [User Service](../../tech-stack/backend/services/user-service.md#organizations) — API endpoints
- [Domain Model - Organization](../../data/domain-model.md#organization)
- [Domain Model - OrganizationMember](../../data/domain-model.md#organizationmember)
- [Role Model](../../business/role-model.md#уровень-организации)
- [Data Architecture](../../architecture/data-architecture.md) — RLS

## Цель

Реализовать полный CRUD организаций, управление членами и переключение между организациями.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### CRUD Организаций

- [x] Пользователь с одобренным запросом может создать организацию (`POST /api/v1/organizations`)
- [x] Slug из одобренного запроса используется автоматически
- [x] Создатель становится OWNER
- [x] Организация создаётся с настройками по умолчанию
- [x] OWNER может редактировать организацию (`PUT /api/v1/organizations/{id}`)
- [x] OWNER может удалить организацию (`DELETE /api/v1/organizations/{id}`) — soft delete
- [ ] При удалении все события архивируются → **P2-009** (event-service слушает `organization.deleted`)

### Управление членами

- [x] Список членов организации (`GET /api/v1/organizations/{id}/members`)
- [x] Приглашение нового члена через Telegram (`POST /api/v1/organizations/{id}/invite`)
- [x] Приглашённый получает deeplink в Telegram
- [x] При переходе по ссылке — становится MODERATOR
- [x] Изменение роли члена (`PUT /api/v1/organizations/{id}/members/{userId}`)
- [x] Удаление члена (`DELETE /api/v1/organizations/{id}/members/{userId}`)
- [x] OWNER не может быть удалён
- [x] Приглашение действительно 7 дней ([FR-3.2.5](../../business/functional-requirements.md))

### Роли и права

- [x] OWNER — полный контроль, удаление организации, передача владения
- [x] MODERATOR — управление событиями, членами (кроме OWNER), check-in
- [x] Только OWNER может назначать роли
- [x] Только OWNER может удалять организацию

### Переключение организаций

- [x] Пользователь видит список своих организаций (`GET /api/v1/organizations`)
- [x] Пользователь может переключиться на другую организацию
- [x] При переключении выдаётся новый access token с другим tenantId

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены (backend часть, frontend — P2-016)
- [x] Код написан согласно code style проекта
- [x] Unit тесты написаны
- [x] Integration тесты написаны (OrganizationControllerIntegrationTest добавлен)
- [x] Миграции созданы (RLS политики — P2-007)
- [ ] Events опубликованы в RabbitMQ → **common-messaging** (отдельная задача после настройки Outbox)
- [x] Code review пройден
- [x] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service-api`, `user-service-service`, `user-service-db`
- [ ] Frontend: создание организации, управление членами, переключатель → **P2-016**
- [x] Database: таблицы `organizations`, `organization_members`, `organization_invites`
- [x] Infrastructure: —

### Модель данных

```sql
-- Organizations
CREATE TABLE user_service.organizations (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES user_service.users(id),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Organization Members
CREATE TABLE user_service.organization_members (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES user_service.organizations(id),
    user_id UUID NOT NULL REFERENCES user_service.users(id),
    role VARCHAR(20) NOT NULL, -- OWNER, MODERATOR
    invited_by UUID REFERENCES user_service.users(id),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(organization_id, user_id)
);

-- Organization Invites
CREATE TABLE user_service.organization_invites (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES user_service.organizations(id),
    invite_code VARCHAR(32) NOT NULL UNIQUE,
    invited_by UUID NOT NULL REFERENCES user_service.users(id),
    telegram_username VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'MODERATOR',
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API Endpoints

```
# Organizations
GET    /api/v1/organizations              — список организаций пользователя
POST   /api/v1/organizations              — создание организации
GET    /api/v1/organizations/{id}         — детали организации
PUT    /api/v1/organizations/{id}         — обновление
DELETE /api/v1/organizations/{id}         — удаление (soft delete)
POST   /api/v1/organizations/{id}/switch  — переключение (новый token)

# Members
GET    /api/v1/organizations/{id}/members           — список членов
POST   /api/v1/organizations/{id}/invite            — создание приглашения
PUT    /api/v1/organizations/{id}/members/{userId}  — изменение роли
DELETE /api/v1/organizations/{id}/members/{userId}  — удаление члена

# Invite acceptance
POST   /api/v1/organizations/join/{inviteCode}      — принятие приглашения
```

### RabbitMQ Events

- `organization.created`
- `organization.updated`
- `organization.deleted`
- `organization.member.added`
- `organization.member.removed`
- `organization.member.role.changed`

## Зависимости

### Блокирует

- [P2-007](./P2-007-organizations-rls.md) Row Level Security
- [P2-008](./P2-008-groups-crud.md) Группы
- [P2-009](./P2-009-events-crud.md) События (требуют tenant_id)

### Зависит от

- [P2-001](./P2-001-auth-email-registration.md) User entity
- [P2-005](./P2-005-organizations-requests.md) Одобрение запросов

## Out of Scope

- Billing и подписки организаций
- Custom domain для организации
- Branding (кастомные цвета, логотипы на страницах)
- Передача владения (можно добавить позже)

## Заметки

- При создании организации нужно также создать первую запись в organization_members с role=OWNER
- Telegram deeplink формат: `https://t.me/AqStreamBot?start=invite_{code}`
- При переключении организации нужно генерировать новый access token
