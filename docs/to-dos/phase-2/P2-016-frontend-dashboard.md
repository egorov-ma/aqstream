# P2-016 Frontend: Dashboard организатора

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Frontend](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Dashboard — главная страница организатора после входа. Показывает обзор организации: события, статистику, быстрые действия. Также включает переключатель организаций и навигацию.

### Технический контекст

- Route group `(dashboard)` — layout с sidebar и header
- Protected routes (требуют авторизации)
- TanStack Query для загрузки данных
- Zustand для текущей организации

**Существующий код:**
- [dashboard/page.tsx](../../../frontend/app/(dashboard)/dashboard/page.tsx) — placeholder с карточками
- [(dashboard)/layout.tsx](../../../frontend/app/(dashboard)/layout.tsx) — layout

**Связанные документы:**
- [Frontend Architecture](../../tech-stack/frontend/architecture.md)
- [User Journeys](../../business/user-journeys.md)

## Цель

Реализовать полнофункциональный dashboard организатора с навигацией, статистикой и быстрыми действиями.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены
- [x] Нет блокеров

## Acceptance Criteria

### Layout

- [ ] Sidebar с навигацией
- [ ] Header с user menu
- [ ] Переключатель организаций
- [ ] Mobile-responsive (sheet для sidebar)
- [ ] Тёмная/светлая тема (toggle)

### Навигация (Sidebar)

- [ ] Обзор (/dashboard)
- [ ] События (/dashboard/events)
- [ ] Регистрации (/dashboard/registrations)
- [ ] Аналитика (/dashboard/analytics)
- [ ] Настройки (/dashboard/settings)

### User Menu (Header)

- [ ] Имя и аватар пользователя
- [ ] Dropdown: профиль, настройки, выход
- [ ] Переключатель организаций (если несколько)
- [ ] Уведомления (badge с количеством)

### Обзор (/dashboard)

- [ ] Карточки статистики: активные события, регистрации за 30 дней, посещаемость
- [ ] Список ближайших событий
- [ ] Быстрые действия: создать событие, просмотреть регистрации
- [ ] Данные загружаются через API

### Переключатель организаций

- [ ] Если пользователь в нескольких организациях — dropdown для выбора
- [ ] При переключении — обновляется token с новым tenantId
- [ ] Данные dashboard обновляются

### Protected Routes

- [ ] Middleware проверяет авторизацию
- [ ] Без токена — redirect на /login
- [ ] При истечении токена — автоматический refresh

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Используются только shadcn/ui компоненты
- [ ] TanStack Query для загрузки данных
- [ ] Responsive design
- [ ] Loading и error states
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [x] Frontend: `app/(dashboard)/*`, `components/layout/*`
- [ ] Backend: —
- [ ] Database: —
- [ ] Infrastructure: —

### Структура файлов

```
frontend/
├── app/
│   └── (dashboard)/
│       ├── layout.tsx              — dashboard layout
│       └── dashboard/
│           ├── page.tsx            — обзор
│           ├── events/             — события (P2-017)
│           ├── registrations/      — регистрации
│           ├── analytics/          — аналитика
│           └── settings/           — настройки
├── components/
│   └── layout/
│       ├── sidebar.tsx
│       ├── header.tsx
│       ├── user-menu.tsx
│       ├── organization-switcher.tsx
│       └── nav-link.tsx
└── lib/
    └── store/
        └── organization-store.ts
```

### Dashboard Layout

```tsx
// app/(dashboard)/layout.tsx
import { Sidebar } from '@/components/layout/sidebar';
import { Header } from '@/components/layout/header';
import { AuthGuard } from '@/components/auth/auth-guard';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <div className="flex min-h-screen">
        <Sidebar />
        <div className="flex-1 flex flex-col">
          <Header />
          <main className="flex-1 p-6">{children}</main>
        </div>
      </div>
    </AuthGuard>
  );
}
```

### Sidebar

```tsx
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { LayoutDashboard, Calendar, Users, BarChart3, Settings } from 'lucide-react';

const navItems = [
  { href: '/dashboard', icon: LayoutDashboard, label: 'Обзор' },
  { href: '/dashboard/events', icon: Calendar, label: 'События' },
  { href: '/dashboard/registrations', icon: Users, label: 'Регистрации' },
  { href: '/dashboard/analytics', icon: BarChart3, label: 'Аналитика' },
  { href: '/dashboard/settings', icon: Settings, label: 'Настройки' },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 border-r bg-background">
      <div className="p-4">
        <h1 className="text-xl font-bold">AqStream</h1>
      </div>
      <nav className="space-y-1 px-2">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors',
              pathname === item.href
                ? 'bg-primary text-primary-foreground'
                : 'hover:bg-accent'
            )}
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
```

### Organization Switcher

```tsx
'use client';

import { useOrganizationStore } from '@/lib/store/organization-store';
import { useOrganizations } from '@/lib/hooks/use-organizations';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Button } from '@/components/ui/button';
import { Building, ChevronDown } from 'lucide-react';

export function OrganizationSwitcher() {
  const { currentOrganization, setCurrentOrganization } = useOrganizationStore();
  const { data: organizations } = useOrganizations();

  if (!organizations || organizations.length <= 1) return null;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="gap-2">
          <Building className="h-4 w-4" />
          {currentOrganization?.name || 'Выбрать организацию'}
          <ChevronDown className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        {organizations.map((org) => (
          <DropdownMenuItem
            key={org.id}
            onClick={() => setCurrentOrganization(org)}
          >
            {org.name}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
```

## Зависимости

### Блокирует

- [P2-017](./P2-017-frontend-event-crud.md) CRUD событий
- [P2-020](./P2-020-frontend-user-cabinet.md) Личный кабинет

### Зависит от

- [P2-015](./P2-015-frontend-auth-pages.md) Auth pages (авторизация)
- [P2-006](./P2-006-organizations-crud.md) Organizations API

## Out of Scope

- Real-time обновления (WebSocket)
- Notification center (полный)
- Onboarding wizard

## Заметки

- Существующие placeholder карточки можно использовать как основу
- При первом входе без организации — показать wizard создания
- Mobile: sidebar как Sheet (выезжает слева)
- Тёмная тема через next-themes (уже настроено)
