'use client';

import { Users, UserCheck, Ticket, TrendingUp } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import type { Event } from '@/lib/api/types';

interface EventStatsCardProps {
  event?: Event;
  registrationsCount?: number;
  checkedInCount?: number;
  ticketTypesCount?: number;
  isLoading?: boolean;
}

// Skeleton для загрузки
function StatsSkeleton() {
  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      {Array.from({ length: 4 }).map((_, i) => (
        <Card key={i}>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <Skeleton className="h-4 w-[100px]" />
            <Skeleton className="h-4 w-4" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-8 w-[60px]" />
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

export function EventStatsCard({
  event,
  registrationsCount = 0,
  checkedInCount = 0,
  ticketTypesCount = 0,
  isLoading,
}: EventStatsCardProps) {
  if (isLoading) {
    return <StatsSkeleton />;
  }

  // Рассчитываем процент посещаемости
  const attendanceRate =
    registrationsCount > 0
      ? Math.round((checkedInCount / registrationsCount) * 100)
      : 0;

  // Доступные места
  const availableSpots = event?.maxCapacity
    ? Math.max(0, event.maxCapacity - registrationsCount)
    : null;

  const stats = [
    {
      title: 'Регистрации',
      value: registrationsCount,
      description: event?.maxCapacity
        ? `из ${event.maxCapacity} мест`
        : 'без лимита',
      icon: Users,
    },
    {
      title: 'Прошли check-in',
      value: checkedInCount,
      description: `${attendanceRate}% от зарегистрированных`,
      icon: UserCheck,
    },
    {
      title: 'Типов билетов',
      value: ticketTypesCount,
      description: 'активных',
      icon: Ticket,
    },
    {
      title: availableSpots !== null ? 'Доступно мест' : 'Посещаемость',
      value: availableSpots !== null ? availableSpots : `${attendanceRate}%`,
      description:
        availableSpots !== null
          ? registrationsCount > 0
            ? 'осталось'
            : 'все места свободны'
          : 'от общего числа',
      icon: TrendingUp,
    },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      {stats.map((stat) => (
        <Card key={stat.title}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
            <stat.icon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stat.value}</div>
            <p className="text-xs text-muted-foreground">{stat.description}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
