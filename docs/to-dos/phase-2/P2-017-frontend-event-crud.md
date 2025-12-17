# P2-017 Frontend: Создание и редактирование события

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
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

- [ ] Таблица с событиями организации
- [ ] Колонки: название, дата, статус, регистрации
- [ ] Фильтры: статус, дата
- [ ] Поиск по названию
- [ ] Кнопка «Создать событие»
- [ ] Actions: редактировать, опубликовать, отменить, удалить
- [ ] Pagination

### Создание события (/dashboard/events/new)

- [ ] Форма с основной информацией:
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
- [ ] Типы билетов (название, количество, период продаж)
- [ ] Предпросмотр
- [ ] Кнопки: Сохранить черновик, Опубликовать

### Редактирование события (/dashboard/events/[id]/edit)

- [ ] Загрузка существующих данных
- [ ] Те же поля что и при создании
- [ ] Ограничения для опубликованных событий
- [ ] История изменений (опционально)

### Управление событием (/dashboard/events/[id])

- [ ] Обзор события (статистика)
- [ ] Список регистраций
- [ ] Actions: редактировать, опубликовать/снять, отменить
- [ ] При отмене — подтверждение и причина

### Типы билетов (inline)

- [ ] Добавление типа билета на форме события
- [ ] Редактирование существующих
- [ ] Удаление (если нет регистраций)
- [ ] Деактивация (если есть регистрации)
- [ ] Drag-and-drop для сортировки

### Markdown Editor

- [ ] Простой редактор для описания
- [ ] Предпросмотр
- [ ] Базовое форматирование (bold, italic, списки, ссылки)

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Используются только shadcn/ui компоненты
- [ ] React Hook Form + Zod
- [ ] TanStack Query mutations
- [ ] Optimistic updates
- [ ] Loading и error states
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

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

## Заметки

- Рассмотреть использование react-markdown для превью описания
- Date picker должен учитывать timezone
- При публикации — валидация что есть хотя бы один тип билета
- Изображение обложки загружается через Media Service
