# P1-010 shadcn/ui Components Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
| Приоритет | `high` |
| Связь с roadmap | [Frontend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Консистентный UI критичен для user experience. shadcn/ui обеспечивает:
- Готовые, протестированные компоненты
- Доступность (a11y) из коробки
- Единый дизайн-язык
- Простую кастомизацию через Tailwind

### Технический контекст

shadcn/ui — **единственная** UI библиотека в проекте (архитектурное решение).

Особенности:
- Компоненты копируются в проект, а не устанавливаются как dependency
- Базируются на Radix UI primitives
- Стилизация через Tailwind CSS
- Полный контроль над кодом компонентов

## Цель

Установить и настроить shadcn/ui с базовым набором компонентов.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [ ] P1-009 завершена (Next.js проект)

## Acceptance Criteria

- [ ] shadcn/ui инициализирован (`pnpm dlx shadcn-ui@latest init`)
- [ ] `components.json` настроен
- [ ] CSS variables для темы в `globals.css`
- [ ] Установлены базовые компоненты:
  - [ ] Button
  - [ ] Card
  - [ ] Input
  - [ ] Label
  - [ ] Form (react-hook-form интеграция)
  - [ ] Dialog
  - [ ] Sheet
  - [ ] Dropdown Menu
  - [ ] Avatar
  - [ ] Badge
  - [ ] Skeleton
  - [ ] Toast (Sonner)
  - [ ] Table
  - [ ] Tabs
  - [ ] Select
- [ ] Компоненты размещены в `components/ui/`
- [ ] Утилита `cn()` работает
- [ ] Документация компонентов обновлена

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Компоненты рендерятся без ошибок
- [ ] Темизация работает (light/dark если нужно)
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: components/ui/, styles
- [ ] Database: не затрагивается
- [ ] Infrastructure: не затрагивается

### Команды установки

```bash
cd frontend

# Инициализация shadcn/ui
pnpm dlx shadcn-ui@latest init

# При инициализации выбрать:
# - Style: Default
# - Base color: Slate
# - CSS variables: Yes
# - tailwind.config: tailwind.config.ts
# - Components location: @/components
# - Utility functions: @/lib/utils

# Установка компонентов
pnpm dlx shadcn-ui@latest add button
pnpm dlx shadcn-ui@latest add card
pnpm dlx shadcn-ui@latest add input
pnpm dlx shadcn-ui@latest add label
pnpm dlx shadcn-ui@latest add form
pnpm dlx shadcn-ui@latest add dialog
pnpm dlx shadcn-ui@latest add sheet
pnpm dlx shadcn-ui@latest add dropdown-menu
pnpm dlx shadcn-ui@latest add avatar
pnpm dlx shadcn-ui@latest add badge
pnpm dlx shadcn-ui@latest add skeleton
pnpm dlx shadcn-ui@latest add sonner
pnpm dlx shadcn-ui@latest add table
pnpm dlx shadcn-ui@latest add tabs
pnpm dlx shadcn-ui@latest add select
```

### components.json

```json
{
  "$schema": "https://ui.shadcn.com/schema.json",
  "style": "default",
  "rsc": true,
  "tsx": true,
  "tailwind": {
    "config": "tailwind.config.ts",
    "css": "styles/globals.css",
    "baseColor": "slate",
    "cssVariables": true
  },
  "aliases": {
    "components": "@/components",
    "utils": "@/lib/utils"
  }
}
```

### globals.css (обновлённый)

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --card: 0 0% 100%;
    --card-foreground: 222.2 84% 4.9%;
    --popover: 0 0% 100%;
    --popover-foreground: 222.2 84% 4.9%;
    --primary: 222.2 47.4% 11.2%;
    --primary-foreground: 210 40% 98%;
    --secondary: 210 40% 96.1%;
    --secondary-foreground: 222.2 47.4% 11.2%;
    --muted: 210 40% 96.1%;
    --muted-foreground: 215.4 16.3% 46.9%;
    --accent: 210 40% 96.1%;
    --accent-foreground: 222.2 47.4% 11.2%;
    --destructive: 0 84.2% 60.2%;
    --destructive-foreground: 210 40% 98%;
    --border: 214.3 31.8% 91.4%;
    --input: 214.3 31.8% 91.4%;
    --ring: 222.2 84% 4.9%;
    --radius: 0.5rem;
  }

  .dark {
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    --card: 222.2 84% 4.9%;
    --card-foreground: 210 40% 98%;
    --popover: 222.2 84% 4.9%;
    --popover-foreground: 210 40% 98%;
    --primary: 210 40% 98%;
    --primary-foreground: 222.2 47.4% 11.2%;
    --secondary: 217.2 32.6% 17.5%;
    --secondary-foreground: 210 40% 98%;
    --muted: 217.2 32.6% 17.5%;
    --muted-foreground: 215 20.2% 65.1%;
    --accent: 217.2 32.6% 17.5%;
    --accent-foreground: 210 40% 98%;
    --destructive: 0 62.8% 30.6%;
    --destructive-foreground: 210 40% 98%;
    --border: 217.2 32.6% 17.5%;
    --input: 217.2 32.6% 17.5%;
    --ring: 212.7 26.8% 83.9%;
  }
}

