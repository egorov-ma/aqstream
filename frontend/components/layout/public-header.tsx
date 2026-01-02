'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useAuthStore } from '@/lib/store/auth-store';
import { Notifications } from './notifications';
import { UserNav } from './user-nav';
import { ThemeToggle } from './theme-toggle';

/**
 * Skeleton для хедера во время гидратации.
 */
function HeaderSkeleton() {
  return (
    <header className="border-b">
      <div className="container flex h-14 items-center justify-between">
        <Link href="/" className="font-bold">
          AqStream
        </Link>
        <nav className="flex items-center gap-2">
          <Skeleton className="h-9 w-20" />
          <Skeleton className="h-9 w-24" />
        </nav>
      </div>
    </header>
  );
}

/**
 * Публичный хедер с условной навигацией.
 *
 * Для неавторизованных: кнопки "Войти" и "Регистрация".
 * Для авторизованных: ссылки "События", "Мои билеты", "Dashboard",
 * уведомления и UserNav dropdown.
 */
export function PublicHeader() {
  const { isAuthenticated } = useAuthStore();
  const [isHydrated, setIsHydrated] = useState(false);

  // Ждём гидратации store из localStorage
  useEffect(() => {
    setIsHydrated(true);
  }, []);

  // Skeleton пока загружается состояние авторизации
  if (!isHydrated) {
    return <HeaderSkeleton />;
  }

  return (
    <header className="border-b">
      <div className="container flex h-14 items-center justify-between">
        <Link href="/" className="font-bold">
          AqStream
        </Link>

        <nav className="flex items-center gap-2">
          {isAuthenticated ? (
            <>
              {/* Навигация для авторизованных */}
              <Button variant="ghost" asChild>
                <Link href="/events" data-testid="header-events-link">
                  События
                </Link>
              </Button>
              <Button variant="ghost" asChild>
                <Link href="/dashboard/my-registrations" data-testid="header-my-tickets-link">
                  Мои билеты
                </Link>
              </Button>
              <Button variant="ghost" asChild>
                <Link href="/dashboard" data-testid="header-dashboard-link">
                  Dashboard
                </Link>
              </Button>

              {/* Уведомления */}
              <div data-testid="header-notifications">
                <Notifications />
              </div>

              {/* Тема */}
              <ThemeToggle />

              {/* Профиль */}
              <UserNav />
            </>
          ) : (
            <>
              {/* Кнопки для неавторизованных */}
              <ThemeToggle />
              <Button variant="ghost" asChild data-testid="header-login-button">
                <Link href="/login">Войти</Link>
              </Button>
              <Button asChild data-testid="header-register-button">
                <Link href="/register">Регистрация</Link>
              </Button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
