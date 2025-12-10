import type { Metadata } from 'next';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PlusCircle, Calendar } from 'lucide-react';

export const metadata: Metadata = {
  title: 'События - AqStream',
  description: 'Управление событиями',
};

export default function EventsPage() {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">События</h1>
        <Button asChild>
          <Link href="/dashboard/events/new">
            <PlusCircle className="mr-2 h-4 w-4" />
            Создать событие
          </Link>
        </Button>
      </div>

      {/* Placeholder для списка событий — будет реализовано в Phase 2 */}
      <Card>
        <CardHeader>
          <CardTitle>Ваши события</CardTitle>
          <CardDescription>Список всех созданных мероприятий</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-12">
            <Calendar className="h-12 w-12 text-muted-foreground" />
            <p className="mt-4 text-lg font-medium">У вас пока нет событий</p>
            <p className="mt-2 text-sm text-muted-foreground">
              Создайте первое событие, чтобы начать работу
            </p>
            <Button className="mt-4" asChild>
              <Link href="/dashboard/events/new">
                <PlusCircle className="mr-2 h-4 w-4" />
                Создать событие
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
