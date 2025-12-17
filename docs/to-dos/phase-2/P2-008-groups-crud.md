# P2-008 Группы для приватных событий

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
| Приоритет | `high` |
| Связь с roadmap | [Roadmap - Организации](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Группы позволяют создавать приватные события внутри организации ([FR-3.4](../../business/functional-requirements.md)). Например: региональные команды, VIP-доступ, корпоративные мероприятия. Участники группы видят и могут регистрироваться на события, привязанные к их группе.

### Технический контекст

- Группы принадлежат организации
- Пользователь приглашается по уникальному инвайт-коду
- Событие может быть привязано к одной или нескольким группам
- Владелец и модераторы организации видят все события (включая групповые)

**Связанные документы:**
- [User Service](../../tech-stack/backend/services/user-service.md#groups) — API endpoints
- [Domain Model - Group](../../data/domain-model.md#group)
- [Domain Model - GroupMember](../../data/domain-model.md#groupmember)
- [Role Model - Группы](../../business/role-model.md#группы)

## Цель

Реализовать CRUD групп внутри организации и механизм приглашения участников по инвайт-коду.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] Нет блокеров

## Acceptance Criteria

### CRUD Групп

- [ ] OWNER и MODERATOR могут создавать группы (`POST /api/v1/organizations/{id}/groups`)
- [ ] Только OWNER может удалять группы (`DELETE /api/v1/groups/{id}`)
- [ ] При создании генерируется уникальный invite_code (8 символов)
- [ ] Группа имеет: название, описание (опционально), invite_code
- [ ] Список групп организации (`GET /api/v1/organizations/{id}/groups`)

### Участники групп

- [ ] Пользователь присоединяется по инвайт-коду (`POST /api/v1/groups/join/{inviteCode}`)
- [ ] Владелец/модератор видит список участников группы (`GET /api/v1/groups/{id}/members`)
- [ ] Владелец/модератор может удалить участника из группы
- [ ] Участник может покинуть группу самостоятельно
- [ ] Пользователь может состоять в нескольких группах одной организации

### Инвайт-коды

- [ ] Инвайт-код уникален глобально
- [ ] OWNER/MODERATOR могут регенерировать инвайт-код
- [ ] Ссылка для приглашения: `/groups/join/{inviteCode}` или Telegram deeplink

### Видимость

- [ ] Владелец и модераторы видят все группы организации
- [ ] Участник видит только группы, в которых состоит

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Код написан согласно code style проекта
- [ ] Unit тесты написаны
- [ ] Integration тесты написаны
- [ ] Миграции созданы
- [ ] Events опубликованы (`group.created`, `group.member.added`)
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Backend: `user-service-api`, `user-service-service`, `user-service-db`
- [ ] Frontend: управление группами, присоединение по коду
- [x] Database: таблицы `groups`, `group_members`
- [ ] Infrastructure: —

### Модель данных

```sql
CREATE TABLE user_service.groups (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES user_service.organizations(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    invite_code VARCHAR(8) NOT NULL UNIQUE,
    created_by UUID NOT NULL REFERENCES user_service.users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_service.group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES user_service.groups(id),
    user_id UUID NOT NULL REFERENCES user_service.users(id),
    invited_by UUID REFERENCES user_service.users(id),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_groups_organization ON user_service.groups(organization_id);
CREATE INDEX idx_groups_invite_code ON user_service.groups(invite_code);
CREATE INDEX idx_group_members_user ON user_service.group_members(user_id);
```

### API Endpoints

```
# Groups
GET    /api/v1/organizations/{id}/groups    — список групп организации
POST   /api/v1/organizations/{id}/groups    — создание группы
GET    /api/v1/groups/{id}                  — детали группы
PUT    /api/v1/groups/{id}                  — обновление группы
DELETE /api/v1/groups/{id}                  — удаление группы
POST   /api/v1/groups/{id}/regenerate-code  — новый инвайт-код

# Members
GET    /api/v1/groups/{id}/members          — список участников
DELETE /api/v1/groups/{id}/members/{userId} — удаление участника
POST   /api/v1/groups/{id}/leave            — выход из группы

# Join
POST   /api/v1/groups/join/{inviteCode}     — присоединение по коду
```

### Генерация invite_code

```java
private String generateInviteCode() {
    // 8 символов: uppercase буквы + цифры (без похожих: 0, O, I, L)
    String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    SecureRandom random = new SecureRandom();
    return random.ints(8, 0, chars.length())
        .mapToObj(chars::charAt)
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .toString();
}
```

## Зависимости

### Блокирует

- [P2-009](./P2-009-events-crud.md) События (привязка к группе)

### Зависит от

- [P2-006](./P2-006-organizations-crud.md) Организации

## Out of Scope

- Вложенные группы (группы в группах)
- Роли внутри группы (все участники равны)
- Экспорт участников группы
- Групповые рассылки

## Заметки

- Invite code должен быть читаемым (для ручного ввода)
- При удалении группы — события остаются, но становятся публичными внутри организации
- Пользователь может состоять в нескольких группах одной организации
