import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { UpcomingEventsSection } from '@/components/features/home';
import { serverEventsApi } from '@/lib/api/server';
import type { PublicEventSummary } from '@/lib/api/types';

/**
 * Загружает предстоящие публичные события на сервере.
 * При ошибке возвращает пустой массив (секция не отображается).
 */
async function getUpcomingEvents(): Promise<{
  events: PublicEventSummary[];
  hasMore: boolean;
}> {
  try {
    const data = await serverEventsApi.listPublic({ size: 6 });
    return {
      events: data.data,
      hasMore: data.hasNext ?? false,
    };
  } catch {
    // При ошибке API просто не показываем секцию
    return { events: [], hasMore: false };
  }
}

export default async function HomePage() {
  const { events, hasMore } = await getUpcomingEvents();

  return (
    <main className="flex min-h-screen flex-col">
      {/* Hero Section */}
      <section className="flex flex-1 flex-col items-center justify-center p-8 md:p-24">
        <div className="text-center max-w-3xl">
          <h1 className="mb-4 text-4xl md:text-5xl font-bold">AqStream</h1>
          <p className="mb-8 text-xl text-muted-foreground">
            Платформа для управления мероприятиями
          </p>

          <div className="flex flex-col sm:flex-row justify-center gap-4">
            <Button size="lg" asChild>
              <Link href="/login">Войти</Link>
            </Button>
            <Button size="lg" variant="outline" asChild>
              <Link href="/register">Регистрация</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Предстоящие события */}
      <UpcomingEventsSection events={events} hasMore={hasMore} />
    </main>
  );
}
