# P1-011 Frontend Base Structure

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
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
- [ ] P1-010 завершена (shadcn/ui)

## Acceptance Criteria

- [ ] Layout components созданы:
  - [ ] `Header` с навигацией
  - [ ] `Sidebar` для dashboard
  - [ ] `Footer` (опционально)
- [ ] Route groups настроены:
  - [ ] `(auth)` — минимальный layout для auth страниц
  - [ ] `(dashboard)` — sidebar + header для dashboard
  - [ ] `(public)` — публичный layout для страниц событий
- [ ] Placeholder страницы созданы:
  - [ ] `/login` — страница входа
  - [ ] `/register` — страница регистрации
  - [ ] `/events` — список событий (dashboard)
  - [ ] `/events/[slug]` — публичная страница события
- [ ] Loading states (skeleton) для каждой группы
- [ ] Error boundaries для каждой группы
- [ ] Not found страницы
- [ ] Responsive design (mobile-first)
- [ ] Навигация между страницами работает

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Layouts корректно отображаются
- [ ] Навигация работает без перезагрузки страницы
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: layouts, pages, components
- [ ] Database: не затрагивается
- [ ] Infrastructure: не затрагивается

### Структура страниц

```
app/
├── (auth)/
│   ├── layout.tsx          # Минимальный layout
│   ├── login/
│   │   └── page.tsx
│   └── register/
│       └── page.tsx
├── (dashboard)/
│   ├── layout.tsx          # Sidebar + Header
│   ├── page.tsx            # Dashboard home (redirect to /events)
│   ├── events/
│   │   ├── page.tsx        # Список событий
│   │   ├── new/
│   │   │   └── page.tsx    # Создание события
│   │   └── [id]/
│   │       ├── page.tsx    # Детали события
│   │       └── edit/
│   │           └── page.tsx
│   ├── registrations/
│   │   └── page.tsx        # Список регистраций
│   └── settings/
│       └── page.tsx        # Настройки
├── (public)/
│   ├── layout.tsx          # Публичный layout
│   └── events/
│       └── [slug]/
│           └── page.tsx    # Публичная страница события
├── layout.tsx              # Root layout
├── page.tsx                # Landing page
├── error.tsx
├── loading.tsx
└── not-found.tsx

components/
├── layout/
│   ├── header.tsx
│   ├── sidebar.tsx
│   ├── sidebar-nav.tsx
│   ├── user-nav.tsx
│   └── mobile-nav.tsx
└── ...
```

### Header Component

```tsx
// components/layout/header.tsx
import Link from 'next/link';
import { UserNav } from './user-nav';
import { MobileNav } from './mobile-nav';

export function Header() {
  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 items-center">
        <div className="mr-4 hidden md:flex">
          <Link href="/" className="mr-6 flex items-center space-x-2">
            <span className="hidden font-bold sm:inline-block">
              AqStream
            </span>
          </Link>
        </div>

        <MobileNav />

        <div className="flex flex-1 items-center justify-end space-x-4">
          <UserNav />
        </div>
      </div>
    </header>
  );
}
```

### Sidebar Component

```tsx
// components/layout/sidebar.tsx
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import {
  CalendarDays,
  Users,
  Settings,
  BarChart3,
  LayoutDashboard,
} from 'lucide-react';

const navigation = [
  {
    name: 'Обзор',
    href: '/dashboard',
    icon: LayoutDashboard,
  },
  {
    name: 'События',
    href: '/events',
    icon: CalendarDays,
  },
  {
    name: 'Регистрации',
    href: '/registrations',
    icon: Users,
  },
  {
    name: 'Аналитика',
    href: '/analytics',
    icon: BarChart3,
  },
  {
    name: 'Настройки',
    href: '/settings',
    icon: Settings,
  },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <div className="hidden border-r bg-gray-100/40 lg:block">
      <div className="flex h-full max-h-screen flex-col gap-2">
        <div className="flex h-14 items-center border-b px-4 lg:h-[60px] lg:px-6">
          <Link href="/" className="flex items-center gap-2 font-semibold">
            <span>AqStream</span>
          </Link>
        </div>
        <div className="flex-1">
          <nav className="grid items-start px-2 text-sm font-medium lg:px-4">
            {navigation.map((item) => {
              const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={cn(
                    'flex items-center gap-3 rounded-lg px-3 py-2 transition-all hover:text-primary',
                    isActive
                      ? 'bg-muted text-primary'
                      : 'text-muted-foreground'
                  )}
                >
                  <item.icon className="h-4 w-4" />
                  {item.name}
                </Link>
              );
            })}
          </nav>
        </div>
      </div>
    </div>
  );
}
```

