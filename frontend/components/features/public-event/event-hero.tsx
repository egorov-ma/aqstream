'use client';

import { Badge } from '@/components/ui/badge';
import { Calendar, MapPin, Globe, Building2 } from 'lucide-react';
import { formatEventDate } from '@/lib/utils/seo';
import { getLocationTypeLabel } from '@/lib/utils/event';
import type { Event, LocationType } from '@/lib/api/types';

interface EventHeroProps {
  event: Event;
}

/**
 * Получает иконку для типа локации
 */
function getLocationIcon(locationType: LocationType) {
  switch (locationType) {
    case 'ONLINE':
      return Globe;
    case 'OFFLINE':
      return MapPin;
    case 'HYBRID':
      return Building2;
    default:
      return MapPin;
  }
}

/**
 * Hero-секция публичной страницы события
 * Отображает обложку, название, дату/время, место проведения
 */
export function EventHero({ event }: EventHeroProps) {
  const LocationIcon = getLocationIcon(event.locationType);
  const formattedDate = formatEventDate(event.startsAt, event.timezone);

  return (
    <div className="relative" data-testid="event-hero">
      {/* Обложка */}
      <div className="relative h-64 md:h-80 lg:h-96 w-full overflow-hidden rounded-lg bg-muted">
        {event.coverImageUrl ? (
          <img
            src={event.coverImageUrl}
            alt={event.title}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="h-full w-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
            <Calendar className="h-24 w-24 text-primary/30" />
          </div>
        )}

        {/* Overlay gradient */}
        <div className="absolute inset-0 bg-gradient-to-t from-background/90 via-background/50 to-transparent" />
      </div>

      {/* Контент поверх обложки */}
      <div className="absolute bottom-0 left-0 right-0 p-6">
        <div className="max-w-4xl">
          {/* Badges */}
          <div className="flex flex-wrap gap-2 mb-3">
            {event.status === 'CANCELLED' && (
              <Badge variant="destructive">Отменено</Badge>
            )}
            {event.status === 'COMPLETED' && (
              <Badge variant="secondary">Завершено</Badge>
            )}
            <Badge variant="outline" className="bg-background/80">
              {getLocationTypeLabel(event.locationType)}
            </Badge>
          </div>

          {/* Название */}
          <h1 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-4 text-foreground drop-shadow-sm" data-testid="event-title">
            {event.title}
          </h1>

          {/* Дата и место */}
          <div className="flex flex-col sm:flex-row gap-4 text-muted-foreground">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              <span>{formattedDate}</span>
            </div>

            {(event.locationAddress || event.onlineUrl) && (
              <div className="flex items-center gap-2">
                <LocationIcon className="h-5 w-5" />
                <span>
                  {event.locationType === 'ONLINE'
                    ? 'Онлайн-мероприятие'
                    : event.locationAddress || 'Место уточняется'}
                </span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
