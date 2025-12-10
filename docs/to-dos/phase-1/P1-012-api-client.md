# P1-012 API Client Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `done` |
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
- **Zustand** — client state management
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
- [x] P1-011 завершена (frontend structure)

## Acceptance Criteria

- [x] Axios client настроен с interceptors
- [x] TanStack Query provider добавлен
- [x] Базовые TypeScript types созданы
- [x] API модули структурированы:
  - [x] `lib/api/client.ts` — базовый клиент
  - [x] `lib/api/auth.ts` — auth endpoints
  - [x] `lib/api/events.ts` — events endpoints
- [x] Custom hooks созданы:
  - [x] `useAuth` — аутентификация
  - [x] `useEvents` — работа с событиями
- [x] Error handling настроен
- [x] Loading states интегрированы
- [x] Zustand store для auth state
- [x] Types для API responses

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [x] Все Acceptance Criteria выполнены
- [x] API client корректно работает с mock данными
- [x] TypeScript компилируется без ошибок
- [x] Code review пройден
- [x] CI pipeline проходит
- [x] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: lib/api/, lib/hooks/, lib/store/, lib/providers/
- [ ] Database: не затрагивается
- [ ] Infrastructure: не затрагивается

### Добавленные зависимости

| Зависимость | Назначение |
|-------------|-----------|
| `axios` | HTTP client |
| `@tanstack/react-query` | Server state management |
| `zustand` | Client state management |
| `@tanstack/react-query-devtools` | DevTools (dev only) |

### Реализованные файлы

| Файл | Описание |
|------|----------|
| `lib/api/types.ts` | TypeScript типы для API |
| `lib/api/client.ts` | Axios instance с interceptors |
| `lib/api/auth.ts` | Auth API (login, register, logout, me) |
| `lib/api/events.ts` | Events API (CRUD, publish, cancel) |
| `lib/store/auth-store.ts` | Zustand store для auth state |
| `lib/store/ui-store.ts` | Zustand store для UI state |
| `lib/providers/query-provider.tsx` | TanStack Query provider |
| `lib/hooks/use-auth.ts` | Hooks для аутентификации |
| `lib/hooks/use-events.ts` | Hooks для работы с событиями |

### Структура lib/

```
lib/
├── api/
│   ├── client.ts
│   ├── auth.ts
│   ├── events.ts
│   └── types.ts
├── hooks/
│   ├── use-auth.ts
│   └── use-events.ts
├── store/
│   ├── auth-store.ts
│   └── ui-store.ts
├── providers/
│   └── query-provider.tsx
└── utils.ts
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
