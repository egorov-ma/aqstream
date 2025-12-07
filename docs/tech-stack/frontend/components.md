# Frontend Components

Каталог UI компонентов AqStream.

## Структура компонентов

```
components/
├── ui/                   # shadcn/ui primitives (не трогаем)
├── forms/                # Формы
├── layout/               # Layout компоненты
└── features/             # Feature-specific компоненты
    ├── auth/
    ├── events/
    ├── organizations/
    └── registrations/
```

## UI Primitives (shadcn/ui)

Установленные компоненты из shadcn/ui:

| Компонент | Использование |
|-----------|--------------|
| Button | Кнопки |
| Card | Карточки |
| Dialog | Модальные окна |
| Form | Формы (обёртка над react-hook-form) |
| Input | Текстовые поля |
| Label | Лейблы |
| Select | Выпадающие списки |
| Table | Таблицы |
| Tabs | Табы |
| Toast | Уведомления |
| Badge | Бейджи |
| Avatar | Аватары |
| Dropdown Menu | Выпадающие меню |
| Sheet | Боковые панели |
| Skeleton | Загрузочные плейсхолдеры |

```bash
# Добавление нового компонента
pnpm dlx shadcn-ui@latest add [component-name]
```

## Layout Components

### Header

```tsx
// components/layout/header.tsx
import { UserNav } from './user-nav';
import { OrganizationSwitcher } from './organization-switcher';

export function Header() {
  return (
    <header className="border-b">
      <div className="flex h-16 items-center px-4">
        <OrganizationSwitcher />
        <div className="ml-auto flex items-center space-x-4">
          <UserNav />
        </div>
      </div>
    </header>
  );
}
```

### Sidebar

```tsx
// components/layout/sidebar.tsx
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { 
  CalendarDays, 
  Users, 
  Settings, 
  BarChart3 
} from 'lucide-react';

const navigation = [
  { name: 'События', href: '/events', icon: CalendarDays },
  { name: 'Регистрации', href: '/registrations', icon: Users },
  { name: 'Аналитика', href: '/analytics', icon: BarChart3 },
  { name: 'Настройки', href: '/settings', icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <div className="flex flex-col w-64 border-r bg-gray-50">
      <div className="p-4">
        <Link href="/" className="text-xl font-bold">
          AqStream
        </Link>
      </div>
      <nav className="flex-1 p-4 space-y-1">
        {navigation.map((item) => (
          <Link
            key={item.name}
            href={item.href}
            className={cn(
              'flex items-center px-3 py-2 rounded-md text-sm font-medium',
              pathname === item.href
                ? 'bg-gray-200 text-gray-900'
                : 'text-gray-600 hover:bg-gray-100'
            )}
          >
            <item.icon className="mr-3 h-5 w-5" />
            {item.name}
          </Link>
        ))}
      </nav>
    </div>
  );
}
```

## Form Components

### EventForm

```tsx
// components/forms/event-form.tsx
interface EventFormProps {
  onSubmit: (data: EventFormData) => void;
  defaultValues?: Partial<EventFormData>;
  isLoading?: boolean;
}

export function EventForm({ onSubmit, defaultValues, isLoading }: EventFormProps) {
  // ... implementation
}
```

### RegistrationForm

```tsx
// components/forms/registration-form.tsx
interface RegistrationFormProps {
  event: Event;
  ticketTypes: TicketType[];
  onSubmit: (data: RegistrationFormData) => void;
  isLoading?: boolean;
}

export function RegistrationForm({ 
  event, 
  ticketTypes, 
  onSubmit, 
  isLoading 
}: RegistrationFormProps) {
  // ... implementation
}
```

## Feature Components

### Events

```tsx
// components/features/events/event-card.tsx
export function EventCard({ event, onRegister }: EventCardProps) { }

// components/features/events/event-list.tsx
export function EventList({ events, isLoading }: EventListProps) { }

// components/features/events/event-status-badge.tsx
export function EventStatusBadge({ status }: { status: EventStatus }) { }

// components/features/events/ticket-type-selector.tsx
export function TicketTypeSelector({ 
  ticketTypes, 
  selectedId, 
  onSelect 
}: TicketTypeSelectorProps) { }
```

### Organizations

```tsx
// components/features/organizations/organization-switcher.tsx
export function OrganizationSwitcher() { }

// components/features/organizations/member-list.tsx
export function MemberList({ members, onRoleChange, onRemove }: MemberListProps) { }

// components/features/organizations/invite-member-dialog.tsx
export function InviteMemberDialog({ onInvite }: InviteMemberDialogProps) { }
```

### Registrations

```tsx
// components/features/registrations/registration-table.tsx
export function RegistrationTable({ 
  registrations, 
  onCheckIn 
}: RegistrationTableProps) { }

// components/features/registrations/check-in-scanner.tsx
export function CheckInScanner({ onScan }: CheckInScannerProps) { }

// components/features/registrations/ticket-preview.tsx
export function TicketPreview({ registration }: { registration: Registration }) { }
```

## Примеры использования

### Страница списка событий

```tsx
// app/(dashboard)/events/page.tsx
'use client';

import { useEvents } from '@/lib/hooks/use-events';
import { EventList } from '@/components/features/events/event-list';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { Plus } from 'lucide-react';

export default function EventsPage() {
  const { data, isLoading } = useEvents();

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">События</h1>
        <Button asChild>
          <Link href="/events/new">
            <Plus className="mr-2 h-4 w-4" />
            Создать событие
          </Link>
        </Button>
      </div>

      <EventList events={data?.data ?? []} isLoading={isLoading} />
    </div>
  );
}
```

### Страница создания события

```tsx
// app/(dashboard)/events/new/page.tsx
'use client';

import { useRouter } from 'next/navigation';
import { useCreateEvent } from '@/lib/hooks/use-events';
import { EventForm } from '@/components/forms/event-form';
import { useToast } from '@/components/ui/use-toast';

export default function NewEventPage() {
  const router = useRouter();
  const { toast } = useToast();
  const createEvent = useCreateEvent();

  const handleSubmit = async (data: EventFormData) => {
    try {
      const event = await createEvent.mutateAsync(data);
      toast({ title: 'Событие создано' });
      router.push(`/events/${event.id}`);
    } catch (error) {
      toast({ 
        title: 'Ошибка', 
        description: 'Не удалось создать событие',
        variant: 'destructive' 
      });
    }
  };

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold mb-6">Новое событие</h1>
      <EventForm 
        onSubmit={handleSubmit} 
        isLoading={createEvent.isPending} 
      />
    </div>
  );
}
```

## Правила создания компонентов

### Можно без согласования

- Композиции из shadcn/ui в `features/`
- Layout компоненты из Tailwind

### Требует согласования

- Новый UI primitive
- Кастомная стилизация, отличающаяся от shadcn/ui
- Сторонняя библиотека

### Стиль кастомных компонентов

```tsx
// Если согласовали кастомный компонент — следуем стилю shadcn/ui
import * as React from 'react';
import { cn } from '@/lib/utils';

export interface CustomComponentProps 
  extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'outline';
}

const CustomComponent = React.forwardRef<HTMLDivElement, CustomComponentProps>(
  ({ className, variant = 'default', ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          'base-classes',
          variant === 'outline' && 'outline-classes',
          className
        )}
        {...props}
      />
    );
  }
);
CustomComponent.displayName = 'CustomComponent';

export { CustomComponent };
```

## Дальнейшее чтение

- [Frontend Architecture](./architecture.md) — архитектура
- [Tech Stack Overview](../overview.md) — обзор технологий
