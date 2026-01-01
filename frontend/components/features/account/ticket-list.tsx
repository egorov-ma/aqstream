'use client';

import { useMemo } from 'react';
import Link from 'next/link';
import { Calendar, XCircle } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';

import { TicketCard } from './ticket-card';
import { formatEventDate } from '@/lib/utils/date-time';
import {
  getStatusBadgeVariant,
  getStatusLabel,
  canCancelRegistration,
} from '@/lib/utils/registration';
import type { Registration } from '@/lib/api/types';

interface TicketListProps {
  registrations: Registration[];
  onCancel: (id: string) => void;
  isCancelling: boolean;
}

interface EmptyStateProps {
  type: 'upcoming' | 'past' | 'cancelled';
}

function EmptyState({ type }: EmptyStateProps) {
  const messages = {
    upcoming: 'У вас нет предстоящих регистраций',
    past: 'У вас нет прошедших событий',
    cancelled: 'У вас нет отменённых регистраций',
  };

  return (
    <div className="py-12 text-center">
      <Calendar className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
      <p className="text-muted-foreground mb-4">{messages[type]}</p>
      {type === 'upcoming' && (
        <Button asChild>
          <Link href="/events">Найти события</Link>
        </Button>
      )}
    </div>
  );
}

interface TicketTableProps {
  registrations: Registration[];
  onCancel: (id: string) => void;
  isCancelling: boolean;
}

/**
 * Таблица билетов для desktop.
 */
function TicketTable({ registrations, onCancel, isCancelling }: TicketTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Событие</TableHead>
          <TableHead>Дата события</TableHead>
          <TableHead>Тип билета</TableHead>
          <TableHead>Код</TableHead>
          <TableHead>Статус</TableHead>
          <TableHead className="text-right">Действия</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {registrations.map((registration) => {
          const canCancel = canCancelRegistration(registration.status);
          return (
            <TableRow key={registration.id}>
              <TableCell className="font-medium">
                <Link
                  href={`/events/${registration.eventSlug}`}
                  className="hover:underline"
                >
                  {registration.eventTitle}
                </Link>
              </TableCell>
              <TableCell>
                {formatEventDate(registration.eventStartsAt)}
              </TableCell>
              <TableCell>{registration.ticketTypeName}</TableCell>
              <TableCell>
                <code className="font-mono text-sm bg-muted px-2 py-1 rounded">
                  {registration.confirmationCode}
                </code>
              </TableCell>
              <TableCell>
                <Badge variant={getStatusBadgeVariant(registration.status)}>
                  {getStatusLabel(registration.status)}
                </Badge>
              </TableCell>
              <TableCell className="text-right space-x-2">
                <Button variant="ghost" size="sm" asChild>
                  <Link href={`/dashboard/my-registrations/${registration.id}`}>
                    Подробнее
                  </Link>
                </Button>
                {canCancel && (
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive hover:text-destructive"
                        disabled={isCancelling}
                      >
                        <XCircle className="h-4 w-4 mr-1" />
                        Отменить
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Отменить регистрацию?</AlertDialogTitle>
                        <AlertDialogDescription>
                          Вы уверены, что хотите отменить регистрацию на событие &quot;
                          {registration.eventTitle}&quot;? Это действие нельзя отменить.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Нет, оставить</AlertDialogCancel>
                        <AlertDialogAction
                          onClick={() => onCancel(registration.id)}
                          className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                          Да, отменить
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                )}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}

interface TicketGridProps {
  registrations: Registration[];
  onCancel: (id: string) => void;
  isCancelling: boolean;
}

/**
 * Сетка карточек билетов для мобильных.
 */
function TicketGrid({ registrations, onCancel, isCancelling }: TicketGridProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {registrations.map((registration) => (
        <TicketCard
          key={registration.id}
          registration={registration}
          onCancel={onCancel}
          isCancelling={isCancelling}
        />
      ))}
    </div>
  );
}

/**
 * Адаптивный список билетов с табами.
 * Показывает карточки на мобильных устройствах и таблицу на desktop.
 */
export function TicketList({ registrations, onCancel, isCancelling }: TicketListProps) {
  // Фильтрация регистраций по категориям
  const { upcoming, past, cancelled } = useMemo(() => {
    const now = new Date();

    const upcomingRegs = registrations.filter(r =>
      ['CONFIRMED', 'RESERVED', 'PENDING'].includes(r.status) &&
      new Date(r.eventStartsAt) > now
    );

    const pastRegs = registrations.filter(r =>
      r.status === 'CONFIRMED' &&
      new Date(r.eventStartsAt) <= now
    );

    const cancelledRegs = registrations.filter(r =>
      ['CANCELLED', 'EXPIRED'].includes(r.status)
    );

    return { upcoming: upcomingRegs, past: pastRegs, cancelled: cancelledRegs };
  }, [registrations]);

  // Рендер списка регистраций (адаптивный)
  const renderRegistrations = (regs: Registration[], emptyType: 'upcoming' | 'past' | 'cancelled') => {
    if (regs.length === 0) {
      return <EmptyState type={emptyType} />;
    }

    return (
      <>
        {/* Карточки для мобильных (< md) */}
        <div className="block md:hidden">
          <TicketGrid
            registrations={regs}
            onCancel={onCancel}
            isCancelling={isCancelling}
          />
        </div>
        {/* Таблица для desktop (>= md) */}
        <div className="hidden md:block">
          <TicketTable
            registrations={regs}
            onCancel={onCancel}
            isCancelling={isCancelling}
          />
        </div>
      </>
    );
  };

  return (
    <Tabs defaultValue="upcoming" className="w-full">
      <TabsList className="grid w-full grid-cols-3 mb-4">
        <TabsTrigger value="upcoming">
          Предстоящие ({upcoming.length})
        </TabsTrigger>
        <TabsTrigger value="past">
          Прошедшие ({past.length})
        </TabsTrigger>
        <TabsTrigger value="cancelled">
          Отменённые ({cancelled.length})
        </TabsTrigger>
      </TabsList>

      <TabsContent value="upcoming">
        {renderRegistrations(upcoming, 'upcoming')}
      </TabsContent>

      <TabsContent value="past">
        {renderRegistrations(past, 'past')}
      </TabsContent>

      <TabsContent value="cancelled">
        {renderRegistrations(cancelled, 'cancelled')}
      </TabsContent>
    </Tabs>
  );
}
