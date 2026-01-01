# P2-018 Frontend: Публичная страница события

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `done` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Frontend](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Публичная страница события — витрина для участников. Показывает информацию о событии, доступные билеты, форму регистрации. Должна быть привлекательной и конвертировать посетителей в участников.

### Технический контекст

- Route group `(public)` — минимальный layout
- SEO: metadata, OG tags
- Server-side rendering для SEO
- Доступна без авторизации

**Существующий код:**
- [events/[slug]/page.tsx](../../../frontend/app/(public)/events/[slug]/page.tsx) — placeholder

**Связанные документы:**
- [User Journeys - Journey 2](../../business/user-journeys.md#journey-2-регистрация-на-событие)

## Цель

Реализовать привлекательную публичную страницу события с информацией и формой регистрации.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены
- [x] Нет блокеров

## Acceptance Criteria

### Информация о событии

- [x] Обложка (hero image)
- [x] Название
- [x] Дата и время (в timezone события и пользователя)
- [x] Место (адрес или онлайн)
- [x] Описание (Markdown rendered)
- [x] Организатор (название организации)

### Типы билетов

- [x] Список доступных типов
- [x] Для каждого: название, описание, доступность
- [x] «Распродан» badge если нет мест
- [x] «Скоро» если продажи не начались
- [x] «Закончились» если продажи завершились

### Регистрация

- [x] Выбор типа билета
- [x] Форма: first_name, last_name, email
- [x] Custom fields (если настроены)
- [x] Кнопка «Зарегистрироваться»
- [x] Если авторизован — поля заполнены автоматически
- [x] Если не авторизован — предложение войти или продолжить без входа

### Видимость участников

- [x] Если OPEN — показать список зарегистрированных
- [x] Распределение по типам билетов
- [x] Скрыть email (показать только имена)

### Состояния

- [x] Событие не опубликовано — 404
- [x] Событие отменено — сообщение об отмене
- [x] Событие завершено — «Событие завершилось»
- [x] Нет билетов — «Регистрация закрыта» + лист ожидания (Phase 3)

### SEO

- [x] Title: «{eventTitle} — AqStream»
- [x] Description из описания события
- [x] OG tags для sharing
- [x] Structured data (Event schema)

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Server-side rendering
- [x] Responsive design
- [x] Fast loading (< 2s)
- [x] Accessibility basics
- [x] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Структура

```
frontend/app/(public)/events/[slug]/
├── page.tsx              — страница события
├── not-found.tsx         — 404
└── loading.tsx           — skeleton

frontend/components/features/public-event/
├── event-hero.tsx
├── event-info.tsx
├── ticket-selector.tsx
├── registration-form.tsx
└── participants-list.tsx
```

### Server Component

```tsx
// app/(public)/events/[slug]/page.tsx
import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { eventApi } from '@/lib/api/events';

interface PageProps {
  params: { slug: string };
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const event = await eventApi.getBySlug(params.slug);
  if (!event) return {};

  return {
    title: `${event.title} — AqStream`,
    description: event.description?.slice(0, 160),
    openGraph: {
      title: event.title,
      description: event.description?.slice(0, 160),
      images: event.coverImageUrl ? [event.coverImageUrl] : [],
    },
  };
}

export default async function EventPage({ params }: PageProps) {
  const event = await eventApi.getBySlug(params.slug);

  if (!event || event.status === 'DRAFT') {
    notFound();
  }

  const ticketTypes = await eventApi.getTicketTypes(event.id);

  return (
    <main>
      <EventHero event={event} />
      <div className="container py-8">
        <div className="grid gap-8 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <EventInfo event={event} />
          </div>
          <div>
            <RegistrationCard
              event={event}
              ticketTypes={ticketTypes}
            />
          </div>
        </div>
      </div>
    </main>
  );
}
```

### Registration Form

```tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateRegistration } from '@/lib/hooks/use-registrations';
import { useAuthStore } from '@/lib/store/auth-store';

const registrationSchema = z.object({
  ticketTypeId: z.string().uuid(),
  firstName: z.string().min(1, 'Имя обязательно'),
  lastName: z.string().min(1, 'Фамилия обязательна'),
  email: z.string().email('Некорректный email'),
});

export function RegistrationForm({ eventId, ticketTypes }) {
  const { user } = useAuthStore();
  const mutation = useCreateRegistration(eventId);

  const form = useForm({
    resolver: zodResolver(registrationSchema),
    defaultValues: {
      ticketTypeId: ticketTypes[0]?.id,
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.email || '',
    },
  });

  async function onSubmit(data) {
    await mutation.mutateAsync(data);
    // Redirect to success page or show ticket
  }

  return (
    <Form {...form}>
      {/* Ticket type selector */}
      {/* Name fields */}
      {/* Email field */}
      <Button type="submit" disabled={mutation.isPending}>
        Зарегистрироваться
      </Button>
    </Form>
  );
}
```

## Зависимости

### Блокирует

- [P2-019](./P2-019-frontend-registration.md) Процесс регистрации

### Зависит от

- [P2-009](./P2-009-events-crud.md) Events API
- [P2-010](./P2-010-events-ticket-types.md) Ticket Types API

## Out of Scope

- Comments / Q&A
- Social sharing buttons
- Event recommendations
- Лист ожидания (Phase 3)

## Заметки

- URL формат: /events/{slug} — SEO friendly
- Рассмотреть добавление countdown для событий
- Lazy load registration form (не критично для SEO)
- Timezone: показывать в timezone события + local
