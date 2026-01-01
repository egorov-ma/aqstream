import Link from 'next/link';
import { ArrowRight, CalendarDays } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EventGrid } from '@/components/features/public-event';
import type { PublicEventSummary } from '@/lib/api/types';

interface UpcomingEventsSectionProps {
  events: PublicEventSummary[];
  hasMore?: boolean;
}

/**
 * SSR секция "Предстоящие события" на главной странице.
 * Данные загружаются на сервере для SEO.
 */
export function UpcomingEventsSection({ events, hasMore }: UpcomingEventsSectionProps) {
  if (events.length === 0) {
    return null;
  }

  return (
    <section className="py-16 bg-muted/30" data-testid="upcoming-events-section">
      <div className="container">
        {/* Заголовок секции */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <CalendarDays className="h-6 w-6 text-primary" />
            </div>
            <div>
              <h2 className="text-2xl font-bold">Предстоящие события</h2>
              <p className="text-muted-foreground">
                Ближайшие публичные мероприятия
              </p>
            </div>
          </div>

          {hasMore && (
            <Button variant="ghost" asChild className="hidden sm:flex">
              <Link href="/events">
                Все события
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
          )}
        </div>

        {/* Grid карточек */}
        <EventGrid events={events} />

        {/* Мобильная кнопка "Все события" */}
        {hasMore && (
          <div className="mt-8 text-center sm:hidden">
            <Button variant="outline" asChild>
              <Link href="/events">
                Все события
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
          </div>
        )}
      </div>
    </section>
  );
}
