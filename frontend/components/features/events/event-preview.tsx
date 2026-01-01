'use client';

import * as React from 'react';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  Calendar,
  Clock,
  MapPin,
  Globe,
  Users,
  Ticket,
  Eye,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import { MarkdownPreview } from './markdown-editor';
import { getTimezoneLabel } from './timezone-select';
import type { EventFormData, TicketTypeFormData } from '@/lib/validations/events';

interface EventPreviewProps {
  data: EventFormData;
  trigger?: React.ReactNode;
}

// Форматирование даты и времени
function formatDateTime(isoString: string | undefined | null): string {
  if (!isoString) return '';
  try {
    return format(new Date(isoString), "d MMMM yyyy 'в' HH:mm", { locale: ru });
  } catch {
    return '';
  }
}

// Метка типа локации
function getLocationTypeLabel(locationType: EventFormData['locationType']): string {
  const labels: Record<EventFormData['locationType'], string> = {
    ONLINE: 'Онлайн',
    OFFLINE: 'Офлайн',
    HYBRID: 'Гибрид',
  };
  return labels[locationType] ?? locationType;
}

// Компонент типа билета в предпросмотре
function TicketTypePreview({ ticketType }: { ticketType: TicketTypeFormData }) {
  return (
    <div className="flex items-center justify-between rounded-lg border p-3">
      <div>
        <p className="font-medium">{ticketType.name || 'Без названия'}</p>
        {ticketType.description && (
          <p className="text-sm text-muted-foreground">{ticketType.description}</p>
        )}
      </div>
      <div className="text-right">
        <p className="text-sm text-muted-foreground">
          {ticketType.quantity ? `${ticketType.quantity} мест` : 'Без ограничений'}
        </p>
      </div>
    </div>
  );
}

// Компонент содержимого предпросмотра
function PreviewContent({ data }: { data: EventFormData }) {
  const hasLocation =
    (data.locationType === 'OFFLINE' || data.locationType === 'HYBRID') &&
    data.locationAddress;
  const hasOnlineUrl =
    (data.locationType === 'ONLINE' || data.locationType === 'HYBRID') &&
    data.onlineUrl;

  return (
    <div className="space-y-6">
      {/* Обложка */}
      {data.coverImageUrl && (
        <div className="relative aspect-video w-full overflow-hidden rounded-lg">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={data.coverImageUrl}
            alt={data.title || 'Обложка события'}
            className="h-full w-full object-cover"
          />
        </div>
      )}

      {/* Заголовок и метки */}
      <div>
        <div className="flex flex-wrap gap-2 mb-2">
          <Badge variant="secondary">{getLocationTypeLabel(data.locationType)}</Badge>
          {data.isPublic && <Badge variant="outline">Публичное</Badge>}
        </div>
        <h2 className="text-2xl font-bold">{data.title || 'Без названия'}</h2>
      </div>

      {/* Дата и время */}
      <Card>
        <CardContent className="pt-4">
          <div className="space-y-3">
            {data.startsAt && (
              <div className="flex items-center gap-3">
                <Calendar className="h-5 w-5 text-muted-foreground" />
                <div>
                  <p className="font-medium">Начало</p>
                  <p className="text-sm text-muted-foreground">
                    {formatDateTime(data.startsAt)}
                  </p>
                </div>
              </div>
            )}
            {data.endsAt && (
              <div className="flex items-center gap-3">
                <Clock className="h-5 w-5 text-muted-foreground" />
                <div>
                  <p className="font-medium">Окончание</p>
                  <p className="text-sm text-muted-foreground">
                    {formatDateTime(data.endsAt)}
                  </p>
                </div>
              </div>
            )}
            {data.timezone && (
              <p className="text-sm text-muted-foreground pl-8">
                {getTimezoneLabel(data.timezone)}
              </p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Локация */}
      {(hasLocation || hasOnlineUrl) && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Место проведения</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {hasLocation && (
              <div className="flex items-start gap-3">
                <MapPin className="h-5 w-5 text-muted-foreground mt-0.5" />
                <p>{data.locationAddress}</p>
              </div>
            )}
            {hasOnlineUrl && (
              <div className="flex items-start gap-3">
                <Globe className="h-5 w-5 text-muted-foreground mt-0.5" />
                <a
                  href={data.onlineUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-primary hover:underline break-all"
                >
                  {data.onlineUrl}
                </a>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Описание */}
      {data.description && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Описание</CardTitle>
          </CardHeader>
          <CardContent>
            <MarkdownPreview source={data.description} />
          </CardContent>
        </Card>
      )}

      {/* Вместимость */}
      {data.maxCapacity && (
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <Users className="h-4 w-4" />
          <span>Максимум участников: {data.maxCapacity}</span>
        </div>
      )}

      {/* Типы билетов */}
      {data.ticketTypes && data.ticketTypes.length > 0 && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base flex items-center gap-2">
              <Ticket className="h-4 w-4" />
              Типы билетов
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {data.ticketTypes.map((tt, index) => (
              <TicketTypePreview key={index} ticketType={tt} />
            ))}
          </CardContent>
        </Card>
      )}

      {/* Регистрация */}
      {(data.registrationOpensAt || data.registrationClosesAt) && (
        <>
          <Separator />
          <div className="text-sm text-muted-foreground space-y-1">
            {data.registrationOpensAt && (
              <p>Регистрация открывается: {formatDateTime(data.registrationOpensAt)}</p>
            )}
            {data.registrationClosesAt && (
              <p>Регистрация закрывается: {formatDateTime(data.registrationClosesAt)}</p>
            )}
          </div>
        </>
      )}
    </div>
  );
}

export function EventPreview({ data, trigger }: EventPreviewProps) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        {trigger || (
          <Button type="button" variant="outline" size="sm">
            <Eye className="mr-2 h-4 w-4" />
            Предпросмотр
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[90vh]">
        <DialogHeader>
          <DialogTitle>Предпросмотр события</DialogTitle>
        </DialogHeader>
        <ScrollArea className="max-h-[calc(90vh-100px)]">
          <div className="pr-4">
            <PreviewContent data={data} />
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  );
}

// Экспорт отдельного компонента без диалога для встраивания
export { PreviewContent as EventPreviewContent };
