# P2-017 Frontend: Создание и редактирование события

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Frontend](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Организатор создаёт событие через веб-интерфейс: заполняет информацию, настраивает типы билетов, публикует. Форма должна быть интуитивной и поддерживать все возможности Event Service.

### Технический контекст

- Многошаговая форма или табы
- React Hook Form + Zod
- TanStack Query для мутаций
- Markdown editor для описания

**Существующий код:**
- [events/page.tsx](../../../frontend/app/(dashboard)/dashboard/events/page.tsx) — список событий
- [events/new/page.tsx](../../../frontend/app/(dashboard)/dashboard/events/new/page.tsx) — создание

**Связанные документы:**
- [Event Service API](../../tech-stack/backend/services/event-service.md)
- [User Journeys - Journey 1](../../business/user-journeys.md#journey-1-создание-и-публикация-события)

## Цель

Реализовать полнофункциональные страницы создания, редактирования и управления событиями.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены
- [x] Нет блокеров

## Acceptance Criteria

### Список событий (/dashboard/events)

- [x] Таблица с событиями организации
- [x] Колонки: название, дата, статус, регистрации
- [x] Фильтры: статус, дата
- [x] Поиск по названию
- [x] Кнопка «Создать событие»
- [x] Actions: редактировать, опубликовать, отменить, удалить
- [x] Pagination

### Создание события (/dashboard/events/new)

- [x] Форма с основной информацией:
  - Название (обязательно)
  - Описание (Markdown)
  - Дата и время начала (обязательно)
  - Дата и время окончания (опционально)
  - Timezone
  - Тип локации (онлайн/офлайн/гибрид)
  - Адрес или URL
  - Обложка (загрузка изображения)
  - Видимость участников (CLOSED/OPEN)
  - Привязка к группе (опционально)
- [x] Типы билетов (название, количество, период продаж)
- [x] Предпросмотр
- [x] Кнопки: Сохранить черновик, Опубликовать

### Редактирование события (/dashboard/events/[id]/edit)

- [x] Загрузка существующих данных
- [x] Те же поля что и при создании
- [x] Ограничения для опубликованных событий
- [ ] История изменений (опционально) — out of scope

### Управление событием (/dashboard/events/[id])

- [x] Обзор события (статистика)
- [x] Список регистраций
- [x] Actions: редактировать, опубликовать/снять, отменить
- [x] При отмене — подтверждение и причина (UI готов, backend не поддерживает cancelReason)

### Типы билетов (inline)

- [x] Добавление типа билета на форме события
- [x] Редактирование существующих
- [x] Удаление (если нет регистраций)
- [x] Деактивация (если есть регистрации)
- [x] Drag-and-drop для сортировки

### Markdown Editor

- [x] Простой редактор для описания
- [x] Предпросмотр
- [x] Базовое форматирование (bold, italic, списки, ссылки)

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Используются только shadcn/ui компоненты
- [x] React Hook Form + Zod
- [x] TanStack Query mutations
- [x] Optimistic updates
- [x] Loading и error states
- [x] Code review пройден
- [x] CI/CD pipeline проходит

## Технические детали

### Структура файлов

```
frontend/app/(dashboard)/dashboard/events/
├── page.tsx                  — список событий
├── new/page.tsx              — создание
├── [id]/
│   ├── page.tsx              — детали события
│   └── edit/page.tsx         — редактирование

frontend/components/features/events/
├── event-list.tsx
├── event-form.tsx
├── event-card.tsx
├── ticket-type-form.tsx
├── ticket-type-list.tsx
└── event-status-badge.tsx
```

### Event Form Schema

```typescript
const eventFormSchema = z.object({
  title: z.string().min(1, 'Название обязательно').max(255),
  description: z.string().optional(),
  startsAt: z.string().datetime(),
  endsAt: z.string().datetime().optional(),
  timezone: z.string().default('Europe/Moscow'),
  locationType: z.enum(['ONLINE', 'OFFLINE', 'HYBRID']),
  locationAddress: z.string().optional(),
  locationUrl: z.string().url().optional(),
  groupId: z.string().uuid().optional(),
  participantsVisibility: z.enum(['CLOSED', 'OPEN']).default('CLOSED'),
  ticketTypes: z.array(z.object({
    name: z.string().min(1).max(100),
    description: z.string().optional(),
    quantity: z.number().min(1).optional(),
    salesStart: z.string().datetime().optional(),
    salesEnd: z.string().datetime().optional(),
  })).min(1, 'Добавьте хотя бы один тип билета'),
});
```

### useCreateEvent Hook

```typescript
export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateEventRequest) => {
      // 1. Создать событие
      const event = await eventApi.create(data);
      // 2. Создать типы билетов
      await Promise.all(
        data.ticketTypes.map(tt =>
          eventApi.createTicketType(event.id, tt)
        )
      );
      return event;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Событие создано');
    },
  });
}
```

## Зависимости

### Блокирует

- [P2-018](./P2-018-frontend-event-page.md) Публичная страница события

### Зависит от

- [P2-009](./P2-009-events-crud.md) Events API
- [P2-010](./P2-010-events-ticket-types.md) Ticket Types API
- [P2-016](./P2-016-frontend-dashboard.md) Dashboard layout

## Out of Scope

- Drag-and-drop builder для формы регистрации
- Recurring events
- Templates
- Bulk operations
- История изменений события (опционально)
- `cancelReason` при отмене события — требует доработки backend API

## Заметки

- Рассмотреть использование react-markdown для превью описания
- Date picker должен учитывать timezone
- При публикации — валидация что есть хотя бы один тип билета
- Изображение обложки загружается через Media Service
