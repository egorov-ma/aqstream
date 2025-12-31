'use client';

import { useState } from 'react';
import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';
import {
  History,
  Plus,
  Pencil,
  Send,
  EyeOff,
  XCircle,
  CheckCircle,
  Trash2,
  ChevronLeft,
  ChevronRight,
  User,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { useEventActivity } from '@/lib/hooks/use-events';
import type { EventAuditLog, EventAuditAction } from '@/lib/api/types';

interface EventActivityLogProps {
  eventId: string;
}

// Конфигурация действий
const ACTION_CONFIG: Record<
  EventAuditAction,
  { icon: React.ElementType; label: string; color: string }
> = {
  CREATED: { icon: Plus, label: 'Создано', color: 'text-green-600' },
  UPDATED: { icon: Pencil, label: 'Изменено', color: 'text-blue-600' },
  PUBLISHED: { icon: Send, label: 'Опубликовано', color: 'text-indigo-600' },
  UNPUBLISHED: { icon: EyeOff, label: 'Снято с публикации', color: 'text-amber-600' },
  CANCELLED: { icon: XCircle, label: 'Отменено', color: 'text-orange-600' },
  COMPLETED: { icon: CheckCircle, label: 'Завершено', color: 'text-emerald-600' },
  DELETED: { icon: Trash2, label: 'Удалено', color: 'text-red-600' },
};

// Названия полей на русском
const FIELD_NAMES: Record<string, string> = {
  title: 'Название',
  description: 'Описание',
  startsAt: 'Дата начала',
  endsAt: 'Дата окончания',
  timezone: 'Часовой пояс',
  locationType: 'Тип локации',
  locationAddress: 'Адрес',
  onlineUrl: 'Ссылка',
  maxCapacity: 'Макс. участников',
  registrationOpensAt: 'Открытие регистрации',
  registrationClosesAt: 'Закрытие регистрации',
  isPublic: 'Публичность',
  participantsVisibility: 'Видимость участников',
  groupId: 'Группа',
};

// Форматирование даты
function formatDate(isoString: string): string {
  const date = parseISO(isoString);
  return format(date, 'd MMM yyyy, HH:mm', { locale: ru });
}

// Skeleton для загрузки
function ActivityLogSkeleton() {
  return (
    <div className="space-y-4">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex items-start gap-3 p-3 border-b last:border-b-0">
          <Skeleton className="h-8 w-8 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-[200px]" />
            <Skeleton className="h-3 w-[150px]" />
          </div>
          <Skeleton className="h-3 w-[100px]" />
        </div>
      ))}
    </div>
  );
}

// Компонент одной записи
function ActivityItem({ entry }: { entry: EventAuditLog }) {
  const config = ACTION_CONFIG[entry.action];
  const Icon = config.icon;

  return (
    <div className="flex items-start gap-3 p-3 border-b last:border-b-0 hover:bg-muted/50 transition-colors">
      {/* Иконка действия */}
      <div className={`flex-shrink-0 p-2 rounded-full bg-muted ${config.color}`}>
        <Icon className="h-4 w-4" />
      </div>

      {/* Контент */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <Badge variant="secondary" className="font-medium">
            {config.label}
          </Badge>
          {entry.actorEmail && (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <User className="h-3 w-3" />
                    <span className="truncate max-w-[150px]">{entry.actorEmail}</span>
                  </div>
                </TooltipTrigger>
                <TooltipContent>
                  <p>{entry.actorEmail}</p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          )}
        </div>

        {/* Описание изменения */}
        {entry.description && (
          <p className="text-sm text-muted-foreground mt-1">{entry.description}</p>
        )}

        {/* Изменённые поля */}
        {entry.changedFields && Object.keys(entry.changedFields).length > 0 && (
          <div className="mt-2 text-xs space-y-1">
            {Object.entries(entry.changedFields).slice(0, 3).map(([field, change]) => (
              <div key={field} className="flex items-center gap-2 text-muted-foreground">
                <span className="font-medium">{FIELD_NAMES[field] || field}:</span>
                <span className="line-through text-red-500/70">
                  {change.from || '(пусто)'}
                </span>
                <span>&rarr;</span>
                <span className="text-green-600">
                  {change.to || '(пусто)'}
                </span>
              </div>
            ))}
            {Object.keys(entry.changedFields).length > 3 && (
              <span className="text-muted-foreground">
                +{Object.keys(entry.changedFields).length - 3} изменений
              </span>
            )}
          </div>
        )}
      </div>

      {/* Дата */}
      <div className="flex-shrink-0 text-xs text-muted-foreground">
        {formatDate(entry.createdAt)}
      </div>
    </div>
  );
}

// Основной компонент
export function EventActivityLog({ eventId }: EventActivityLogProps) {
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const { data, isLoading, isError } = useEventActivity(eventId, page, pageSize);

  const handlePrevPage = () => setPage((p) => Math.max(0, p - 1));
  const handleNextPage = () => {
    if (data && page < data.totalPages - 1) {
      setPage((p) => p + 1);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <History className="h-5 w-5" />
          История изменений
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading && <ActivityLogSkeleton />}

        {isError && (
          <div className="text-center py-8 text-muted-foreground">
            Не удалось загрузить историю изменений
          </div>
        )}

        {data && data.content.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">
            История изменений пуста
          </div>
        )}

        {data && data.content.length > 0 && (
          <>
            <div className="divide-y">
              {data.content.map((entry) => (
                <ActivityItem key={entry.id} entry={entry} />
              ))}
            </div>

            {/* Пагинация */}
            {data.totalPages > 1 && (
              <div className="flex items-center justify-between mt-4 pt-4 border-t">
                <span className="text-sm text-muted-foreground">
                  Страница {page + 1} из {data.totalPages}
                </span>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handlePrevPage}
                    disabled={page === 0}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleNextPage}
                    disabled={page >= data.totalPages - 1}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
