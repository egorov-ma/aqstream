import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, Edit, Users, Calendar, MapPin } from 'lucide-react';

interface EventDetailPageProps {
  params: Promise<{
    id: string;
  }>;
}

export async function generateMetadata({ params }: EventDetailPageProps): Promise<Metadata> {
  const { id } = await params;
  return {
    title: `Событие ${id} - AqStream`,
    description: 'Детали мероприятия',
  };
}

export default async function EventDetailPage({ params }: EventDetailPageProps) {
  const { id } = await params;

  // Placeholder — в Phase 2 здесь будет загрузка данных события
  if (!id) {
    notFound();
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/dashboard/events">
              <ArrowLeft className="h-4 w-4" />
              <span className="sr-only">Назад</span>
            </Link>
          </Button>
          <div>
            <h1 className="text-2xl font-bold">Событие {id}</h1>
            <p className="text-sm text-muted-foreground">ID: {id}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="secondary">Черновик</Badge>
          <Button variant="outline" asChild>
            <Link href={`/dashboard/events/${id}/edit`}>
              <Edit className="mr-2 h-4 w-4" />
              Редактировать
            </Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {/* Информация о событии */}
        <Card>
          <CardHeader>
            <CardTitle>Информация</CardTitle>
            <CardDescription>Основные данные о мероприятии</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-2 text-sm">
              <Calendar className="h-4 w-4 text-muted-foreground" />
              <span className="text-muted-foreground">Дата не указана</span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <MapPin className="h-4 w-4 text-muted-foreground" />
              <span className="text-muted-foreground">Место не указано</span>
            </div>
            <p className="text-sm text-muted-foreground">
              Описание мероприятия будет отображаться здесь.
            </p>
          </CardContent>
        </Card>

        {/* Статистика регистраций */}
        <Card>
          <CardHeader>
            <CardTitle>Регистрации</CardTitle>
            <CardDescription>Статистика участников</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <Users className="h-8 w-8 text-muted-foreground" />
              <div>
                <p className="text-2xl font-bold">0</p>
                <p className="text-sm text-muted-foreground">Зарегистрировано</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Placeholder для списка регистраций */}
      <Card>
        <CardHeader>
          <CardTitle>Участники</CardTitle>
          <CardDescription>Список зарегистрированных участников</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="rounded-lg border border-dashed p-8 text-center">
            <p className="text-muted-foreground">
              Список участников будет отображаться здесь после реализации в Phase 2.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
