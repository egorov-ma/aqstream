import Image from 'next/image';
import Link from 'next/link';
import { Calendar, MapPin, Globe, Building2 } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { getLocationTypeLabel } from '@/lib/utils/event';
import type { PublicEventSummary, LocationType } from '@/lib/api/types';

interface EventCardProps {
  event: PublicEventSummary;
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
 * Форматирует дату для карточки (краткий формат)
 */
function formatCardDate(dateString: string, timezone: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('ru-RU', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: timezone,
  });
}

/**
 * Карточка публичного события для grid-отображения
 */
export function EventCard({ event }: EventCardProps) {
  const LocationIcon = getLocationIcon(event.locationType);
  const formattedDate = formatCardDate(event.startsAt, event.timezone);

  return (
    <Link href={`/events/${event.slug}`} data-testid={`event-card-${event.id}`}>
      <Card className="group h-full overflow-hidden transition-all hover:shadow-lg hover:border-primary/50">
        {/* Обложка */}
        <div className="relative h-48 w-full overflow-hidden bg-muted">
          {event.coverImageUrl ? (
            <Image
              src={event.coverImageUrl}
              alt={event.title}
              fill
              className="object-cover transition-transform group-hover:scale-105"
              sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
            />
          ) : (
            <div className="h-full w-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
              <Calendar className="h-16 w-16 text-primary/30" />
            </div>
          )}

          {/* Badge формата */}
          <Badge
            variant="secondary"
            className="absolute top-3 left-3 bg-background/90 backdrop-blur-sm"
          >
            {getLocationTypeLabel(event.locationType)}
          </Badge>
        </div>

        <CardContent className="p-4">
          {/* Название */}
          <h3 className="font-semibold text-lg line-clamp-2 mb-2 group-hover:text-primary transition-colors">
            {event.title}
          </h3>

          {/* Описание */}
          {event.description && (
            <p className="text-sm text-muted-foreground line-clamp-2 mb-3">
              {event.description}
            </p>
          )}

          {/* Мета-информация */}
          <div className="space-y-2 text-sm text-muted-foreground">
            {/* Дата */}
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 flex-shrink-0" />
              <span>{formattedDate}</span>
            </div>

            {/* Место */}
            <div className="flex items-center gap-2">
              <LocationIcon className="h-4 w-4 flex-shrink-0" />
              <span className="truncate">
                {event.locationType === 'ONLINE'
                  ? 'Онлайн'
                  : event.locationAddress || 'Место уточняется'}
              </span>
            </div>

            {/* Организатор */}
            {event.organizerName && (
              <div className="text-xs text-muted-foreground/70 pt-1">
                {event.organizerName}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
