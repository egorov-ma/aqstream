# Frontend Architecture

Архитектура frontend приложения AqStream.

## Технологии

| Компонент | Технология |
|-----------|-----------|
| Framework | Next.js 14 (App Router) |
| Language | TypeScript 5 |
| Styling | Tailwind CSS 3 |
| UI Components | shadcn/ui |
| Theming | next-themes |
| Server State | TanStack Query 5 |
| Client State | Zustand 4 |
| Forms | React Hook Form + Zod |
| HTTP Client | Axios |

## Структура проекта

```text
frontend/
├── app/                      # Next.js App Router
│   ├── (auth)/               # Auth pages (login, register)
│   │   ├── login/
│   │   └── register/
│   ├── (dashboard)/          # Protected pages
│   │   └── dashboard/        # /dashboard/*
│   │       ├── events/
│   │       ├── registrations/
│   │       ├── analytics/
│   │       └── settings/
│   ├── (public)/             # Public pages
│   │   └── events/[slug]/
│   ├── layout.tsx
│   └── page.tsx
├── components/
│   ├── ui/                   # shadcn/ui primitives
│   ├── layout/               # Layout components (Header, Sidebar, etc.)
│   ├── forms/                # Form compositions
│   └── features/             # Feature components
├── lib/
│   ├── api/                  # API client
│   ├── hooks/                # Custom hooks
│   ├── store/                # Zustand stores
│   └── utils/                # Utilities
├── types/                    # TypeScript types
└── styles/                   # Global styles
```

## Routing (App Router)

### Route Groups

```text
app/
├── (auth)/                   # Без layout dashboard
│   ├── layout.tsx            # Auth layout (минимальный)
│   ├── login/page.tsx        # /login
│   └── register/page.tsx     # /register
├── (dashboard)/              # С sidebar и header
│   ├── layout.tsx            # Dashboard layout
│   └── dashboard/
│       ├── page.tsx          # /dashboard
│       ├── events/
│       │   ├── page.tsx      # /dashboard/events
│       │   ├── new/page.tsx  # /dashboard/events/new
│       │   └── [id]/page.tsx # /dashboard/events/:id
│       ├── registrations/
│       │   └── page.tsx      # /dashboard/registrations
│       └── settings/
│           └── page.tsx      # /dashboard/settings
└── (public)/                 # Публичные страницы
    └── events/
        └── [slug]/page.tsx   # /events/:slug (public event page)
```

### Layouts

```tsx
// app/(dashboard)/layout.tsx
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex-1">
        <Header />
        <main className="p-6">{children}</main>
      </div>
    </div>
  );
}
```

## UI Components

### shadcn/ui — единственная UI библиотека

```bash
# Установка компонентов
pnpm dlx shadcn-ui@latest add button
pnpm dlx shadcn-ui@latest add card
pnpm dlx shadcn-ui@latest add dialog
pnpm dlx shadcn-ui@latest add form
pnpm dlx shadcn-ui@latest add input
pnpm dlx shadcn-ui@latest add table
```

```tsx
// ✅ ПРАВИЛЬНО
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

// ❌ ЗАПРЕЩЕНО
import { Button } from '@chakra-ui/react';
import { Button } from '@mui/material';
```

## Темизация (Theming)

Приложение поддерживает светлую и тёмную темы через `next-themes`.

### Конфигурация

```tsx
// app/layout.tsx
import { ThemeProvider } from '@/components/theme-provider';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru" suppressHydrationWarning>
      <body>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
```

### Использование темы

```tsx
'use client';

import { useTheme } from 'next-themes';

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  return (
    <button onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
      Переключить тему
    </button>
  );
}
```

### Доступные темы

| Тема | Описание |
|------|----------|
| `light` | Светлая тема |
| `dark` | Тёмная тема |
| `system` | Системная (по умолчанию) |

### CSS Variables

Цвета определены в `styles/globals.css` через CSS variables:

```css
:root {
  --background: 0 0% 100%;
  --foreground: 222.2 84% 4.9%;
  /* ... */
}

.dark {
  --background: 222.2 84% 4.9%;
  --foreground: 210 40% 98%;
  /* ... */
}
```

shadcn/ui компоненты автоматически используют эти переменные.

### Feature Components

Композиции из shadcn/ui:

