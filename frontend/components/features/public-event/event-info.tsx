'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Calendar, MapPin, Globe, Building2, User, Clock } from 'lucide-react';
import { formatEventDate, formatEventDateLocal } from '@/lib/utils/seo';
import { getLocationTypeLabelFull } from '@/lib/utils/event';
import type { Event } from '@/lib/api/types';
import ReactMarkdown from 'react-markdown';
import rehypeSanitize from 'rehype-sanitize';

interface EventInfoProps {
  event: Event;
}

/**
 * Компонент информационной секции
 */
function InfoSection({
  icon: Icon,
  title,
  children,
}: {
  icon: React.ElementType;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex gap-3">
      <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
        <Icon className="h-5 w-5 text-primary" />
      </div>
      <div>
        <h3 className="font-medium text-sm text-muted-foreground">{title}</h3>
        <div className="mt-0.5">{children}</div>
      </div>
    </div>
  );
}

/**
 * Детальная информация о событии
 * Описание, дата/время, место, организатор
 */
export function EventInfo({ event }: EventInfoProps) {
  const eventTimezone = event.timezone;
  const userTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const showLocalTime = eventTimezone !== userTimezone;

  return (
    <div className="space-y-6" data-testid="event-info">
      {/* Описание */}
      {event.description && (
        <Card data-testid="event-description-card">
          <CardHeader>
            <CardTitle>О событии</CardTitle>
          </CardHeader>
          <CardContent className="prose prose-sm max-w-none dark:prose-invert">
            <ReactMarkdown rehypePlugins={[rehypeSanitize]}>
              {event.description}
            </ReactMarkdown>
          </CardContent>
        </Card>
      )}

      {/* Дата и время */}
      <Card data-testid="event-datetime-card">
        <CardHeader>
          <CardTitle>Дата и время</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <InfoSection icon={Calendar} title="Начало">
            <p className="font-medium">{formatEventDate(event.startsAt, eventTimezone)}</p>
            <p className="text-sm text-muted-foreground">Часовой пояс: {eventTimezone}</p>
          </InfoSection>

          {event.endsAt && (
            <InfoSection icon={Clock} title="Окончание">
              <p className="font-medium">{formatEventDate(event.endsAt, eventTimezone)}</p>
            </InfoSection>
          )}

          {showLocalTime && (
            <div className="pt-2 border-t">
              <p className="text-sm text-muted-foreground mb-1">В вашем часовом поясе:</p>
              <p className="font-medium">{formatEventDateLocal(event.startsAt)}</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Место проведения */}
      <Card data-testid="event-location-card">
        <CardHeader>
          <CardTitle>Место проведения</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <InfoSection
            icon={event.locationType === 'ONLINE' ? Globe : Building2}
            title="Формат"
          >
            <p className="font-medium">{getLocationTypeLabelFull(event.locationType)}</p>
          </InfoSection>

          {/* Физический адрес */}
          {(event.locationType === 'OFFLINE' || event.locationType === 'HYBRID') &&
            event.locationAddress && (
              <InfoSection icon={MapPin} title="Адрес">
                <p className="font-medium">{event.locationAddress}</p>
              </InfoSection>
            )}

          {/* Онлайн ссылка — скрываем до регистрации для безопасности */}
          {(event.locationType === 'ONLINE' || event.locationType === 'HYBRID') && (
            <InfoSection icon={Globe} title="Онлайн">
              <p className="text-muted-foreground">
                Ссылка для подключения будет доступна после регистрации
              </p>
            </InfoSection>
          )}
        </CardContent>
      </Card>

      {/* Организатор */}
      {event.organizerName && (
        <Card data-testid="event-organizer-card">
          <CardHeader>
            <CardTitle>Организатор</CardTitle>
          </CardHeader>
          <CardContent>
            <InfoSection icon={User} title="Организация">
              <p className="font-medium">{event.organizerName}</p>
            </InfoSection>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
