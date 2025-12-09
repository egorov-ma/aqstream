# P1-009 Next.js 14 Project Setup

## Метаданные

| Поле | Значение |
|------|----------|
| Фаза | Phase 1: Foundation |
| Статус | `backlog` |
| Приоритет | `critical` |
| Связь с roadmap | [Frontend Foundation](../../business/roadmap.md#фаза-1-foundation) |

## Контекст

### Бизнес-контекст

Frontend AqStream — это SPA для организаторов и публичные страницы для участников:
- Dashboard для управления событиями
- Публичные страницы событий
- Форма регистрации
- Личный кабинет участника

### Технический контекст

Стек:
- Next.js 14 с App Router
- TypeScript 5 (strict mode)
- Tailwind CSS 3
- pnpm как package manager

App Router преимущества:
- Server Components для SEO
- Streaming и Suspense
- Route groups для организации
- Parallel routes для layouts

## Цель

Инициализировать Next.js 14 проект с базовой конфигурацией и структурой.

## Definition of Ready (DoR)

Задача готова к взятию в работу, когда:

- [x] Контекст понятен и описан
- [x] Цель сформулирована
- [x] Acceptance Criteria определены
- [x] Технические детали проработаны
- [x] Зависимости определены и разрешены
- [ ] P1-001 завершена (структура репозитория)

## Acceptance Criteria

- [ ] Next.js 14 проект создан в `frontend/`
- [ ] TypeScript настроен в strict mode
- [ ] Tailwind CSS 3 настроен
- [ ] ESLint + Prettier настроены
- [ ] Базовая структура директорий создана:
  - [ ] `app/` с route groups
  - [ ] `components/`
  - [ ] `lib/`
  - [ ] `types/`
- [ ] `pnpm` используется как package manager
- [ ] Базовый `layout.tsx` создан
- [ ] Placeholder страницы созданы
- [ ] `pnpm dev` запускает dev server
- [ ] `pnpm build` успешно собирает проект
- [ ] `pnpm lint` проходит без ошибок
- [ ] Environment variables настроены
- [ ] Dockerfile создан

## Definition of Done (DoD)

Задача считается выполненной, когда:

- [ ] Все Acceptance Criteria выполнены
- [ ] Dev server работает на localhost:3000
- [ ] Production build успешен
- [ ] Code review пройден
- [ ] CI pipeline проходит
- [ ] Чеклист в roadmap обновлён

## Технические детали

### Затрагиваемые компоненты

- [ ] Backend: не затрагивается
- [x] Frontend: полная инициализация
- [ ] Database: не затрагивается
- [x] Infrastructure: Dockerfile, docker-compose

### Структура проекта

```
frontend/
├── app/
│   ├── (auth)/                    # Route group: auth pages
│   │   ├── login/
│   │   │   └── page.tsx
│   │   ├── register/
│   │   │   └── page.tsx
│   │   └── layout.tsx
│   ├── (dashboard)/               # Route group: protected pages
│   │   ├── events/
│   │   │   └── page.tsx
│   │   ├── settings/
│   │   │   └── page.tsx
│   │   └── layout.tsx
│   ├── (public)/                  # Route group: public pages
│   │   ├── events/
│   │   │   └── [slug]/
│   │   │       └── page.tsx
│   │   └── layout.tsx
│   ├── layout.tsx                 # Root layout
│   ├── page.tsx                   # Home page
│   ├── error.tsx                  # Error boundary
│   ├── loading.tsx                # Loading state
│   └── not-found.tsx              # 404 page
├── components/
│   ├── ui/                        # shadcn/ui components (P1-010)
│   ├── forms/                     # Form components
│   ├── layout/                    # Layout components
│   └── features/                  # Feature components
├── lib/
│   ├── api/                       # API client (P1-012)
│   ├── hooks/                     # Custom hooks
│   ├── store/                     # Zustand stores
│   └── utils/                     # Utilities
│       └── cn.ts                  # className utility
├── types/
│   └── index.ts                   # TypeScript types
├── styles/
│   └── globals.css                # Global styles + Tailwind
├── public/
│   └── favicon.ico
├── .env.example
├── .env.local
├── .eslintrc.json
├── .prettierrc
├── next.config.js
├── tailwind.config.ts
├── tsconfig.json
├── postcss.config.js
├── package.json
├── pnpm-lock.yaml
└── Dockerfile
```

### Команды инициализации

```bash
cd frontend

# Создание проекта
pnpm create next-app@14 . --typescript --tailwind --eslint --app --src-dir=false --import-alias="@/*"

# Дополнительные зависимости
pnpm add clsx tailwind-merge
pnpm add -D prettier eslint-config-prettier
```

### package.json

```json
{
  "name": "aqstream-frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "lint:fix": "next lint --fix",
    "typecheck": "tsc --noEmit",
    "format": "prettier --write .",
    "format:check": "prettier --check ."
  },
  "dependencies": {
    "next": "14.2.0",
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.2.0"
  },
  "devDependencies": {
    "@types/node": "^20.11.0",
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "autoprefixer": "^10.4.0",
    "eslint": "^8.56.0",
    "eslint-config-next": "14.2.0",
    "eslint-config-prettier": "^9.1.0",
    "postcss": "^8.4.0",
    "prettier": "^3.2.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.3.0"
  }
}
```

### tsconfig.json

```json
{
  "compilerOptions": {
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "incremental": true,
    "plugins": [
      {
        "name": "next"
      }
    ],
    "paths": {
      "@/*": ["./*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx", ".next/types/**/*.ts"],
  "exclude": ["node_modules"]
}
```

### tailwind.config.ts

```typescript
import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: ['class'],
  content: [
    './pages/**/*.{ts,tsx}',
    './components/**/*.{ts,tsx}',
    './app/**/*.{ts,tsx}',
  ],
  theme: {
    container: {
      center: true,
      padding: '2rem',
      screens: {
        '2xl': '1400px',
      },
    },
    extend: {
      colors: {
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
    },
  },
  plugins: [],
};

export default config;
```

### app/layout.tsx

```tsx
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import '@/styles/globals.css';

const inter = Inter({ subsets: ['latin', 'cyrillic'] });

export const metadata: Metadata = {
  title: 'AqStream - Платформа для управления мероприятиями',
  description: 'Создавайте события, управляйте регистрациями, анализируйте результаты',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ru">
      <body className={inter.className}>{children}</body>
    </html>
  );
}
```

### lib/utils/cn.ts

```typescript
import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

### .env.example

```bash
# API
NEXT_PUBLIC_API_URL=http://localhost:8080

# App
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

### .eslintrc.json

```json
{
  "extends": ["next/core-web-vitals", "prettier"],
  "rules": {
    "react/no-unescaped-entities": "off",
    "@next/next/no-page-custom-font": "off"
  }
}
```

### .prettierrc

```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100,
  "plugins": ["prettier-plugin-tailwindcss"]
}
```

### Dockerfile

```dockerfile
FROM node:20-alpine AS base

# Install pnpm
RUN corepack enable && corepack prepare pnpm@8 --activate

# Dependencies stage
FROM base AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# Build stage
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN pnpm build

# Production stage
FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
```

### next.config.js (для standalone output)

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,
};

module.exports = nextConfig;
```

## Зависимости

### Блокирует

- [P1-010] shadcn/ui components setup
- [P1-011] Frontend base structure
- [P1-012] API client setup

### Зависит от

- [P1-001] Настройка монорепозитория

## Out of Scope

- shadcn/ui компоненты (P1-010)
- API client и hooks (P1-012)
- State management (Zustand, TanStack Query)
- Аутентификация
- Реальные страницы (только placeholders)

## Заметки

- Используем Inter с поддержкой кириллицы
- Route groups `(auth)`, `(dashboard)`, `(public)` для разных layouts
- standalone output для оптимального Docker образа
- Tailwind config подготовлен для shadcn/ui
- ESLint + Prettier для code style
- TypeScript в strict mode обязателен
