# P1-011 Frontend Base Structure

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `high` |
| Связь с roadmap | [Frontend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Базовая структура frontend определяет:
- Организацию кода и компонентов
- Layouts для разных типов страниц
- Навигацию между разделами
- Общие UI паттерны

### Технический контекст

Next.js App Router структура:
- Route groups для разных layouts
- Server Components по умолчанию
- Client Components где нужна интерактивность
- Shared layouts для переиспользования

## Цель

Создать базовую структуру страниц и layouts для frontend приложения.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-010 завершена (shadcn/ui)

## Acceptance Criteria

- [x] Layout components созданы:
  - [x] `Header` с навигацией
  - [x] `Sidebar` для dashboard
  - [x] `Footer` (в public layout)
- [x] Route groups настроены:
  - [x] `(auth)` — минимальный layout для auth страниц
  - [x] `(dashboard)` — sidebar + header для dashboard
  - [x] `(public)` — публичный layout для страниц событий
- [x] Placeholder страницы созданы:
  - [x] `/login` — страница входа
  - [x] `/register` — страница регистрации
  - [x] `/dashboard/events` — список событий (dashboard)
  - [x] `/events/[slug]` — публичная страница события
- [x] Loading states (skeleton) для каждой группы
- [x] Error boundaries для каждой группы
- [x] Not found страницы
- [x] Responsive design (mobile-first)
- [x] Навигация между страницами работает

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Layouts корректно отображаются
- [x] Навигация работает без перезагрузки страницы
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: layouts, pages, components
- [ ] Database: не затрагивается
- [ ] Infrastructure: не затрагивается

### Реализованные layout компоненты

| Компонент | Путь | Описание |
|-----------|------|----------|
| Header | `frontend/components/layout/header.tsx` | Шапка с mobile nav и user nav |
| Sidebar | `frontend/components/layout/sidebar.tsx` | Боковая панель dashboard |
| SidebarNav | `frontend/components/layout/sidebar-nav.tsx` | Навигация с иконками |
| UserNav | `frontend/components/layout/user-nav.tsx` | Dropdown меню пользователя |
| MobileNav | `frontend/components/layout/mobile-nav.tsx` | Мобильная навигация (Sheet) |

### Реализованные layouts

| Layout | Путь | Описание |
|--------|------|----------|
| Root | `frontend/app/layout.tsx` | ThemeProvider + Toaster |
| Auth | `frontend/app/(auth)/layout.tsx` | Центрированная карточка |
| Dashboard | `frontend/app/(dashboard)/layout.tsx` | Sidebar + Header |
| Public | `frontend/app/(public)/layout.tsx` | Header + Footer |

### Реализованные страницы

| Маршрут | Путь |
|---------|------|
| `/` | `frontend/app/page.tsx` |
| `/login` | `frontend/app/(auth)/login/page.tsx` |
| `/register` | `frontend/app/(auth)/register/page.tsx` |
| `/dashboard` | `frontend/app/(dashboard)/dashboard/page.tsx` |
| `/dashboard/events` | `frontend/app/(dashboard)/dashboard/events/page.tsx` |
| `/dashboard/events/new` | `frontend/app/(dashboard)/dashboard/events/new/page.tsx` |
| `/dashboard/events/[id]` | `frontend/app/(dashboard)/dashboard/events/[id]/page.tsx` |
| `/dashboard/events/[id]/edit` | `frontend/app/(dashboard)/dashboard/events/[id]/edit/page.tsx` |
| `/dashboard/registrations` | `frontend/app/(dashboard)/dashboard/registrations/page.tsx` |
| `/dashboard/analytics` | `frontend/app/(dashboard)/dashboard/analytics/page.tsx` |
| `/dashboard/settings` | `frontend/app/(dashboard)/dashboard/settings/page.tsx` |
| `/events/[slug]` | `frontend/app/(public)/events/[slug]/page.tsx` |

### Loading / Error / Not Found

| Тип | Файлы |
|-----|-------|
| Loading | `app/loading.tsx`, `app/(auth)/loading.tsx`, `app/(dashboard)/loading.tsx`, `app/(public)/loading.tsx` |
| Error | `app/error.tsx`, `app/(auth)/error.tsx`, `app/(dashboard)/error.tsx`, `app/(public)/error.tsx` |
| Not Found | `app/not-found.tsx`, `app/(public)/events/[slug]/not-found.tsx` |

## Зависимости

### Блокирует

- [P1-012] API client setup
- Все страницы в Phase 2

### Зависит от

- [P1-009] Next.js project
- [P1-010] shadcn/ui components

## Out of Scope

- Аутентификация (логика входа)
- API вызовы
- State management
- Реальные данные на страницах

## Заметки

- Все компоненты layout используют shadcn/ui
- SidebarNav использует `'use client'` для usePathname
- MobileNav использует Sheet из shadcn/ui
- UserNav использует DropdownMenu из shadcn/ui
- Layouts переиспользуются между страницами одной группы
- Lucide icons для иконок
- Mobile-first подход в стилях (lg:hidden, md:grid-cols)
- Страницы — placeholders, реальная логика в Phase 2
