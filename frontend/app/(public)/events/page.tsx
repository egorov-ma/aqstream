import Link from 'next/link';
import { Metadata } from 'next';
import { ChevronLeft, ChevronRight, CalendarDays } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EventGrid } from '@/components/features/public-event';
import { eventsApi } from '@/lib/api/events';
import type { PublicEventSummary } from '@/lib/api/types';

export const metadata: Metadata = {
  title: 'События | AqStream',
  description: 'Список предстоящих публичных мероприятий',
};

interface EventsPageProps {
  searchParams: Promise<{ page?: string }>;
}

const PAGE_SIZE = 12;

/**
 * Загружает события для текущей страницы
 */
async function getEvents(page: number): Promise<{
  events: PublicEventSummary[];
  totalPages: number;
  currentPage: number;
  hasNext: boolean;
  hasPrevious: boolean;
}> {
  try {
    const data = await eventsApi.listPublic({ page, size: PAGE_SIZE });
    return {
      events: data.content,
      totalPages: data.totalPages,
      currentPage: data.page,
      hasNext: data.hasNext ?? false,
      hasPrevious: data.hasPrevious ?? false,
    };
  } catch {
    return {
      events: [],
      totalPages: 0,
      currentPage: 0,
      hasNext: false,
      hasPrevious: false,
    };
  }
}

export default async function EventsPage({ searchParams }: EventsPageProps) {
  const params = await searchParams;
  const page = Math.max(0, parseInt(params.page ?? '0', 10) || 0);
  const { events, totalPages, currentPage, hasNext, hasPrevious } = await getEvents(page);

  return (
    <div className="container py-8">
      {/* Заголовок */}
      <div className="flex items-center gap-3 mb-8">
        <div className="p-2 rounded-lg bg-primary/10">
          <CalendarDays className="h-6 w-6 text-primary" />
        </div>
        <div>
          <h1 className="text-3xl font-bold">События</h1>
          <p className="text-muted-foreground">
            Предстоящие публичные мероприятия
          </p>
        </div>
      </div>

      {/* Список событий */}
      {events.length > 0 ? (
        <>
          <EventGrid events={events} />

          {/* Пагинация */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-8">
              <Button
                variant="outline"
                size="sm"
                asChild
                disabled={!hasPrevious}
              >
                <Link
                  href={`/events?page=${currentPage - 1}`}
                  className={!hasPrevious ? 'pointer-events-none opacity-50' : ''}
                >
                  <ChevronLeft className="h-4 w-4 mr-1" />
                  Назад
                </Link>
              </Button>

              <span className="text-sm text-muted-foreground">
                Страница {currentPage + 1} из {totalPages}
              </span>

              <Button
                variant="outline"
                size="sm"
                asChild
                disabled={!hasNext}
              >
                <Link
                  href={`/events?page=${currentPage + 1}`}
                  className={!hasNext ? 'pointer-events-none opacity-50' : ''}
                >
                  Вперёд
                  <ChevronRight className="h-4 w-4 ml-1" />
                </Link>
              </Button>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-16">
          <CalendarDays className="h-16 w-16 text-muted-foreground/30 mx-auto mb-4" />
          <h2 className="text-xl font-semibold mb-2">Нет предстоящих событий</h2>
          <p className="text-muted-foreground mb-6">
            Пока нет публичных мероприятий. Загляните позже!
          </p>
          <Button asChild>
            <Link href="/">На главную</Link>
          </Button>
        </div>
      )}
    </div>
  );
}
