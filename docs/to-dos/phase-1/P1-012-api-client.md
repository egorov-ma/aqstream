# P1-012 API Client Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
| Приоритет | `high` |
| Связь с roadmap | [Frontend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Frontend должен взаимодействовать с backend API для:
- Аутентификации пользователей
- Получения и отправки данных
- Обработки ошибок
- Показа состояний загрузки

### Технический контекст

Стек для работы с API:
- **Axios** — HTTP client
- **TanStack Query** — server state management
- **Zod** — runtime validation
- **TypeScript** — type safety

TanStack Query преимущества:
- Автоматическое кэширование
- Refetch при focus/reconnect
- Оптимистичные обновления
- Pagination и infinite scroll

## Цель

Настроить API client и infrastructure для взаимодействия с backend.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [ ] P1-011 завершена (frontend structure)

## Acceptance Criteria

- [ ] Axios client настроен с interceptors
- [ ] TanStack Query provider добавлен
- [ ] Базовые TypeScript types созданы
- [ ] API модули структурированы:
  - [ ] `lib/api/client.ts` — базовый клиент
  - [ ] `lib/api/auth.ts` — auth endpoints
  - [ ] `lib/api/events.ts` — events endpoints
- [ ] Custom hooks созданы:
  - [ ] `useAuth` — аутентификация
  - [ ] `useEvents` — работа с событиями
- [ ] Error handling настроен
- [ ] Loading states интегрированы
- [ ] Zustand store для auth state
- [ ] Types для API responses

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] API client корректно работает с mock данными
- [ ] TypeScript компилируется без ошибок
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: lib/api/, lib/hooks/, lib/store/
- [ ] Database: не затрагивается
- [ ] Infrastructure: не затрагивается

### Дополнительные зависимости

```bash
cd frontend

pnpm add axios @tanstack/react-query zustand
pnpm add -D @tanstack/react-query-devtools
```

### Структура файлов

```
lib/
├── api/
│   ├── client.ts           # Axios instance
│   ├── auth.ts             # Auth API
│   ├── events.ts           # Events API
│   ├── organizations.ts    # Organizations API
│   └── types.ts            # API types
├── hooks/
│   ├── use-auth.ts
│   ├── use-events.ts
│   └── use-organizations.ts
├── store/
│   ├── auth-store.ts
│   └── ui-store.ts
└── utils/
    └── cn.ts
```

### Axios Client

```typescript
// lib/api/client.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/lib/store/auth-store';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor — добавляем токен
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — обработка ошибок
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;

    // 401 — попытка refresh token
    if (error.response?.status === 401 && originalRequest) {
      const authStore = useAuthStore.getState();

      if (authStore.refreshToken) {
        try {
          const response = await axios.post(`${API_URL}/api/v1/auth/refresh`, {
            refreshToken: authStore.refreshToken,
          });

          const { accessToken, refreshToken } = response.data;
          authStore.setTokens(accessToken, refreshToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        } catch (refreshError) {
          authStore.logout();
          window.location.href = '/login';
        }
      } else {
        authStore.logout();
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);
```

### API Types

```typescript
// lib/api/types.ts

// Common
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, string>;
}

export interface PageResponse<T> {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// Auth
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

// User
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  createdAt: string;
}

// Organization
export interface Organization {
  id: string;
  name: string;
  slug: string;
  ownerId: string;
  createdAt: string;
}

// Event
export interface Event {
  id: string;
  title: string;
  description?: string;
  slug: string;
  status: EventStatus;
  startsAt: string;
  endsAt?: string;
  timezone: string;
  location?: string;
  createdAt: string;
  updatedAt: string;
}

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';

export interface CreateEventRequest {
  title: string;
  description?: string;
  startsAt: string;
  endsAt?: string;
  timezone: string;
  location?: string;
}

export interface UpdateEventRequest extends Partial<CreateEventRequest> {}
```

### Auth API

```typescript
// lib/api/auth.ts
import { apiClient } from './client';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  User,
} from './types';

export const authApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/login', data);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<User> => {
    const response = await apiClient.post<User>('/api/v1/auth/register', data);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/api/v1/auth/logout');
  },

  me: async (): Promise<User> => {
    const response = await apiClient.get<User>('/api/v1/users/me');
    return response.data;
  },

  refresh: async (refreshToken: string): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/refresh', {
      refreshToken,
    });
    return response.data;
  },
};
```