@layer base {
  * {
    @apply border-border;
  }
  body {
    @apply bg-background text-foreground;
  }
}
```

### Структура компонентов после установки

```
components/
└── ui/
    ├── avatar.tsx
    ├── badge.tsx
    ├── button.tsx
    ├── card.tsx
    ├── dialog.tsx
    ├── dropdown-menu.tsx
    ├── form.tsx
    ├── input.tsx
    ├── label.tsx
    ├── select.tsx
    ├── sheet.tsx
    ├── skeleton.tsx
    ├── sonner.tsx
    ├── table.tsx
    └── tabs.tsx
```

### Пример использования компонентов

```tsx
// app/page.tsx
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function HomePage() {
  return (
    <main className="container mx-auto py-10">
      <Card className="max-w-md mx-auto">
        <CardHeader>
          <CardTitle>Добро пожаловать в AqStream</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" placeholder="name@example.com" />
          </div>
          <Button className="w-full">Начать</Button>
        </CardContent>
      </Card>
    </main>
  );
}
```

### Toast Provider Setup

```tsx
// app/layout.tsx
import { Toaster } from '@/components/ui/sonner';

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ru">
      <body className={inter.className}>
        {children}
        <Toaster />
      </body>
    </html>
  );
}
```

### Использование Toast

```tsx
import { toast } from 'sonner';

function handleSubmit() {
  toast.success('Событие создано');
  // или
  toast.error('Ошибка при создании события');
}
```

### Дополнительные зависимости (устанавливаются автоматически)

```json
{
  "dependencies": {
    "@radix-ui/react-avatar": "^1.0.4",
    "@radix-ui/react-dialog": "^1.0.5",
    "@radix-ui/react-dropdown-menu": "^2.0.6",
    "@radix-ui/react-label": "^2.0.2",
    "@radix-ui/react-select": "^2.0.0",
    "@radix-ui/react-slot": "^1.0.2",
    "@radix-ui/react-tabs": "^1.0.4",
    "@hookform/resolvers": "^3.3.0",
    "class-variance-authority": "^0.7.0",
    "lucide-react": "^0.300.0",
    "react-hook-form": "^7.49.0",
    "sonner": "^1.3.0",
    "zod": "^3.22.0"
  }
}
```

## Зависимости

### Блокирует

- [P1-011] Frontend base structure (использует компоненты)
- Все UI задачи в Phase 2

### Зависит от

- [P1-009] Next.js 14 project setup

## Out of Scope

- Кастомные компоненты (будут в feature-задачах)
- Dark mode toggle (можно добавить позже)
- Анимации (framer-motion)
- Локализация компонентов

## Заметки

- shadcn/ui — **единственная** UI библиотека, никаких MUI, Chakra, Ant Design
- Компоненты копируются в проект — можно модифицировать
- При обновлении shadcn/ui нужно вручную обновлять файлы
- Sonner используется вместо встроенного Toast (лучше UX)
- Form компонент интегрирован с react-hook-form и zod
- Все компоненты поддерживают ref forwarding
