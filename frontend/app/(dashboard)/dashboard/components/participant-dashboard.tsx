'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { CalendarDays, Search, Building2, Ticket, ArrowRight } from 'lucide-react';
import { useAuthStore } from '@/lib/store/auth-store';
import { useMyRegistrations } from '@/lib/hooks/use-registrations';
import type { Registration } from '@/lib/api/types';

/**
 * Дашборд для обычного пользователя (участника без организации).
 * Показывает предстоящие события и CTA для поиска событий / создания организации.
 */
export function ParticipantDashboard() {
  const user = useAuthStore((state) => state.user);
  const { data: registrations, isLoading } = useMyRegistrations({ size: 20 });

  // Фильтруем только предстоящие события (активные статусы + дата в будущем)
  const upcomingRegistrations = useMemo(() => {
    if (!registrations?.data) return [];

    const now = new Date();
    const activeStatuses = ['CONFIRMED', 'RESERVED', 'PENDING'];

    return registrations.data
      .filter(
        (reg) => activeStatuses.includes(reg.status) && new Date(reg.eventStartsAt) > now
      )
      .sort((a, b) => new Date(a.eventStartsAt).getTime() - new Date(b.eventStartsAt).getTime())
      .slice(0, 5);
  }, [registrations]);

  if (isLoading) {
    return <ParticipantDashboardSkeleton />;
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Приветствие */}
      <div>
        <h1 className="text-2xl font-bold">
          Добро пожаловать{user?.firstName ? `, ${user.firstName}` : ''}!
        </h1>
        <p className="text-muted-foreground">Ваш личный кабинет в AqStream</p>
      </div>

      {/* Предстоящие события */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CalendarDays className="h-5 w-5" />
            Мои предстоящие события
          </CardTitle>
          <CardDescription>События, на которые вы зарегистрированы</CardDescription>
        </CardHeader>
        <CardContent>
          {upcomingRegistrations.length > 0 ? (
            <div className="space-y-3">
              {upcomingRegistrations.map((registration) => (
                <UpcomingEventItem key={registration.id} registration={registration} />
              ))}
              <div className="pt-2">
                <Button variant="outline" asChild className="w-full">
                  <Link href="/dashboard/my-registrations" data-testid="all-tickets-link">
                    <Ticket className="mr-2 h-4 w-4" />
                    Все мои билеты
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Link>
                </Button>
              </div>
            </div>
          ) : (
            <EmptyUpcomingState />
          )}
        </CardContent>
      </Card>

      {/* CTA карточки */}
      <div className="grid gap-4 md:grid-cols-2">
        {/* Найти события */}
        <Card className="border-dashed">
          <CardContent className="pt-6 text-center">
            <Search className="h-10 w-10 mx-auto mb-3 text-primary" />
            <h3 className="font-semibold mb-2">Найдите интересные события</h3>
            <p className="text-sm text-muted-foreground mb-4">
              Просмотрите каталог публичных мероприятий
            </p>
            <Button asChild data-testid="find-events-button">
              <Link href="/events">Смотреть события</Link>
            </Button>
          </CardContent>
        </Card>

        {/* Стать организатором */}
        <Card className="border-dashed">
          <CardContent className="pt-6 text-center">
            <Building2 className="h-10 w-10 mx-auto mb-3 text-primary" />
            <h3 className="font-semibold mb-2">Станьте организатором</h3>
            <p className="text-sm text-muted-foreground mb-4">
              Создавайте события и управляйте регистрациями
            </p>
            <Button variant="outline" asChild data-testid="create-organization-button">
              <Link href="/dashboard/organization-request">Подать заявку</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

interface UpcomingEventItemProps {
  registration: Registration;
}

function UpcomingEventItem({ registration }: UpcomingEventItemProps) {
  return (
    <Link
      href={`/events/${registration.eventSlug}`}
      className="flex items-center gap-4 p-3 rounded-lg border hover:bg-accent transition-colors"
      data-testid={`upcoming-event-${registration.id}`}
    >
      <CalendarDays className="h-8 w-8 text-primary flex-shrink-0" />
      <div className="min-w-0 flex-1">
        <h3 className="font-medium truncate">{registration.eventTitle}</h3>
        <p className="text-sm text-muted-foreground">
          {format(new Date(registration.eventStartsAt), 'dd MMMM yyyy, HH:mm', { locale: ru })}
        </p>
        <p className="text-xs text-muted-foreground">{registration.ticketTypeName}</p>
      </div>
    </Link>
  );
}

function EmptyUpcomingState() {
  return (
    <div className="text-center py-6">
      <CalendarDays className="h-12 w-12 mx-auto mb-3 text-muted-foreground" />
      <h3 className="font-medium mb-1">Нет предстоящих событий</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Вы пока не зарегистрированы на события
      </p>
      <Button variant="outline" asChild>
        <Link href="/events">Найти события</Link>
      </Button>
    </div>
  );
}

function ParticipantDashboardSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <Skeleton className="h-8 w-64 mb-2" />
        <Skeleton className="h-4 w-48" />
      </div>
      <Skeleton className="h-64" />
      <div className="grid gap-4 md:grid-cols-2">
        <Skeleton className="h-48" />
        <Skeleton className="h-48" />
      </div>
    </div>
  );
}
