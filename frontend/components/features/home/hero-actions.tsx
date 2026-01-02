'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useAuthStore } from '@/lib/store/auth-store';

/**
 * Skeleton для кнопок hero-секции во время гидратации.
 */
function HeroActionsSkeleton() {
  return (
    <>
      <Skeleton className="h-11 w-32" />
      <Skeleton className="h-11 w-32" />
    </>
  );
}

/**
 * Кнопки действий в hero-секции главной страницы.
 *
 * Для неавторизованных: "Войти" и "Регистрация".
 * Для авторизованных: "Dashboard" и "Все события".
 */
export function HeroActions() {
  const { isAuthenticated } = useAuthStore();
  const [isHydrated, setIsHydrated] = useState(false);

  // Ждём гидратации store из localStorage
  useEffect(() => {
    setIsHydrated(true);
  }, []);

  // Skeleton пока загружается состояние авторизации
  if (!isHydrated) {
    return <HeroActionsSkeleton />;
  }

  if (isAuthenticated) {
    return (
      <>
        <Button size="lg" asChild data-testid="hero-dashboard-button">
          <Link href="/dashboard">Перейти в Dashboard</Link>
        </Button>
        <Button size="lg" variant="outline" asChild data-testid="hero-events-button">
          <Link href="/events">Все события</Link>
        </Button>
      </>
    );
  }

  return (
    <>
      <Button size="lg" asChild data-testid="hero-login-button">
        <Link href="/login">Войти</Link>
      </Button>
      <Button size="lg" variant="outline" asChild data-testid="hero-register-button">
        <Link href="/register">Регистрация</Link>
      </Button>
    </>
  );
}
