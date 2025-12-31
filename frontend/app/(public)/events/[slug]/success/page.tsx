import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { CheckCircle, MessageSquare, Calendar, ArrowLeft } from 'lucide-react';

import { eventsApi } from '@/lib/api/events';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { AddToCalendarButton } from '@/components/features/public-event';
import { formatEventDateLocal } from '@/lib/utils/seo';
import { getEventLocation } from '@/lib/utils/event';
import { ROUTES } from '@/lib/routes';
import type { Event } from '@/lib/api/types';

interface SuccessPageProps {
  params: Promise<{
    slug: string;
  }>;
  searchParams: Promise<{
    code?: string;
  }>;
}

/**
 * Получает событие по slug на сервере
 */
async function getEvent(slug: string): Promise<Event | null> {
  try {
    return await eventsApi.getBySlug(slug);
  } catch {
    return null;
  }
}

/**
 * Генерирует метаданные страницы
 */
export async function generateMetadata({ params }: SuccessPageProps): Promise<Metadata> {
  const { slug } = await params;
  const event = await getEvent(slug);

  if (!event) {
    return {
      title: 'Событие не найдено - AqStream',
    };
  }

  return {
    title: `Регистрация успешна - ${event.title} - AqStream`,
    description: `Вы успешно зарегистрировались на событие "${event.title}"`,
    robots: {
      index: false, // Не индексируем страницу успеха
    },
  };
}

export default async function SuccessPage({ params, searchParams }: SuccessPageProps) {
  const { slug } = await params;
  const { code } = await searchParams;

  const event = await getEvent(slug);

  // Событие не найдено
  if (!event) {
    notFound();
  }

  // Код подтверждения не передан — редирект на страницу события
  if (!code) {
    notFound();
  }

  // Определяем местоположение (используем утилиту для consistency)
  const locationText = getEventLocation(event);

  return (
    <div className="min-h-screen bg-background">
      <div className="container py-12">
        <Card className="max-w-md mx-auto">
          <CardHeader className="text-center">
            <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
            <CardTitle className="text-2xl">Регистрация успешна!</CardTitle>
          </CardHeader>

          <CardContent className="space-y-6">
            {/* Confirmation Code */}
            <div className="text-center">
              <p className="text-sm text-muted-foreground">Код вашего билета:</p>
              <p
                className="text-3xl font-mono font-bold tracking-wider mt-1"
                data-testid="confirmation-code"
              >
                {code}
              </p>
            </div>

            <Separator />

            {/* Event Details */}
            <div className="space-y-2">
              <h3 className="font-semibold text-lg">{event.title}</h3>
              <div className="text-sm text-muted-foreground space-y-1">
                <p className="flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  {formatEventDateLocal(event.startsAt)}
                </p>
                <p>{locationText}</p>
              </div>
            </div>

            {/* Telegram Message */}
            <Alert>
              <MessageSquare className="h-4 w-4" />
              <AlertDescription>
                Билет с QR-кодом отправлен в Telegram. Если у вас не привязан Telegram —
                сохраните код билета выше.
              </AlertDescription>
            </Alert>

            {/* Actions */}
            <div className="space-y-3">
              <AddToCalendarButton event={event} />

              <Button variant="outline" className="w-full" asChild>
                <Link href={ROUTES.EVENT(slug)}>
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  Вернуться к событию
                </Link>
              </Button>

              <Button variant="ghost" className="w-full" asChild>
                <Link href={ROUTES.MY_REGISTRATIONS}>Мои регистрации</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
