import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ArrowLeft } from 'lucide-react';

interface EditEventPageProps {
  params: Promise<{
    id: string;
  }>;
}

export async function generateMetadata({ params }: EditEventPageProps): Promise<Metadata> {
  const { id } = await params;
  return {
    title: `Редактирование события - AqStream`,
    description: 'Редактирование мероприятия',
  };
}

export default async function EditEventPage({ params }: EditEventPageProps) {
  const { id } = await params;

  // Placeholder — в Phase 2 здесь будет загрузка данных события
  if (!id) {
    notFound();
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link href={`/dashboard/events/${id}`}>
            <ArrowLeft className="h-4 w-4" />
            <span className="sr-only">Назад</span>
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">Редактирование события</h1>
      </div>

      {/* Placeholder форма — будет реализована в Phase 2 */}
      <Card className="max-w-2xl">
        <CardHeader>
          <CardTitle>Информация о событии</CardTitle>
          <CardDescription>Редактирование данных мероприятия</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">Название</Label>
            <Input id="title" placeholder="Название мероприятия" disabled />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Описание</Label>
            <Input id="description" placeholder="Описание мероприятия" disabled />
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="date">Дата</Label>
              <Input id="date" type="date" disabled />
            </div>
            <div className="space-y-2">
              <Label htmlFor="time">Время</Label>
              <Input id="time" type="time" disabled />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="location">Место проведения</Label>
            <Input id="location" placeholder="Адрес или онлайн" disabled />
          </div>

          <div className="flex gap-4 pt-4">
            <Button disabled>Сохранить</Button>
            <Button variant="outline" asChild>
              <Link href={`/dashboard/events/${id}`}>Отмена</Link>
            </Button>
          </div>

          <p className="text-sm text-muted-foreground">
            Форма будет полностью функциональна в Phase 2.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
