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
  // Empty state
  if (events.length === 0) {
    return (
      <section
        className="py-20 md:py-32 bg-muted/30"
        data-testid="upcoming-events-section"
      >
        <div className="container">
          <div className="text-center max-w-2xl mx-auto">
            <div className="p-4 rounded-full bg-muted/50 w-fit mx-auto mb-6">
              <CalendarDays className="h-16 w-16 text-muted-foreground/30" />
            </div>
            <h2 className="text-2xl md:text-3xl font-bold mb-4">
              Скоро здесь появятся события
            </h2>
            <p className="text-muted-foreground mb-6">
              Станьте первым организатором и создайте мероприятие для вашего сообщества
            </p>
            <Button asChild data-testid="empty-state-create-event">
              <Link href="/dashboard/events/new">Создать событие</Link>
            </Button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="py-20 md:py-32 bg-muted/30" data-testid="upcoming-events-section">
      <div className="container">
        {/* Заголовок секции */}
        <div className="flex items-center justify-between mb-12">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <CalendarDays className="h-6 w-6 text-primary" />
            </div>
            <div>
              <h2 className="text-2xl md:text-3xl font-bold">Предстоящие события</h2>
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
