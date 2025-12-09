# Задачи проекта (To-Dos)

Директория содержит детальные описания задач проекта, связанных с [Roadmap](../business/roadmap.md).

## Структура

```
to-dos/
├── _template.md       # Шаблон для создания новых задач
├── phase-1/           # Задачи Phase 1: Foundation
├── phase-2/           # Задачи Phase 2: Core
├── phase-3/           # Задачи Phase 3: Growth
└── phase-4/           # Задачи Phase 4: Scale
```

## Формат ID задач

Каждая задача имеет уникальный ID в формате:

```
P{фаза}-{номер}
```

Примеры:
- `P1-001` — первая задача Phase 1
- `P2-015` — пятнадцатая задача Phase 2

## Жизненный цикл задачи

```
backlog → ready → in_progress → review → done
```

| Статус | Описание |
|--------|----------|
| `backlog` | Задача создана, но не готова к работе |
| `ready` | Задача готова к взятию (DoR выполнен) |
| `in_progress` | Задача в работе |
| `review` | На code review |
| `done` | Завершена (DoD выполнен) |

## Как создать задачу

1. Скопировать [_template.md](./_template.md)
2. Переименовать по формату: `P{фаза}-{номер}-{короткое-название}.md`
3. Заполнить все секции шаблона
4. Положить в соответствующую папку фазы

## Связь с Roadmap

Каждая задача связана с конкретным пунктом [Roadmap](../business/roadmap.md). При завершении задачи:

1. Обновить статус в файле задачи (`done`)
2. Отметить чеклист в Roadmap как выполненный
3. Обновить таблицу "Текущий статус" в Roadmap при завершении блока задач

## Definition of Done (глобальный)

Каждая задача должна соответствовать:

- Код написан согласно code style проекта
- Unit тесты написаны и проходят
- Integration тесты написаны (если применимо)
- Документация обновлена (если затрагивает API или архитектуру)
- Code review пройден
- CI/CD pipeline проходит

## Текущий прогресс

| Фаза | Всего задач | Готово | В работе |
|------|-------------|--------|----------|
| Phase 1: Foundation | 13 | 1 | 0 |
| Phase 2: Core | — | — | — |
| Phase 3: Growth | — | — | — |
| Phase 4: Scale | — | — | — |

## Phase 1: Foundation — Задачи

| ID | Название | Приоритет | Статус | Зависит от |
|----|----------|-----------|--------|------------|
| [P1-001](./phase-1/P1-001-monorepo-setup.md) | Настройка монорепозитория | critical | done | — |
| [P1-002](./phase-1/P1-002-docker-compose-local.md) | Docker Compose для локальной разработки | critical | backlog | P1-001 |
| [P1-003](./phase-1/P1-003-ci-cd-pipeline.md) | CI/CD Pipeline (GitHub Actions) | critical | backlog | P1-001 |
| [P1-004](./phase-1/P1-004-gradle-multimodule.md) | Gradle Multi-Module структура | critical | backlog | P1-001 |
| [P1-005](./phase-1/P1-005-common-modules.md) | Common Modules реализация | critical | backlog | P1-004 |
| [P1-006](./phase-1/P1-006-api-gateway.md) | API Gateway Setup | critical | backlog | P1-002, P1-005 |
| [P1-007](./phase-1/P1-007-postgresql-multitenancy.md) | PostgreSQL с Multi-Tenancy (RLS) | critical | backlog | P1-002 |
| [P1-008](./phase-1/P1-008-rabbitmq-setup.md) | RabbitMQ Setup | high | backlog | P1-002, P1-005 |
| [P1-009](./phase-1/P1-009-nextjs-project.md) | Next.js 14 Project Setup | critical | backlog | P1-001 |
| [P1-010](./phase-1/P1-010-shadcn-ui.md) | shadcn/ui Components Setup | high | backlog | P1-009 |
| [P1-011](./phase-1/P1-011-frontend-structure.md) | Frontend Base Structure | high | backlog | P1-010 |
| [P1-012](./phase-1/P1-012-api-client.md) | API Client Setup | high | backlog | P1-011 |
| [P1-013](./phase-1/P1-013-doc-as-code.md) | Doc-as-Code инфраструктура | high | backlog | P1-001, P1-003 |

### Граф зависимостей Phase 1

```
P1-001 (Monorepo)
├── P1-002 (Docker Compose)
│   ├── P1-006 (Gateway)
│   ├── P1-007 (PostgreSQL RLS)
│   └── P1-008 (RabbitMQ)
├── P1-003 (CI/CD)
│   └── P1-013 (Doc-as-Code)
├── P1-004 (Gradle)
│   └── P1-005 (Common Modules)
│       ├── P1-006 (Gateway)
│       └── P1-008 (RabbitMQ)
└── P1-009 (Next.js)
    └── P1-010 (shadcn/ui)
        └── P1-011 (Frontend Structure)
            └── P1-012 (API Client)
```

### Рекомендуемый порядок выполнения

1. **P1-001** — Монорепозиторий (блокирует всё)
2. **P1-002, P1-003, P1-004, P1-009** — можно параллельно
3. **P1-005, P1-007, P1-010, P1-013** — после зависимостей
4. **P1-006, P1-008, P1-011** — после зависимостей
5. **P1-012** — последняя (нужна вся инфраструктура)
