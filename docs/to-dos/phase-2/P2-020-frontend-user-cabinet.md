# P2-020 Frontend: Личный кабинет участника

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `done` |
| Приоритет | `high` |
| Связь с roadmap | [Roadmap - Frontend](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Личный кабинет участника показывает билеты на события, историю регистраций, настройки профиля. Это «домашняя страница» для обычных пользователей (не организаторов).

### Технический контекст

- Отдельный layout для участников или часть dashboard
- Показывает регистрации пользователя (из всех организаций)
- Настройки профиля и уведомлений

**Связанные документы:**
- [User Journeys - Journey 7](../../business/user-journeys.md#journey-7-участник-не-получил-билет)
- [Role Model](../../business/role-model.md)

## Цель

Реализовать личный кабинет для участников с билетами, историей и настройками.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены
- [x] Нет блокеров

## Acceptance Criteria

### Мои билеты

- [x] Список регистраций пользователя
- [x] Фильтры: предстоящие, прошедшие, отменённые
- [x] Карточка билета: событие, дата, тип билета, статус, confirmation code
- [x] QR-код для каждого билета
- [x] Кнопка «Отменить регистрацию» (если разрешено)
- [x] Кнопка «Повторно отправить билет» в Telegram

### Детали билета

- [x] Полная информация о событии
- [x] QR-код (крупный, для сканирования)
- [x] Confirmation code
- [x] Добавить в календарь
- [x] Поделиться

### Профиль

- [x] Просмотр и редактирование: firstName, lastName, email
- [x] Смена пароля (если email-аккаунт)
- [x] Привязка/отвязка Telegram
- [x] Аватар (загрузка)

### Настройки уведомлений

- [x] Toggle для типов уведомлений
- [x] Напоминания о событиях (on/off)
- [x] Обновления регистраций (on/off)

### Организации

- [x] Список организаций, где пользователь член
- [x] Для каждой: роль, кнопка перейти в dashboard
- [x] Кнопка «Создать организацию» → запрос

### Группы

- [x] Список групп, в которых состоит
- [x] Кнопка выхода из группы
- [x] Присоединение по инвайт-коду

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] Responsive design
- [x] Loading и error states
- [x] Тесты для критических функций
- [x] Code review пройден
- [x] CI/CD pipeline проходит

## Технические детали

### Структура

```
frontend/app/(dashboard)/
├── dashboard/              — организаторский dashboard
└── account/                — личный кабинет участника
    ├── page.tsx            — мои билеты
    ├── profile/page.tsx    — профиль
    ├── notifications/page.tsx — настройки уведомлений
    └── organizations/page.tsx — организации

frontend/components/features/account/
├── my-tickets.tsx
├── ticket-card.tsx
├── ticket-detail.tsx
├── profile-form.tsx
├── notification-settings.tsx
└── organization-list.tsx
```

### My Tickets

```tsx
'use client';

import { useMyRegistrations } from '@/lib/hooks/use-registrations';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { TicketCard } from './ticket-card';

export function MyTickets() {
  const { data: registrations, isLoading } = useMyRegistrations();

  const upcoming = registrations?.filter(r =>
    r.status === 'CONFIRMED' && new Date(r.event.startsAt) > new Date()
  );

  const past = registrations?.filter(r =>
    r.status === 'CHECKED_IN' || new Date(r.event.startsAt) < new Date()
  );

  const cancelled = registrations?.filter(r =>
    r.status === 'CANCELLED'
  );

  return (
    <Tabs defaultValue="upcoming">
      <TabsList>
        <TabsTrigger value="upcoming">
          Предстоящие ({upcoming?.length || 0})
        </TabsTrigger>
        <TabsTrigger value="past">
          Прошедшие ({past?.length || 0})
        </TabsTrigger>
        <TabsTrigger value="cancelled">
          Отменённые ({cancelled?.length || 0})
        </TabsTrigger>
      </TabsList>

      <TabsContent value="upcoming">
        <div className="grid gap-4 md:grid-cols-2">
          {upcoming?.map(reg => (
            <TicketCard key={reg.id} registration={reg} />
          ))}
        </div>
      </TabsContent>
      {/* ... */}
    </Tabs>
  );
}
```

### Ticket Card

```tsx
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { QRCodeSVG } from 'qrcode.react';

export function TicketCard({ registration }) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between">
        <div>
          <CardTitle>{registration.event.title}</CardTitle>
          <p className="text-sm text-muted-foreground">
            {formatDate(registration.event.startsAt)}
          </p>
        </div>
        <Badge variant={getStatusVariant(registration.status)}>
          {getStatusLabel(registration.status)}
        </Badge>
      </CardHeader>
      <CardContent className="flex gap-4">
        <QRCodeSVG
          value={`https://aqstream.ru/check-in/${registration.confirmationCode}`}
          size={80}
        />
        <div className="flex-1">
          <p className="text-sm">
            <span className="text-muted-foreground">Тип билета:</span>{' '}
            {registration.ticketType.name}
          </p>
          <p className="text-sm font-mono">
            Код: {registration.confirmationCode}
          </p>
        </div>
      </CardContent>
      <CardFooter className="gap-2">
        <Button variant="outline" size="sm" asChild>
          <Link href={`/account/tickets/${registration.id}`}>
            Подробнее
          </Link>
        </Button>
        <Button variant="ghost" size="sm" onClick={resendTicket}>
          Отправить в Telegram
        </Button>
      </CardFooter>
    </Card>
  );
}
```

### Navigation

Для пользователей с организациями:
- Dashboard — организаторский вид
- Мои билеты — участника вид

Для пользователей без организаций:
- Только личный кабинет

```tsx
// Определить какой вид показывать
const { user } = useAuthStore();
const { data: memberships } = useOrganizationMemberships();

const hasOrganizations = memberships && memberships.length > 0;

// В sidebar показывать разные меню
```

## Зависимости

### Блокирует

- Нет

### Зависит от

- [P2-011](./P2-011-registrations-crud.md) Registrations API
- [P2-015](./P2-015-frontend-auth-pages.md) Auth
- [P2-016](./P2-016-frontend-dashboard.md) Dashboard layout

## Out of Scope

- История платежей (Phase 3)
- Избранные события
- Подписка на организаторов
- Social features

## Заметки

- Личный кабинет доступен всем авторизованным пользователям
- Организаторы видят и dashboard и личный кабинет
- QR-код генерируется на клиенте через qrcode.react
- Рассмотреть добавление push notifications через Telegram
