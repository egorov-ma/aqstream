'use client';

import * as React from 'react';
import Link from 'next/link';
import { useParams, notFound } from 'next/navigation';
import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  ArrowLeft,
  Edit,
  Calendar,
  MapPin,
  Globe,
  Users,
  Ticket,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';
import { useEvent } from '@/lib/hooks/use-events';
import { useTicketTypes } from '@/lib/hooks/use-ticket-types';
import {
  EventStatusBadge,
  EventActions,
  EventStatsCard,
  MarkdownPreview,
  RegistrationList,
  EventActivityLog,
  getTimezoneLabel,
} from '@/components/features/events';

export default function EventDetailPage() {
  const params = useParams();
  const id = params.id as string;

  const { data: event, isLoading: isLoadingEvent, error } = useEvent(id);
  const { data: ticketTypes, isLoading: isLoadingTickets } = useTicketTypes(id);

  // Если событие не найдено
  if (error) {
    notFound();
  }

  // Loading state
  if (isLoadingEvent) {
    return <EventDetailSkeleton />;
  }

  if (!event) {
    return null;
  }

  const formatDate = (isoString: string) => {
    const date = parseISO(isoString);
    return format(date, 'd MMMM yyyy, HH:mm', { locale: ru });
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/dashboard/events">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold">{event.title}</h1>
              <EventStatusBadge status={event.status} />
            </div>
            <p className="text-sm text-muted-foreground">
              Создано {formatDate(event.createdAt)}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" asChild>
            <Link href={`/dashboard/events/${id}/edit`}>
              <Edit className="mr-2 h-4 w-4" />
              Редактировать
            </Link>
          </Button>
          <EventActions event={event} />
        </div>
      </div>

      {/* Статистика */}
      <EventStatsCard
        event={event}
        registrationsCount={0}
        checkedInCount={0}
        ticketTypesCount={ticketTypes?.length ?? 0}
        isLoading={isLoadingTickets}
      />

      <div className="grid gap-6 md:grid-cols-2">
        {/* Информация о событии */}
        <Card>
          <CardHeader>
            <CardTitle>Информация</CardTitle>
            <CardDescription>Основные данные о мероприятии</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Дата и время */}
            <div className="flex items-start gap-3">
              <Calendar className="h-5 w-5 text-muted-foreground mt-0.5" />
              <div>
                <p className="font-medium">{formatDate(event.startsAt)}</p>
                {event.endsAt && (
                  <p className="text-sm text-muted-foreground">
                    до {formatDate(event.endsAt)}
                  </p>
                )}
                <p className="text-sm text-muted-foreground">
                  {getTimezoneLabel(event.timezone)}
                </p>
              </div>
            </div>

            <Separator />

            {/* Локация */}
            <div className="flex items-start gap-3">
              {event.locationType === 'ONLINE' ? (
                <Globe className="h-5 w-5 text-muted-foreground mt-0.5" />
              ) : (
                <MapPin className="h-5 w-5 text-muted-foreground mt-0.5" />
              )}
              <div>
                {event.locationType === 'ONLINE' && (
                  <>
                    <p className="font-medium">Онлайн мероприятие</p>
                    {event.onlineUrl && (
                      <a
                        href={event.onlineUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sm text-primary hover:underline"
                      >
                        {event.onlineUrl}
                      </a>
                    )}
                  </>
                )}
                {event.locationType === 'OFFLINE' && (
                  <>
                    <p className="font-medium">Офлайн мероприятие</p>
                    {event.locationAddress && (
                      <p className="text-sm text-muted-foreground">
                        {event.locationAddress}
                      </p>
                    )}
                  </>
                )}
                {event.locationType === 'HYBRID' && (
                  <>
                    <p className="font-medium">Гибридное мероприятие</p>
                    {event.locationAddress && (
                      <p className="text-sm text-muted-foreground">
                        {event.locationAddress}
                      </p>
                    )}
                    {event.onlineUrl && (
                      <a
                        href={event.onlineUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sm text-primary hover:underline"
                      >
                        Онлайн трансляция
                      </a>
                    )}
                  </>
                )}
              </div>
            </div>

            {/* Описание */}
            {event.description && (
              <>
                <Separator />
                <div>
                  <p className="font-medium mb-2">Описание</p>
                  <MarkdownPreview source={event.description} />
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Типы билетов */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Ticket className="h-5 w-5" />
              Типы билетов
            </CardTitle>
            <CardDescription>Доступные варианты регистрации</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoadingTickets ? (
              <div className="space-y-3">
                {[1, 2].map((i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : ticketTypes && ticketTypes.length > 0 ? (
              <div className="space-y-3">
                {ticketTypes.map((tt) => (
                  <div
                    key={tt.id}
                    className="flex items-center justify-between rounded-lg border p-3"
                  >
                    <div>
                      <p className="font-medium">{tt.name}</p>
                      {tt.description && (
                        <p className="text-sm text-muted-foreground">
                          {tt.description}
                        </p>
                      )}
                    </div>
                    <div className="text-right text-sm">
                      <p className="font-medium">
                        {tt.quantity ? `${tt.soldCount}/${tt.quantity}` : tt.soldCount}
                      </p>
                      <p className="text-muted-foreground">
                        {tt.quantity ? 'продано' : 'без лимита'}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 text-muted-foreground">
                <Ticket className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p>Типы билетов не добавлены</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Список регистраций */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            Участники
          </CardTitle>
          <CardDescription>Список зарегистрированных участников</CardDescription>
        </CardHeader>
        <CardContent>
          <RegistrationList eventId={id} />
        </CardContent>
      </Card>

      {/* История изменений */}
      <EventActivityLog eventId={id} />
    </div>
  );
}

// Skeleton для загрузки
function EventDetailSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-4">
        <Skeleton className="h-10 w-10" />
        <div className="space-y-2">
          <Skeleton className="h-8 w-[300px]" />
          <Skeleton className="h-4 w-[200px]" />
        </div>
      </div>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} className="h-[100px]" />
        ))}
      </div>
      <div className="grid gap-6 md:grid-cols-2">
        <Skeleton className="h-[300px]" />
        <Skeleton className="h-[300px]" />
      </div>
    </div>
  );
}
