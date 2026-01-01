import { CalendarDays } from 'lucide-react';
import { EventCardSkeleton } from '@/components/features/public-event';

export default function EventsLoading() {
  return (
    <div className="container py-8">
      {/* Заголовок */}
      <div className="flex items-center gap-3 mb-8">
        <div className="p-2 rounded-lg bg-primary/10">
          <CalendarDays className="h-6 w-6 text-primary" />
        </div>
        <div>
          <h1 className="text-3xl font-bold">События</h1>
          <p className="text-muted-foreground">
            Предстоящие публичные мероприятия
          </p>
        </div>
      </div>

      {/* Skeleton карточек */}
      <div
        className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
        data-testid="events-grid-loading"
      >
        {Array.from({ length: 6 }).map((_, i) => (
          <EventCardSkeleton key={i} />
        ))}
      </div>
    </div>
  );
}
