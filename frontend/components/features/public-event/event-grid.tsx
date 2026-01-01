import { EventCard } from './event-card';
import { EventCardSkeleton } from './event-card-skeleton';
import type { PublicEventSummary } from '@/lib/api/types';

interface EventGridProps {
  events?: PublicEventSummary[];
  isLoading?: boolean;
  skeletonCount?: number;
}

/**
 * Grid-контейнер для карточек событий
 */
export function EventGrid({
  events,
  isLoading,
  skeletonCount = 6,
}: EventGridProps) {
  if (isLoading) {
    return (
      <div
        className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
        data-testid="events-grid-loading"
      >
        {Array.from({ length: skeletonCount }).map((_, i) => (
          <EventCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (!events || events.length === 0) {
    return null;
  }

  return (
    <div
      className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
      data-testid="events-grid"
    >
      {events.map((event) => (
        <EventCard key={event.id} event={event} />
      ))}
    </div>
  );
}
