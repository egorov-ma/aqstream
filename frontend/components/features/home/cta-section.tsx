'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useAuthStore } from '@/lib/store/auth-store';
import { useHydrated } from '@/lib/hooks/use-hydrated';

function CtaSkeleton() {
  return (
    <div className="flex flex-col sm:flex-row justify-center gap-4">
      <Skeleton className="h-11 w-40" />
      <Skeleton className="h-11 w-40" />
    </div>
  );
}

export function CtaSection() {
  const { isAuthenticated } = useAuthStore();
  const isHydrated = useHydrated();

  return (
    <section
      className="py-20 md:py-32 bg-gradient-to-t from-muted/30 to-background"
      data-testid="cta-section"
    >
      <div className="container">
        <div className="text-center max-w-3xl mx-auto">
          <h2 className="text-3xl md:text-5xl font-bold mb-6">Готовы начать?</h2>
          <p className="text-muted-foreground text-lg mb-8">
            Присоединяйтесь к организаторам, которые уже используют AqStream для своих
            мероприятий
          </p>

          {!isHydrated ? (
            <CtaSkeleton />
          ) : isAuthenticated ? (
            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <Button size="lg" asChild data-testid="cta-dashboard">
                <Link href="/dashboard">Личный кабинет</Link>
              </Button>
              <Button size="lg" variant="ghost" asChild data-testid="cta-events">
                <Link href="/events">Посмотреть события</Link>
              </Button>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <Button size="lg" asChild data-testid="cta-register">
                <Link href="/register">Создать аккаунт</Link>
              </Button>
              <Button size="lg" variant="ghost" asChild data-testid="cta-view-events">
                <Link href="/events">Посмотреть события</Link>
              </Button>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
