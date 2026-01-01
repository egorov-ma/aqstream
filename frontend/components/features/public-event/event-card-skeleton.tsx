import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

/**
 * Skeleton карточки события для состояния загрузки
 */
export function EventCardSkeleton() {
  return (
    <Card className="h-full overflow-hidden">
      {/* Обложка */}
      <Skeleton className="h-48 w-full rounded-none" />

      <CardContent className="p-4">
        {/* Название */}
        <Skeleton className="h-6 w-3/4 mb-2" />
        <Skeleton className="h-6 w-1/2 mb-2" />

        {/* Описание */}
        <Skeleton className="h-4 w-full mb-1" />
        <Skeleton className="h-4 w-4/5 mb-3" />

        {/* Мета */}
        <div className="space-y-2">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-4 w-40" />
        </div>
      </CardContent>
    </Card>
  );
}