### Dashboard Layout

```tsx
// app/(dashboard)/layout.tsx
import { Header } from '@/components/layout/header';
import { Sidebar } from '@/components/layout/sidebar';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="grid min-h-screen w-full md:grid-cols-[220px_1fr] lg:grid-cols-[280px_1fr]">
      <Sidebar />
      <div className="flex flex-col">
        <Header />
        <main className="flex flex-1 flex-col gap-4 p-4 lg:gap-6 lg:p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
```

### Auth Layout

```tsx
// app/(auth)/layout.tsx
export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md">
        {children}
      </div>
    </div>
  );
}
```

### Public Layout

```tsx
// app/(public)/layout.tsx
import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b">
        <div className="container flex h-14 items-center justify-between">
          <Link href="/" className="font-bold">
            AqStream
          </Link>
          <div className="flex items-center gap-4">
            <Button variant="ghost" asChild>
              <Link href="/login">Войти</Link>
            </Button>
            <Button asChild>
              <Link href="/register">Регистрация</Link>
            </Button>
          </div>
        </div>
      </header>
      <main className="flex-1">
        {children}
      </main>
      <footer className="border-t py-6">
        <div className="container text-center text-sm text-muted-foreground">
          AqStream — платформа для управления мероприятиями
        </div>
      </footer>
    </div>
  );
}
```

### Login Page (placeholder)

```tsx
// app/(auth)/login/page.tsx
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function LoginPage() {
  return (
    <Card>
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl">Вход</CardTitle>
        <CardDescription>
          Введите email и пароль для входа в аккаунт
        </CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div className="grid gap-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" placeholder="name@example.com" />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="password">Пароль</Label>
          <Input id="password" type="password" />
        </div>
      </CardContent>
      <CardFooter className="flex flex-col gap-4">
        <Button className="w-full">Войти</Button>
        <p className="text-sm text-muted-foreground text-center">
          Нет аккаунта?{' '}
          <Link href="/register" className="text-primary hover:underline">
            Зарегистрироваться
          </Link>
        </p>
      </CardFooter>
    </Card>
  );
}
```

### Events List Page (placeholder)

```tsx
// app/(dashboard)/events/page.tsx
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { PlusCircle } from 'lucide-react';

export default function EventsPage() {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">События</h1>
        <Button asChild>
          <Link href="/events/new">
            <PlusCircle className="mr-2 h-4 w-4" />
            Создать событие
          </Link>
        </Button>
      </div>

      <div className="rounded-lg border p-8 text-center text-muted-foreground">
        <p>У вас пока нет событий.</p>
        <p className="mt-2">
          <Link href="/events/new" className="text-primary hover:underline">
            Создайте первое событие
          </Link>
        </p>
      </div>
    </div>
  );
}
```

### Loading State

```tsx
// app/(dashboard)/loading.tsx
import { Skeleton } from '@/components/ui/skeleton';

export default function DashboardLoading() {
  return (
    <div className="flex flex-col gap-4">
      <Skeleton className="h-8 w-48" />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-32 rounded-lg" />
        ))}
      </div>
    </div>
  );
}
```

### Error Boundary

```tsx
// app/(dashboard)/error.tsx
'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
      <h2 className="text-xl font-semibold">Что-то пошло не так</h2>
      <p className="text-muted-foreground">
        Произошла ошибка при загрузке страницы
      </p>
      <Button onClick={reset}>Попробовать снова</Button>
    </div>
  );
}
```

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
- Mobile navigation (можно добавить позже)

## Заметки

- Все компоненты layout используют shadcn/ui
- Sidebar использует `'use client'` для usePathname
- Layouts переиспользуются между страницами одной группы
- Lucide icons для иконок (устанавливается с shadcn/ui)
- Mobile-first подход в стилях
- Страницы — placeholders, реальная логика в Phase 2
