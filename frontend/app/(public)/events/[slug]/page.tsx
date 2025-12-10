import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Calendar, MapPin, Users } from 'lucide-react';

interface EventPageProps {
  params: Promise<{
    slug: string;
  }>;
}

export async function generateMetadata({ params }: EventPageProps): Promise<Metadata> {
  const { slug } = await params;

  // В будущем здесь будет загрузка данных события по slug
  return {
    title: `${slug} - AqStream`,
    description: 'Страница мероприятия',
  };
}

export default async function EventPage({ params }: EventPageProps) {
  const { slug } = await params;

  // Placeholder — в будущем здесь будет загрузка данных события
  if (!slug) {
    notFound();
  }

  return (
    <div className="container py-12">
      <div className="mx-auto max-w-3xl">
        {/* Информация о событии */}
        <Card className="mb-8">
          <CardHeader>
            <div className="flex items-start justify-between">
              <div>
                <Badge variant="secondary" className="mb-2">
                  Черновик
                </Badge>
                <CardTitle className="text-3xl">{slug}</CardTitle>
                <CardDescription>Описание мероприятия будет здесь</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Calendar className="h-4 w-4" />
              <span>Дата не указана</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <MapPin className="h-4 w-4" />
              <span>Место не указано</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Users className="h-4 w-4" />
              <span>0 участников</span>
            </div>
          </CardContent>
        </Card>

        {/* Форма регистрации */}
        <Card>
          <CardHeader>
            <CardTitle>Регистрация</CardTitle>
            <CardDescription>Заполните форму для участия в мероприятии</CardDescription>
          </CardHeader>
          <CardContent className="text-center">
            <p className="mb-6 text-muted-foreground">
              Форма регистрации будет доступна после реализации в Phase 2.
            </p>
            <Button size="lg" disabled>
              Зарегистрироваться
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
