'use client';

import Link from 'next/link';
import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';
import { CalendarDays, MapPin, Users } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import type { Event, PageResponse } from '@/lib/api/types';
import { EventStatusBadge } from './event-status-badge';
import { EventActions } from './event-actions';

interface EventListProps {
  data?: PageResponse<Event>;
  isLoading?: boolean;
  onPageChange?: (page: number) => void;
}

// Форматирование даты
function formatDate(isoString: string): string {
  const date = parseISO(isoString);
  return format(date, 'd MMM yyyy, HH:mm', { locale: ru });
}

// Получение локации для отображения
function getLocationDisplay(event: Event): string {
  if (event.locationType === 'ONLINE') {
    return 'Онлайн';
  }
  if (event.locationAddress) {
    return event.locationAddress;
  }
  return event.locationType === 'HYBRID' ? 'Гибрид' : '—';
}

// Skeleton для загрузки
function EventListSkeleton() {
  return (
    <div className="space-y-4">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex items-center space-x-4 p-4">
          <Skeleton className="h-12 w-12 rounded" />
          <div className="space-y-2 flex-1">
            <Skeleton className="h-4 w-[250px]" />
            <Skeleton className="h-4 w-[200px]" />
          </div>
          <Skeleton className="h-6 w-20" />
          <Skeleton className="h-8 w-8" />
        </div>
      ))}
    </div>
  );
}

// Пустое состояние
function EmptyState() {
  return (
    <div className="text-center py-12">
      <CalendarDays className="mx-auto h-12 w-12 text-muted-foreground" />
      <h3 className="mt-4 text-lg font-semibold">Нет событий</h3>
      <p className="mt-2 text-sm text-muted-foreground">
        Создайте первое событие, чтобы начать работу
      </p>
      <Button asChild className="mt-4">
        <Link href="/dashboard/events/new">Создать событие</Link>
      </Button>
    </div>
  );
}

export function EventList({ data, isLoading, onPageChange }: EventListProps) {
  if (isLoading) {
    return <EventListSkeleton />;
  }

  if (!data || data.content.length === 0) {
    return <EmptyState />;
  }

  return (
    <div className="space-y-4">
      <div className="rounded-md border" data-testid="events-table">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[300px]">Название</TableHead>
              <TableHead>Дата</TableHead>
              <TableHead>Локация</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead className="text-right">Регистрации</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.map((event) => (
              <TableRow key={event.id} data-testid={`event-row-${event.id}`}>
                <TableCell>
                  <Link
                    href={`/dashboard/events/${event.id}`}
                    className="font-medium hover:underline"
                  >
                    {event.title}
                  </Link>
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-1 text-sm text-muted-foreground">
                    <CalendarDays className="h-4 w-4" />
                    {formatDate(event.startsAt)}
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-1 text-sm text-muted-foreground">
                    <MapPin className="h-4 w-4" />
                    <span className="truncate max-w-[200px]">
                      {getLocationDisplay(event)}
                    </span>
                  </div>
                </TableCell>
                <TableCell>
                  <EventStatusBadge status={event.status} />
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex items-center justify-end gap-1 text-sm text-muted-foreground">
                    <Users className="h-4 w-4" />
                    {/* TODO: добавить количество регистраций */}
                    0
                  </div>
                </TableCell>
                <TableCell>
                  <EventActions event={event} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Пагинация */}
      {data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Показано {data.content.length} из {data.totalElements}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange?.(data.page - 1)}
              disabled={!data.hasPrevious}
            >
              Назад
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange?.(data.page + 1)}
              disabled={!data.hasNext}
            >
              Вперёд
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