```tsx
// components/features/events/event-card.tsx
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

interface EventCardProps {
  event: Event;
  onRegister?: (id: string) => void;
}

export function EventCard({ event, onRegister }: EventCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex justify-between items-start">
          <CardTitle>{event.title}</CardTitle>
          <Badge variant={event.status === 'PUBLISHED' ? 'default' : 'secondary'}>
            {event.status}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-4">
          {event.description}
        </p>
        <Button onClick={() => onRegister?.(event.id)}>
          Зарегистрироваться
        </Button>
      </CardContent>
    </Card>
  );
}
```

## State Management

### Server State (TanStack Query)

```tsx
// lib/hooks/use-events.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { eventApi } from '@/lib/api/event-api';

export function useEvents(filters?: EventFilters) {
  return useQuery({
    queryKey: ['events', filters],
    queryFn: () => eventApi.list(filters),
  });
}

export function useEvent(id: string) {
  return useQuery({
    queryKey: ['events', id],
    queryFn: () => eventApi.getById(id),
    enabled: !!id,
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: eventApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
    },
  });
}
```

### Client State (Zustand)

```tsx
// lib/store/auth-store.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  setAuth: (user: User, token: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      setAuth: (user, accessToken) => set({ user, accessToken }),
      logout: () => set({ user: null, accessToken: null }),
    }),
    { name: 'auth-storage' }
  )
);
```

```tsx
// lib/store/organization-store.ts
import { create } from 'zustand';

interface OrganizationState {
  currentOrganization: Organization | null;
  setCurrentOrganization: (org: Organization) => void;
}

export const useOrganizationStore = create<OrganizationState>((set) => ({
  currentOrganization: null,
  setCurrentOrganization: (org) => set({ currentOrganization: org }),
}));
```

## Forms

```tsx
// components/forms/event-form.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';

const eventSchema = z.object({
  title: z.string().min(1, 'Название обязательно').max(255),
  description: z.string().optional(),
  startsAt: z.string().datetime(),
  timezone: z.string(),
});

type EventFormData = z.infer<typeof eventSchema>;

interface EventFormProps {
  onSubmit: (data: EventFormData) => void;
  defaultValues?: Partial<EventFormData>;
  isLoading?: boolean;
}

export function EventForm({ onSubmit, defaultValues, isLoading }: EventFormProps) {
  const form = useForm<EventFormData>({
    resolver: zodResolver(eventSchema),
    defaultValues,
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="title"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Название</FormLabel>
              <FormControl>
                <Input placeholder="Tech Meetup" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        
        {/* ... other fields */}
        
        <Button type="submit" disabled={isLoading}>
          {isLoading ? 'Сохранение...' : 'Сохранить'}
        </Button>
      </form>
    </Form>
  );
}
```

## API Client

```tsx
// lib/api/client.ts
import axios from 'axios';
import { useAuthStore } from '@/lib/store/auth-store';

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

```tsx
// lib/api/event-api.ts
import { apiClient } from './client';

export const eventApi = {
  list: async (filters?: EventFilters) => {
    const { data } = await apiClient.get<PageResponse<Event>>('/api/v1/events', {
      params: filters,
    });
    return data;
  },
  
  getById: async (id: string) => {
    const { data } = await apiClient.get<Event>(`/api/v1/events/${id}`);
    return data;
  },
  
  create: async (request: CreateEventRequest) => {
    const { data } = await apiClient.post<Event>('/api/v1/events', request);
    return data;
  },
  
  update: async (id: string, request: UpdateEventRequest) => {
    const { data } = await apiClient.put<Event>(`/api/v1/events/${id}`, request);
    return data;
  },
  
  publish: async (id: string) => {
    const { data } = await apiClient.post<Event>(`/api/v1/events/${id}/publish`);
    return data;
  },
};
```

## Error Handling

```tsx
// components/error-boundary.tsx
'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';

export default function Error({
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
    <div className="flex flex-col items-center justify-center min-h-screen">
      <h2 className="text-xl font-semibold mb-4">Что-то пошло не так</h2>
      <Button onClick={reset}>Попробовать снова</Button>
    </div>
  );
}
```

## Code Style

```tsx
// ✅ Named exports
export function EventCard() { }

// ✅ Explicit types
interface EventCardProps {
  event: Event;
  onRegister?: (id: string) => void;
}

// ✅ TypeScript strict mode
// tsconfig.json: "strict": true

// ❌ Default exports (кроме pages)
export default function EventCard() { }

// ❌ any
function handleEvent(data: any) { }
```

## Дальнейшее чтение

- [Components](./components.md) — список компонентов
- [Tech Stack Overview](../overview.md) — обзор технологий
