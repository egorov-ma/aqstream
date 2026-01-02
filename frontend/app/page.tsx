import { PublicHeader } from '@/components/layout/public-header';
import { UpcomingEventsSection, HeroActions } from '@/components/features/home';
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
    <div className="flex min-h-screen flex-col">
      {/* Публичный хедер с условной навигацией */}
      <PublicHeader />

      <main className="flex flex-1 flex-col">
        {/* Hero Section */}
        <section className="flex flex-1 flex-col items-center justify-center p-8 md:p-24">
          <div className="text-center max-w-3xl">
            <h1 className="mb-4 text-4xl md:text-5xl font-bold">AqStream</h1>
            <p className="mb-8 text-xl text-muted-foreground">
              Платформа для управления мероприятиями
            </p>

            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <HeroActions />
            </div>
          </div>
        </section>

        {/* Предстоящие события */}
        <UpcomingEventsSection events={events} hasMore={hasMore} />
      </main>

      {/* Футер */}
      <footer className="border-t py-6">
        <div className="container text-center text-sm text-muted-foreground">
          AqStream — платформа для управления мероприятиями
        </div>
      </footer>
    </div>
  );
}
