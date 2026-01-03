'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useAuthStore } from '@/lib/store/auth-store';
import { useHydrated } from '@/lib/hooks/use-hydrated';

/**
 * Skeleton для кнопок hero-секции во время гидратации.
 */
function HeroActionsSkeleton() {
  return (
    <>
      <Skeleton className="h-11 w-40" />
      <Skeleton className="h-11 w-40" />
    </>
  );
}

/**
 * Кнопки действий в hero-секции главной страницы.
 *
 * Для неавторизованных: "Начать" и "Узнать больше".
 * Для авторизованных: "Личный кабинет" и "Все события".
 */
export function HeroActions() {
  const { isAuthenticated } = useAuthStore();
  const isHydrated = useHydrated();

  // Skeleton пока загружается состояние авторизации
  if (!isHydrated) {
    return <HeroActionsSkeleton />;
  }

  if (isAuthenticated) {
    return (
      <>
        <Button size="lg" asChild data-testid="hero-dashboard-button">
          <Link href="/dashboard">Личный кабинет</Link>
        </Button>
        <Button size="lg" variant="outline" asChild data-testid="hero-events-button">
          <Link href="/events">Все события</Link>
        </Button>
      </>
    );
  }

  return (
    <>
      <Button size="lg" asChild data-testid="hero-start-button">
        <Link href="/register">Начать</Link>
      </Button>
      <Button size="lg" variant="outline" asChild data-testid="hero-learn-more-button">
        <a href="#features">Узнать больше</a>
      </Button>
    </>
  );
}
