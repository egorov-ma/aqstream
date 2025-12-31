'use client';

import * as React from 'react';
import { Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { PlusCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useEvents } from '@/lib/hooks/use-events';
import type { EventStatus } from '@/lib/api/types';
import {
  EventFilters,
  EventList,
  type EventFiltersState,
} from '@/components/features/events';

// Компонент с useSearchParams должен быть обёрнут в Suspense
function EventsPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Читаем фильтры из URL
  const initialFilters: EventFiltersState = {
    search: searchParams.get('search') ?? '',
    status: (searchParams.get('status') as EventStatus | 'ALL') ?? 'ALL',
    dateFrom: searchParams.get('dateFrom') ?? undefined,
    dateTo: searchParams.get('dateTo') ?? undefined,
  };

  const [filters, setFilters] = React.useState<EventFiltersState>(initialFilters);
  const [page, setPage] = React.useState(
    parseInt(searchParams.get('page') ?? '0', 10)
  );

  // Запрос списка событий
  const { data, isLoading } = useEvents({
    search: filters.search || undefined,
    status: filters.status !== 'ALL' ? filters.status : undefined,
    startsAfter: filters.dateFrom || undefined,
    startsBefore: filters.dateTo || undefined,
    page,
    size: 20,
  });

  // Обновляем URL при изменении фильтров
  React.useEffect(() => {
    const params = new URLSearchParams();
    if (filters.search) params.set('search', filters.search);
    if (filters.status !== 'ALL') params.set('status', filters.status);
    if (filters.dateFrom) params.set('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.set('dateTo', filters.dateTo);
    if (page > 0) params.set('page', page.toString());

    const query = params.toString();
    router.replace(`/dashboard/events${query ? `?${query}` : ''}`, {
      scroll: false,
    });
  }, [filters, page, router]);

  const handleFiltersChange = (newFilters: EventFiltersState) => {
    setFilters(newFilters);
    setPage(0); // Сбрасываем страницу при изменении фильтров
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">События</h1>
          <p className="text-sm text-muted-foreground">
            Управление мероприятиями вашей организации
          </p>
        </div>
        <Button asChild>
          <Link href="/dashboard/events/new">
            <PlusCircle className="mr-2 h-4 w-4" />
            Создать событие
          </Link>
        </Button>
      </div>

      {/* Фильтры */}
      <EventFilters filters={filters} onFiltersChange={handleFiltersChange} />

      {/* Список событий */}
      <EventList data={data} isLoading={isLoading} onPageChange={setPage} />
    </div>
  );
}

// Fallback для Suspense
function EventsPageFallback() {
  return (
    <div className="flex flex-col gap-6">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <Skeleton className="h-8 w-32" />
          <Skeleton className="mt-1 h-4 w-64" />
        </div>
        <Skeleton className="h-10 w-40" />
      </div>
      {/* Фильтры */}
      <div className="flex gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-10 w-32" />
      </div>
      {/* Список */}
      <div className="space-y-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full" />
        ))}
      </div>
    </div>
  );
}

export default function EventsPage() {
  return (
    <Suspense fallback={<EventsPageFallback />}>
      <EventsPageContent />
    </Suspense>
  );
}