### Events API

```typescript
// lib/api/events.ts
import { apiClient } from './client';
import type {
  Event,
  CreateEventRequest,
  UpdateEventRequest,
  PageResponse,
} from './types';

export interface EventFilters {
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

export const eventsApi = {
  list: async (filters?: EventFilters): Promise<PageResponse<Event>> => {
    const response = await apiClient.get<PageResponse<Event>>('/api/v1/events', {
      params: filters,
    });
    return response.data;
  },

  getById: async (id: string): Promise<Event> => {
    const response = await apiClient.get<Event>(`/api/v1/events/${id}`);
    return response.data;
  },

  getBySlug: async (slug: string): Promise<Event> => {
    const response = await apiClient.get<Event>(`/api/v1/events/public/${slug}`);
    return response.data;
  },

  create: async (data: CreateEventRequest): Promise<Event> => {
    const response = await apiClient.post<Event>('/api/v1/events', data);
    return response.data;
  },

  update: async (id: string, data: UpdateEventRequest): Promise<Event> => {
    const response = await apiClient.put<Event>(`/api/v1/events/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/events/${id}`);
  },

  publish: async (id: string): Promise<Event> => {
    const response = await apiClient.post<Event>(`/api/v1/events/${id}/publish`);
    return response.data;
  },

  cancel: async (id: string): Promise<Event> => {
    const response = await apiClient.post<Event>(`/api/v1/events/${id}/cancel`);
    return response.data;
  },
};
```

### Auth Store (Zustand)

```typescript
// lib/store/auth-store.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '@/lib/api/types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  setUser: (user: User) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  login: (user: User, accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      setUser: (user) => set({ user }),

      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken }),

      login: (user, accessToken, refreshToken) =>
        set({
          user,
          accessToken,
          refreshToken,
          isAuthenticated: true,
        }),

      logout: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    }
  )
);
```

### Events Hooks

```typescript
// lib/hooks/use-events.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { eventsApi, type EventFilters } from '@/lib/api/events';
import type { CreateEventRequest, UpdateEventRequest } from '@/lib/api/types';
import { toast } from 'sonner';

export function useEvents(filters?: EventFilters) {
  return useQuery({
    queryKey: ['events', filters],
    queryFn: () => eventsApi.list(filters),
  });
}

export function useEvent(id: string) {
  return useQuery({
    queryKey: ['events', id],
    queryFn: () => eventsApi.getById(id),
    enabled: !!id,
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateEventRequest) => eventsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Событие создано');
    },
    onError: () => {
      toast.error('Ошибка при создании события');
    },
  });
}

export function useUpdateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateEventRequest }) =>
      eventsApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие обновлено');
    },
    onError: () => {
      toast.error('Ошибка при обновлении события');
    },
  });
}

export function usePublishEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.publish(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие опубликовано');
    },
    onError: () => {
      toast.error('Ошибка при публикации события');
    },
  });
}
```

### Query Provider

```typescript
// lib/providers/query-provider.tsx
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState, type ReactNode } from 'react';

export function QueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000, // 1 минута
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
```

### Root Layout Update

```tsx
// app/layout.tsx
import { QueryProvider } from '@/lib/providers/query-provider';
import { Toaster } from '@/components/ui/sonner';

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ru">
      <body className={inter.className}>
        <QueryProvider>
          {children}
          <Toaster />
        </QueryProvider>
      </body>
    </html>
  );
}
```

## Зависимости

### Блокирует

- Все задачи Phase 2 (используют API)

### Зависит от

- [P1-011] Frontend base structure

## Out of Scope

- Реальные API вызовы (backend ещё не готов)
- WebSocket connections
- File upload handling
- Offline support

## Заметки

- TanStack Query заменяет SWR и Redux для server state
- Zustand используется только для client state (auth, UI)
- Axios interceptors автоматически обрабатывают refresh token
- Toast уведомления при успехе/ошибке mutations
- DevTools включены только в development
- staleTime 1 минута — баланс между свежестью и производительностью
