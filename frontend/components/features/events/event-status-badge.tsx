'use client';

import { Badge } from '@/components/ui/badge';
import type { EventStatus } from '@/lib/api/types';
import { cn } from '@/lib/utils';

interface EventStatusBadgeProps {
  status: EventStatus;
  className?: string;
}

// Конфигурация статусов: лейбл и стили
const statusConfig: Record<
  EventStatus,
  { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }
> = {
  DRAFT: { label: 'Черновик', variant: 'secondary' },
  PUBLISHED: { label: 'Опубликовано', variant: 'default' },
  CANCELLED: { label: 'Отменено', variant: 'destructive' },
  COMPLETED: { label: 'Завершено', variant: 'outline' },
};

export function EventStatusBadge({ status, className }: EventStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <Badge
      variant={config.variant}
      className={cn(
        // Дополнительные стили для PUBLISHED (зелёный)
        status === 'PUBLISHED' && 'bg-green-500 hover:bg-green-600',
        // Дополнительные стили для COMPLETED (синий контур)
        status === 'COMPLETED' && 'border-blue-500 text-blue-600',
        className
      )}
    >
      {config.label}
    </Badge>
  );
}

// Хелпер для получения лейбла статуса
export function getEventStatusLabel(status: EventStatus): string {
  return statusConfig[status]?.label ?? status;
}

// Хелпер для проверки возможности редактирования
export function isEventEditable(status: EventStatus): boolean {
  return status === 'DRAFT' || status === 'PUBLISHED';
}

// Хелпер для проверки возможности публикации
export function canPublishEvent(status: EventStatus): boolean {
  return status === 'DRAFT';
}

// Хелпер для проверки возможности отмены
export function canCancelEvent(status: EventStatus): boolean {
  return status === 'DRAFT' || status === 'PUBLISHED';
}
