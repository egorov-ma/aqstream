# P1-009 Next.js 14 Project Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Frontend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Frontend AqStream — это SPA для организаторов и публичные страницы для участников:
- Dashboard для управления событиями
- Публичные страницы событий
- Форма регистрации
- Личный кабинет участника

### Технический контекст

Стек:
- Next.js 14 с App Router
- TypeScript 5 (strict mode)
- Tailwind CSS 3
- pnpm как package manager

App Router преимущества:
- Server Components для SEO
- Streaming и Suspense
- Route groups для организации
- Parallel routes для layouts

## Цель

Инициализировать Next.js 14 проект с базовой конфигурацией и структурой.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-001 завершена (структура репозитория)

## Acceptance Criteria

- [x] Next.js 14 проект создан в `frontend/`
- [x] TypeScript настроен в strict mode
- [x] Tailwind CSS 3 настроен
- [x] ESLint + Prettier настроены
- [x] Базовая структура директорий создана:
  - [x] `app/` с route groups
  - [x] `components/`
  - [x] `lib/`
  - [x] `types/`
- [x] `pnpm` используется как package manager
- [x] Базовый `layout.tsx` создан
- [x] Placeholder страницы созданы
- [x] `pnpm dev` запускает dev server
- [x] `pnpm build` успешно собирает проект
- [x] `pnpm lint` проходит без ошибок
- [x] Environment variables настроены
- [x] Dockerfile создан

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Dev server работает на localhost:3000
- [x] Production build успешен
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: полная инициализация
- [ ] Database: не затрагивается
- [x] Infrastructure: Dockerfile, docker-compose

### Структура файлов

```
frontend/
├── app/
│   ├── (auth)/           # Route group: login, register
│   ├── (dashboard)/      # Route group: events, settings
│   ├── (public)/         # Route group: public event pages
│   ├── layout.tsx        # Root layout
│   ├── page.tsx          # Home page
│   ├── error.tsx         # Error boundary
│   ├── loading.tsx       # Loading state
│   └── not-found.tsx     # 404 page
├── components/
│   ├── ui/               # shadcn/ui (P1-010)
│   ├── forms/
│   ├── layout/
│   └── features/
├── lib/
│   ├── api/              # API client (P1-012)
│   ├── hooks/
│   ├── store/
│   └── utils/
├── styles/
│   └── globals.css
├── types/
│   └── index.ts
└── public/
```

### Реализованные компоненты

Код реализован в следующих файлах:

| Компонент | Путь |
|-----------|------|
| Root Layout | `frontend/app/layout.tsx` |
| Auth Layout | `frontend/app/(auth)/layout.tsx` |
| Dashboard Layout | `frontend/app/(dashboard)/layout.tsx` |
| Public Layout | `frontend/app/(public)/layout.tsx` |
| Home Page | `frontend/app/page.tsx` |
| Login Page | `frontend/app/(auth)/login/page.tsx` |
| Register Page | `frontend/app/(auth)/register/page.tsx` |
| Events Page | `frontend/app/(dashboard)/events/page.tsx` |
| Settings Page | `frontend/app/(dashboard)/settings/page.tsx` |
| Event [slug] Page | `frontend/app/(public)/events/[slug]/page.tsx` |
| Error Boundary | `frontend/app/error.tsx` |
| Loading State | `frontend/app/loading.tsx` |
| 404 Page | `frontend/app/not-found.tsx` |
| cn() utility | `frontend/lib/utils/cn.ts` |
| Types | `frontend/types/index.ts` |
| Tailwind Config | `frontend/tailwind.config.ts` |
| TypeScript Config | `frontend/tsconfig.json` |
| ESLint Config | `frontend/.eslintrc.json` |
| Prettier Config | `frontend/.prettierrc` |
| Dockerfile | `frontend/Dockerfile` |
| Docker Compose | `docker-compose.yml` (frontend service) |

### NPM Scripts

| Команда | Описание |
|---------|----------|
| `pnpm dev` | Dev server на localhost:3000 |
| `pnpm build` | Production build |
| `pnpm start` | Production server |
| `pnpm lint` | ESLint проверка |
| `pnpm typecheck` | TypeScript проверка |
| `pnpm format` | Prettier форматирование |

## Зависимости

### Блокирует

- [P1-010] shadcn/ui components setup
- [P1-011] Frontend base structure
- [P1-012] API client setup

### Зависит от

- [P1-001] Настройка монорепозитория

## Out of Scope

- shadcn/ui компоненты (P1-010)
- API client и hooks (P1-012)
- State management (Zustand, TanStack Query)
- Аутентификация
- Реальные страницы (только placeholders)

## Заметки

- Inter font с поддержкой кириллицы
- Route groups `(auth)`, `(dashboard)`, `(public)` для разных layouts
- standalone output для оптимального Docker образа
- Tailwind CSS variables подготовлены для shadcn/ui
- TypeScript в strict mode
- `@/` alias для импортов
