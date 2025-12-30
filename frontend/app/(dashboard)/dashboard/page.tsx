'use client';

import Link from 'next/link';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { CalendarDays, Users, BarChart3, AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useDashboardStats } from '@/lib/hooks/use-dashboard';

export default function DashboardPage() {
  const { data: stats, isLoading, isError, refetch } = useDashboardStats();

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  if (isError) {
    return (
      <div className="flex flex-col gap-4">
        <h1 className="text-2xl font-bold">Обзор</h1>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-10">
            <AlertCircle className="h-10 w-10 text-destructive mb-4" />
            <p className="text-muted-foreground mb-4">Не удалось загрузить данные</p>
            <Button variant="outline" onClick={() => refetch()}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Попробовать снова
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Обзор</h1>

      {/* Карточки статистики */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">События</CardTitle>
            <CalendarDays className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.activeEventsCount ?? 0}</div>
            <p className="text-xs text-muted-foreground">Активных событий</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Регистрации</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalRegistrations ?? 0}</div>
            <p className="text-xs text-muted-foreground">За последние 30 дней</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Посещаемость</CardTitle>
            <BarChart3 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.attendanceRate ?? 0}%</div>
            <p className="text-xs text-muted-foreground">Средний показатель</p>
          </CardContent>
        </Card>
      </div>

      {/* Ближайшие события */}
      {stats?.upcomingEvents && stats.upcomingEvents.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Ближайшие события</CardTitle>
            <CardDescription>Предстоящие мероприятия вашей организации</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {stats.upcomingEvents.map((event) => (
                <Link
                  key={event.id}
                  href={`/dashboard/events/${event.id}`}
                  className="flex items-center justify-between p-4 rounded-lg border hover:bg-accent transition-colors"
                >
                  <div className="flex items-center gap-4">
                    <CalendarDays className="h-8 w-8 text-primary" />
                    <div>
                      <h3 className="font-semibold">{event.title}</h3>
                      <p className="text-sm text-muted-foreground">
                        {format(new Date(event.startsAt), 'dd MMMM yyyy, HH:mm', { locale: ru })}
                      </p>
                    </div>
                  </div>
                  {event.location && (
                    <span className="text-sm text-muted-foreground hidden md:block">
                      {event.location}
                    </span>
                  )}
                </Link>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Быстрые действия */}
      <Card>
        <CardHeader>
          <CardTitle>Быстрые действия</CardTitle>
          <CardDescription>Начните работу с AqStream</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <Link
            href="/dashboard/events/new"
            className="flex items-center gap-4 rounded-lg border p-4 hover:bg-accent transition-colors"
          >
            <CalendarDays className="h-8 w-8 text-primary" />
            <div>
              <h3 className="font-semibold">Создать событие</h3>
              <p className="text-sm text-muted-foreground">Создайте новое мероприятие</p>
            </div>
          </Link>
          <Link
            href="/dashboard/events"
            className="flex items-center gap-4 rounded-lg border p-4 hover:bg-accent transition-colors"
          >
            <Users className="h-8 w-8 text-primary" />
            <div>
              <h3 className="font-semibold">Управление событиями</h3>
              <p className="text-sm text-muted-foreground">Просмотр и редактирование событий</p>
            </div>
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="flex flex-col gap-4">
      <Skeleton className="h-8 w-32" />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Skeleton className="h-32" />
        <Skeleton className="h-32" />
        <Skeleton className="h-32" />
      </div>
      <Skeleton className="h-64" />
    </div>
  );
}
