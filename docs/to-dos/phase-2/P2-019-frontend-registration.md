# P2-019 Frontend: Процесс регистрации

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `done` |
| Приоритет | `high` |
| Связь с roadmap | [Roadmap - Frontend](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

После заполнения формы регистрации участник видит подтверждение и получает билет в Telegram. Процесс должен быть простым и давать уверенность в успешной регистрации.

### Технический контекст

- Форма на публичной странице события
- Mutation через TanStack Query
- Redirect на success page
- Интеграция с Notification Service для отправки билета

**Связанные документы:**
- [User Journeys - Journey 2](../../business/user-journeys.md#journey-2-регистрация-на-событие)
- [Registrations API](../../tech-stack/backend/services/event-service.md#registrations)

## Цель

Реализовать полный пользовательский путь регистрации от заполнения формы до получения подтверждения.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены
- [x] Нет блокеров

## Acceptance Criteria

### Форма регистрации

- [x] Выбор типа билета (если несколько)
- [x] Поля: firstName, lastName, email
- [x] Валидация в реальном времени
- [x] Custom fields из настроек события (JSONB)
- [x] Если авторизован — автозаполнение
- [x] Loading состояние кнопки

### Подтверждение

- [x] После успешной регистрации — redirect на success page
- [x] Показать: confirmation code, детали события, тип билета
- [x] Сообщение «Билет отправлен в Telegram»
- [x] Если нет Telegram — предложение привязать
- [x] Кнопка «Добавить в календарь» (ics файл)

### Ошибки

- [x] Билеты закончились — понятное сообщение
- [x] Уже зарегистрирован — сообщение со ссылкой на билет
- [x] Событие отменено — сообщение
- [x] Network error — retry option

### Без авторизации

- [x] Разрешить регистрацию без аккаунта *(решение: требуется авторизация)*
- [x] Предложить создать аккаунт после регистрации *(показываем форму входа/регистрации)*
- [x] Email для отправки билета (через Telegram невозможно без привязки)

### С авторизацией

- [x] Автозаполнение из профиля
- [x] Регистрация привязана к user_id
- [x] Билет отправляется в Telegram если привязан

## Definition of Done (DoD)

- [x] Все Acceptance Criteria выполнены
- [x] UX: < 3 клика до регистрации
- [x] Loading states
- [x] Error handling
- [x] Тесты успешного пути
- [x] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Структура

```
frontend/
├── app/(public)/events/[slug]/
│   └── success/page.tsx        — страница подтверждения
├── components/features/registration/
│   ├── registration-form.tsx
│   ├── ticket-selector.tsx
│   ├── registration-success.tsx
│   └── add-to-calendar.tsx
└── lib/hooks/
    └── use-registration.ts
```

### Registration Flow

```tsx
// Упрощённый flow

// 1. Пользователь заполняет форму
const form = useForm<RegistrationInput>({
  defaultValues: {
    ticketTypeId: ticketTypes[0].id,
    firstName: user?.firstName,
    lastName: user?.lastName,
    email: user?.email,
  },
});

// 2. Submit
const mutation = useMutation({
  mutationFn: (data) => eventApi.createRegistration(eventId, data),
  onSuccess: (registration) => {
    router.push(`/events/${slug}/success?code=${registration.confirmationCode}`);
  },
  onError: (error) => {
    if (error.response?.data?.code === 'already_registered') {
      toast.error('Вы уже зарегистрированы на это событие');
    } else if (error.response?.data?.code === 'sold_out') {
      toast.error('Билеты закончились');
    } else {
      toast.error('Ошибка регистрации. Попробуйте ещё раз.');
    }
  },
});

// 3. Success page показывает confirmation code
```

### Success Page

```tsx
// app/(public)/events/[slug]/success/page.tsx

export default async function SuccessPage({ params, searchParams }) {
  const event = await eventApi.getBySlug(params.slug);
  const code = searchParams.code;

  return (
    <div className="container py-12">
      <Card className="max-w-md mx-auto">
        <CardHeader>
          <CheckCircle className="h-12 w-12 text-green-500 mx-auto" />
          <CardTitle className="text-center">Регистрация успешна!</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="text-center">
            <p className="text-muted-foreground">Код вашего билета:</p>
            <p className="text-2xl font-mono font-bold">{code}</p>
          </div>

          <Separator />

          <div>
            <h3 className="font-semibold">{event.title}</h3>
            <p className="text-sm text-muted-foreground">
              {formatDate(event.startsAt)}
            </p>
            <p className="text-sm text-muted-foreground">
              {event.locationAddress || event.locationUrl}
            </p>
          </div>

          <Alert>
            <MessageSquare className="h-4 w-4" />
            <AlertDescription>
              Билет с QR-кодом отправлен в Telegram
            </AlertDescription>
          </Alert>

          <AddToCalendarButton event={event} />
        </CardContent>
      </Card>
    </div>
  );
}
```

### Add to Calendar

```tsx
function AddToCalendarButton({ event }) {
  const generateIcs = () => {
    const icsContent = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'BEGIN:VEVENT',
      `DTSTART:${formatIcsDate(event.startsAt)}`,
      event.endsAt ? `DTEND:${formatIcsDate(event.endsAt)}` : '',
      `SUMMARY:${event.title}`,
      `DESCRIPTION:${event.description?.slice(0, 200)}`,
      `LOCATION:${event.locationAddress || event.locationUrl || ''}`,
      'END:VEVENT',
      'END:VCALENDAR',
    ].filter(Boolean).join('\r\n');

    const blob = new Blob([icsContent], { type: 'text/calendar' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${event.slug}.ics`;
    a.click();
  };

  return (
    <Button variant="outline" onClick={generateIcs}>
      <Calendar className="mr-2 h-4 w-4" />
      Добавить в календарь
    </Button>
  );
}
```

## Зависимости

### Блокирует

- Нет

### Зависит от

- [P2-011](./P2-011-registrations-crud.md) Registrations API
- [P2-018](./P2-018-frontend-event-page.md) Event page

## Out of Scope

- Payment flow (Phase 3)
- Waitlist (Phase 3)
- Групповая регистрация (Phase 4)
- Apple/Google Wallet интеграция

## Заметки

- Confirmation code показывается на success page и отправляется в Telegram
- Если пользователь не привязал Telegram — показать инструкцию
- Рассмотреть добавление share buttons на success page
- Analytics: track registration conversion
