# P2-021 Frontend: Add to Calendar Button

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `backlog` |
| Приоритет | `medium` |
| Связь с roadmap | [Roadmap - Frontend](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

После успешной регистрации на событие участник должен иметь возможность добавить событие в свой календарь (Google Calendar, Apple Calendar, Outlook) одним кликом. Это повышает вероятность посещения события.

### Технический контекст

- Компонент для generation календарных файлов (.ics)
- Интеграция с популярными календарями
- Отображение на success странице после регистрации

**Связанные документы:**
- [User Journeys - Journey 2](../../business/user-journeys.md#journey-2-регистрация-на-событие)
- [Frontend Registration Ticket Card](../../../frontend/components/features/public-event/registration-ticket-card.tsx)

## Цель

Реализовать компонент `AddToCalendarButton` для добавления зарегистрированного события в календарь пользователя.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [ ] Технические детали проработаны
- [ ] Зависимости определены
- [ ] Нет блокеров

## Acceptance Criteria

### Компонент AddToCalendarButton

- [ ] Кнопка "Добавить в календарь" на success странице
- [ ] Dropdown с выбором календаря:
  - [ ] Google Calendar
  - [ ] Apple Calendar (.ics файл)
  - [ ] Outlook (.ics файл)
- [ ] Generation .ics файла с:
  - [ ] Название события
  - [ ] Дата и время (старт, окончание)
  - [ ] Описание
  - [ ] Локация (если офлайн)
  - [ ] Confirmation code (в описании)

### UX

- [ ] Иконка календаря рядом с текстом
- [ ] Shadcn/ui DropdownMenu для выбора
- [ ] data-testid для E2E тестов
- [ ] Loading state во время generation
- [ ] Toast уведомление при успехе/ошибке

### Технические требования

- [ ] Использовать библиотеку для .ics generation (ics.js или аналог)
- [ ] Google Calendar redirect через URL scheme
- [ ] Download .ics для Apple/Outlook
- [ ] Timezone handling (UTC → локальная зона)
- [ ] Responsive design (работает на мобильных)

## Технические детали

### Структура компонента

```tsx
// frontend/components/features/public-event/add-to-calendar-button.tsx
interface AddToCalendarButtonProps {
  event: Event;
  confirmationCode?: string;
}

export function AddToCalendarButton({ event, confirmationCode }: AddToCalendarButtonProps) {
  // DropdownMenu с вариантами календарей
  // Google Calendar - redirect
  // Apple/Outlook - download .ics
}
```

### Библиотека для .ics

```bash
pnpm add ics
```

### Google Calendar URL

```typescript
const googleCalendarUrl = `https://calendar.google.com/calendar/render?action=TEMPLATE&text=${encodeURIComponent(event.title)}&dates=${startTime}/${endTime}&details=${encodeURIComponent(description)}&location=${encodeURIComponent(location)}`;
```

## Dependencies

### Зависимости

- P2-019 Frontend: Процесс регистрации (done)
- Frontend build system (Next.js)

### Блокирующие зависимости

Нет

## Definition of Done (DoD)

- [ ] Компонент реализован в `frontend/components/features/public-event/add-to-calendar-button.tsx`
- [ ] Раскомментирован импорт в `registration-ticket-card.tsx`
- [ ] Работают все 3 календаря (Google, Apple, Outlook)
- [ ] .ics файл корректного формата
- [ ] Timezone правильно обрабатывается
- [ ] Добавлены data-testid
- [ ] E2E тест для кнопки
- [ ] Responsive на всех размерах экрана
- [ ] Production build проходит без ошибок
- [ ] Code review пройден

## Риски и ограничения

### Риски

- **Timezone handling** - сложность с конвертацией в локальную зону
- **Browser compatibility** - download .ics может работать по-разному
- **.ics format** - разные календари могут требовать разные форматы

### Ограничения

- Нет поддержки recurring events (на данный момент не требуется)
- Нет напоминаний в .ics (можно добавить позже)

## Примечания

Компонент был заранее подготовлен в коде с TODO комментариями в [registration-ticket-card.tsx:15-16](../../../frontend/components/features/public-event/registration-ticket-card.tsx#L15-L16) и [registration-ticket-card.tsx:94-95](../../../frontend/components/features/public-event/registration-ticket-card.tsx#L94-L95).

После реализации нужно раскомментировать:
```tsx
// TODO: Restore when add-to-calendar-button is implemented
// import { AddToCalendarButton } from './add-to-calendar-button';
...
{/* TODO: Restore when add-to-calendar-button is implemented */}
{/* <AddToCalendarButton event={event} /> */}
```
