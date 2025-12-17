# P2-015 Frontend: Auth pages

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 2: Core |
| Статус | `ready` |
| Приоритет | `critical` |
| Связь с roadmap | [Roadmap - Frontend](../../business/roadmap.md#фаза-2-core) |

## Контекст

### Бизнес-контекст

Страницы аутентификации — точка входа для пользователей. Нужны формы входа, регистрации и восстановления пароля. Также интеграция с Telegram Login Widget.

### Технический контекст

- Next.js 14 App Router
- Route group `(auth)` — минимальный layout
- shadcn/ui компоненты
- React Hook Form + Zod для валидации
- Zustand для auth state

**Существующий код:**
- [login/page.tsx](../../../frontend/app/(auth)/login/page.tsx) — placeholder
- [register/page.tsx](../../../frontend/app/(auth)/register/page.tsx) — placeholder

**Связанные документы:**
- [Frontend Architecture](../../tech-stack/frontend/architecture.md)
- [Components](../../tech-stack/frontend/components.md)
- [User Service API](../../tech-stack/backend/services/user-service.md#authentication)

## Цель

Реализовать полнофункциональные страницы входа, регистрации и восстановления пароля с интеграцией с backend API.

## Definition of Ready (DoR)

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены (backend API)
- [x] Нет блокеров

## Acceptance Criteria

### Страница входа (/login)

- [ ] Форма с полями: email, password
- [ ] Валидация: email формат, пароль обязателен
- [ ] Кнопка «Войти»
- [ ] Ссылка «Забыли пароль?»
- [ ] Ссылка «Зарегистрироваться»
- [ ] Telegram Login Widget
- [ ] При успешном входе — redirect на /dashboard
- [ ] Отображение ошибок API
- [ ] Loading состояние кнопки

### Страница регистрации (/register)

- [ ] Форма: email, password, confirmPassword, firstName, lastName
- [ ] Валидация: email, пароль 8+ символов с буквами и цифрами
- [ ] Пароли должны совпадать
- [ ] Кнопка «Зарегистрироваться»
- [ ] Ссылка «Уже есть аккаунт?»
- [ ] После регистрации — redirect на /verify-email-sent
- [ ] Telegram Login Widget как альтернатива

### Страница восстановления пароля (/forgot-password)

- [ ] Форма с email
- [ ] Кнопка «Отправить инструкции»
- [ ] После отправки — сообщение об успехе
- [ ] Ссылка назад к входу

### Страница сброса пароля (/reset-password)

- [ ] Форма: newPassword, confirmPassword
- [ ] Валидация пароля
- [ ] Token из URL query params
- [ ] После сброса — redirect на /login с сообщением

### Telegram Login Widget

- [ ] Интеграция на страницах login и register
- [ ] При успешном входе через Telegram — создание/вход
- [ ] Обработка callback от Telegram

### Auth State Management

- [ ] Zustand store для auth state (user, accessToken)
- [ ] Persist в localStorage
- [ ] Auto-refresh токена
- [ ] Logout очищает state

## Definition of Done (DoD)

- [ ] Все Acceptance Criteria выполнены
- [ ] Используются только shadcn/ui компоненты
- [ ] React Hook Form + Zod для форм
- [ ] Responsive design (mobile-first)
- [ ] Unit тесты для форм
- [ ] E2E тесты критических путей
- [ ] Code review пройден
- [ ] CI/CD pipeline проходит

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: —
- [x] Frontend: `app/(auth)/*`, `lib/api/auth.ts`, `lib/store/auth-store.ts`
- [ ] Database: —
- [ ] Infrastructure: —

### Структура файлов

```
frontend/
├── app/
│   └── (auth)/
│       ├── layout.tsx          — минимальный layout
│       ├── login/page.tsx      — вход
│       ├── register/page.tsx   — регистрация
│       ├── forgot-password/page.tsx
│       ├── reset-password/page.tsx
│       └── verify-email/page.tsx
├── components/
│   └── features/
│       └── auth/
│           ├── login-form.tsx
│           ├── register-form.tsx
│           ├── forgot-password-form.tsx
│           ├── reset-password-form.tsx
│           └── telegram-login.tsx
└── lib/
    ├── api/auth.ts
    └── store/auth-store.ts
```

### Login Form

```tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { useAuthStore } from '@/lib/store/auth-store';
import { authApi } from '@/lib/api/auth';

const loginSchema = z.object({
  email: z.string().email('Некорректный email'),
  password: z.string().min(1, 'Пароль обязателен'),
});

export function LoginForm() {
  const { setAuth } = useAuthStore();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const form = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  async function onSubmit(data: z.infer<typeof loginSchema>) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authApi.login(data);
      setAuth(response.user, response.accessToken);
      router.push('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Ошибка входа');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        {error && <div className="text-destructive text-sm">{error}</div>}
        {/* fields */}
        <Button type="submit" className="w-full" disabled={isLoading}>
          {isLoading ? 'Вход...' : 'Войти'}
        </Button>
      </form>
    </Form>
  );
}
```

### Telegram Login Widget

```tsx
'use client';

import { useEffect, useRef } from 'react';

interface TelegramLoginProps {
  botName: string;
  onAuth: (user: TelegramUser) => void;
}

export function TelegramLogin({ botName, onAuth }: TelegramLoginProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Добавляем скрипт Telegram
    const script = document.createElement('script');
    script.src = 'https://telegram.org/js/telegram-widget.js?22';
    script.setAttribute('data-telegram-login', botName);
    script.setAttribute('data-size', 'large');
    script.setAttribute('data-request-access', 'write');
    script.setAttribute('data-onauth', 'onTelegramAuth(user)');
    script.async = true;

    // Callback
    (window as any).onTelegramAuth = (user: TelegramUser) => {
      onAuth(user);
    };

    containerRef.current?.appendChild(script);
  }, [botName, onAuth]);

  return <div ref={containerRef} />;
}
```

## Зависимости

### Блокирует

- [P2-016](./P2-016-frontend-dashboard.md) Dashboard (требует авторизации)

### Зависит от

- [P2-001](./P2-001-auth-email-registration.md) Backend API
- [P2-002](./P2-002-auth-telegram.md) Telegram auth API

## Out of Scope

- Social login (Google, GitHub)
- 2FA
- Magic link login
- Запоминание устройства

## Заметки

- Текущие placeholder страницы показывают «Функционал будет доступен в Phase 2»
- Telegram Login Widget требует HTTPS в production
- accessToken хранить в memory, refreshToken в httpOnly cookie (если возможно) или localStorage
- Рассмотреть middleware для защиты роутов
