# P1-010 shadcn/ui Components Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
| Приоритет | `high` |
| Связь с roadmap | [Frontend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Консистентный UI критичен для user experience. shadcn/ui обеспечивает:
- Готовые, протестированные компоненты
- Доступность (a11y) из коробки
- Единый дизайн-язык
- Простую кастомизацию через Tailwind

### Технический контекст

shadcn/ui — **единственная** UI библиотека в проекте (архитектурное решение).

Особенности:
- Компоненты копируются в проект, а не устанавливаются как dependency
- Базируются на Radix UI primitives
- Стилизация через Tailwind CSS
- Полный контроль над кодом компонентов

## Цель

Установить и настроить shadcn/ui с базовым набором компонентов.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [x] P1-009 завершена (Next.js проект)

## Acceptance Criteria

- [x] shadcn/ui инициализирован (`components.json` создан)
- [x] CSS variables для темы в `globals.css`
- [x] Установлены базовые компоненты:
  - [x] Button
  - [x] Card
  - [x] Input
  - [x] Label
  - [x] Form (react-hook-form интеграция)
  - [x] Dialog
  - [x] Sheet
  - [x] Dropdown Menu
  - [x] Avatar
  - [x] Badge
  - [x] Skeleton
  - [x] Toast (Sonner)
  - [x] Table
  - [x] Tabs
  - [x] Select
- [x] Компоненты размещены в `components/ui/`
- [x] Утилита `cn()` работает
- [x] Toaster добавлен в root layout
- [x] ThemeProvider настроен (light/dark/system)

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] Компоненты рендерятся без ошибок
- [x] `pnpm typecheck` проходит
- [x] `pnpm lint` проходит
- [x] `pnpm build` успешен
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: components/ui/, styles, layout.tsx
- [ ] Database: не затрагивается
- [ ] Infrastructure: не затрагивается

### Реализованные компоненты

| Компонент | Путь |
|-----------|------|
| components.json | `frontend/components.json` |
| ThemeProvider | `frontend/components/theme-provider.tsx` |
| Button | `frontend/components/ui/button.tsx` |
| Card | `frontend/components/ui/card.tsx` |
| Input | `frontend/components/ui/input.tsx` |
| Label | `frontend/components/ui/label.tsx` |
| Form | `frontend/components/ui/form.tsx` |
| Dialog | `frontend/components/ui/dialog.tsx` |
| Sheet | `frontend/components/ui/sheet.tsx` |
| Dropdown Menu | `frontend/components/ui/dropdown-menu.tsx` |
| Avatar | `frontend/components/ui/avatar.tsx` |
| Badge | `frontend/components/ui/badge.tsx` |
| Skeleton | `frontend/components/ui/skeleton.tsx` |
| Sonner (Toast) | `frontend/components/ui/sonner.tsx` |
| Table | `frontend/components/ui/table.tsx` |
| Tabs | `frontend/components/ui/tabs.tsx` |
| Select | `frontend/components/ui/select.tsx` |

### Добавленные зависимости

| Зависимость | Назначение |
|-------------|-----------|
| `@radix-ui/react-*` | UI primitives |
| `@hookform/resolvers` | Form validation |
| `class-variance-authority` | Variant styles |
| `lucide-react` | Icons |
| `next-themes` | Темизация (light/dark/system) |
| `react-hook-form` | Forms |
| `sonner` | Toast notifications |
| `zod` | Schema validation |
| `tailwindcss-animate` | Animations |

## Зависимости

### Блокирует

- [P1-011] Frontend base structure (использует компоненты)
- Все UI задачи в Phase 2

### Зависит от

- [P1-009] Next.js 14 project setup

## Out of Scope

- Кастомные компоненты (будут в feature-задачах)
- Dark mode toggle UI (будет в P1-011)
- Анимации (framer-motion)
- Локализация компонентов

## Заметки

- shadcn/ui — **единственная** UI библиотека, никаких MUI, Chakra, Ant Design
- Компоненты копируются в проект — можно модифицировать
- Sonner используется вместо встроенного Toast (лучше UX)
- Form компонент интегрирован с react-hook-form и zod
- ThemeProvider в `app/layout.tsx` с поддержкой system/light/dark
- Sonner автоматически адаптируется к текущей теме
