# P2-005 Запросы на создание организаций

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `review` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Организации](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Создание организации требует одобрения администратора платформы ([FR-3.1](../../business/functional-requirements.md)). Это защищает от спама и обеспечивает качество организаторов на платформе. После одобрения пользователь может создать организацию и стать её владельцем.

### Технический контекст

- OrganizationRequest — глобальная таблица (без tenant_id)
- Админ платформы видит все запросы
- После одобрения пользователь создаёт Organization
- Organization.id используется как tenant_id для всех связанных данных

**Связанные документы:**
- [User Service](../../tech-stack/backend/services/user-service.md#organization-requests) — API endpoints
- [Domain Model - OrganizationRequest](../../data/domain-model.md#organizationrequest)
- [Role Model](../../business/role-model.md) — роль Админа платформы
- [User Journeys - Journey 3](../../business/user-journeys.md#journey-3-создание-организации)
- [User Journeys - Journey 8](../../business/user-journeys.md#journey-8-одобрение-запроса-на-организацию)

## Цель

Реализовать процесс запроса на создание организации с одобрением администратором.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### Подача запроса

- [x] Пользователь может подать запрос на создание организации (`POST /api/v1/organization-requests`)
- [x] Запрос содержит: название, slug (URL), описание
- [x] Slug проверяется на уникальность (среди организаций и pending запросов)
- [x] Slug валидируется: только lowercase, цифры, дефис; длина 3-50 символов
- [x] Пользователь может иметь только один pending запрос
- [x] Пользователь видит статус своего запроса

### Рассмотрение админом

- [x] Админ видит список всех pending запросов (`GET /api/v1/organization-requests`)
- [x] Админ может одобрить запрос (`POST /api/v1/organization-requests/{id}/approve`)
- [x] Админ может отклонить запрос с указанием причины (`POST /api/v1/organization-requests/{id}/reject`)
- [x] События публикуются в RabbitMQ при одобрении/отклонении
- [ ] Уведомления пользователю → см. [P2-014](./P2-014-notifications-templates.md)

### После одобрения

- [x] Одобренный пользователь может создать организацию
- [x] Slug из запроса резервируется до создания организации (7 дней)
- [x] При создании организации пользователь становится OWNER (см. [P2-006](./P2-006-organizations-crud.md))

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены (уведомления → [P2-014](./P2-014-notifications-templates.md))
- [x] Код написан согласно code style проекта
- [x] Unit тесты написаны
- [x] Integration тесты написаны
- [x] Миграции созданы с rollback
- [x] Опубликованы events в RabbitMQ (`organization.request.created`, `organization.request.approved/rejected`)
- [x] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service-api`, `user-service-service`, `user-service-db`
- [ ] Frontend: форма запроса, страница статуса, админ-панель
- [x] Database: таблица `organization_requests`
- [ ] Infrastructure: —

### Модель данных

См. [Domain Model - OrganizationRequest](../../data/domain-model.md#organizationrequest)

```sql
CREATE TABLE user_service.organization_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_service.users(id),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    reviewed_by UUID REFERENCES user_service.users(id),
    review_comment TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_pending_slug UNIQUE (slug, status)
        WHERE status = 'PENDING'
);
```

### API Endpoints

```
POST   /api/v1/organization-requests           — подать запрос
GET    /api/v1/organization-requests           — список (админ: все, user: свои)
GET    /api/v1/organization-requests/{id}      — детали запроса
POST   /api/v1/organization-requests/{id}/approve — одобрить (админ)
POST   /api/v1/organization-requests/{id}/reject  — отклонить (админ)
```

### RabbitMQ Events

- `organization.request.created` — для уведомления админов
- `organization.request.approved` — для уведомления пользователя
- `organization.request.rejected` — для уведомления пользователя

## Зависимости

### Блокирует

- [P2-006](./P2-006-organizations-crud.md) CRUD организаций
- [P2-014](./P2-014-notifications-templates.md) Уведомления (события для отправки)

### Зависит от

- [P2-001](./P2-001-auth-email-registration.md) User entity и авторизация

## Out of Scope

- Автоматическое одобрение
- Платные планы организаций
- Верификация организаций (синяя галочка)

## Заметки

- В Phase 2 админ-панель может быть простой (список запросов)
- Роль ADMIN определяется в БД (отдельная таблица или флаг в users)
- Уведомления админам о новых запросах — через Telegram (если подключен)
