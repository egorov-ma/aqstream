'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/lib/store/auth-store';
import { Notifications } from './notifications';
import { UserNav } from './user-nav';
import { ThemeToggle } from './theme-toggle';
import { PublicMobileNav } from './public-mobile-nav';

/**
 * Публичный хедер с условной навигацией.
 *
 * Для неавторизованных: кнопки "Войти" и "Регистрация".
 * Для авторизованных: ссылки "События", "Мои билеты", "Личный кабинет",
 * уведомления и UserNav dropdown.
 */
export function PublicHeader() {
  const { isAuthenticated } = useAuthStore();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  // SSR: показываем fallback (неавторизованное состояние)
  if (!mounted) {
    return (
      <header className="border-b" aria-label="Главная навигация">
        <div className="container flex h-14 items-center justify-between">
          <Link href="/" className="font-bold">
            AqStream
          </Link>
          <nav className="flex items-center gap-2" data-testid="public-header-nav">
            <ThemeToggle />
            <Button variant="ghost" asChild data-testid="header-login-button">
              <Link href="/login">Войти</Link>
            </Button>
            <Button asChild data-testid="header-register-button">
              <Link href="/register">Регистрация</Link>
            </Button>
          </nav>
        </div>
      </header>
    );
  }

  // CSR: показываем правильное состояние
  return (
    <header className="border-b" aria-label="Главная навигация">
      <div className="container flex h-14 items-center justify-between">
        <Link href="/" className="font-bold">
          AqStream
        </Link>

        <nav className="flex items-center gap-2" data-testid="public-header-nav">
          {isAuthenticated ? (
            <>
              {/* Hamburger меню на mobile */}
              <PublicMobileNav />

              {/* Навигация для авторизованных (скрыта на mobile) */}
              <div className="hidden lg:flex items-center gap-2">
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
                  <Link href="/dashboard" data-testid="header-personal-cabinet-link">
                    Личный кабинет
                  </Link>
                </Button>
              </div>

              {/* Quick-access элементы (видны всегда) */}
              <div data-testid="header-notifications">
                <Notifications />
              </div>
              <ThemeToggle />
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
