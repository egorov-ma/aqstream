import type { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { CalendarDays, Users, BarChart3 } from 'lucide-react';

export const metadata: Metadata = {
  title: 'Обзор - AqStream',
  description: 'Панель управления AqStream',
};

export default function DashboardPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Обзор</h1>

      {/* Placeholder карточки статистики */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">События</CardTitle>
            <CalendarDays className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">0</div>
            <p className="text-xs text-muted-foreground">Активных событий</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Регистрации</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">0</div>
            <p className="text-xs text-muted-foreground">За последние 30 дней</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Посещаемость</CardTitle>
            <BarChart3 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">0%</div>
            <p className="text-xs text-muted-foreground">Средний показатель</p>
          </CardContent>
        </Card>
      </div>

      {/* Placeholder для быстрых действий */}
      <Card>
        <CardHeader>
          <CardTitle>Быстрые действия</CardTitle>
          <CardDescription>Начните работу с AqStream</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <Link
            href="/dashboard/events/new"
            className="flex items-center gap-4 rounded-lg border p-4 hover:bg-accent"
          >
            <CalendarDays className="h-8 w-8 text-primary" />
            <div>
              <h3 className="font-semibold">Создать событие</h3>
              <p className="text-sm text-muted-foreground">Создайте новое мероприятие</p>
            </div>
          </Link>
          <Link
            href="/dashboard/events"
            className="flex items-center gap-4 rounded-lg border p-4 hover:bg-accent"
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
