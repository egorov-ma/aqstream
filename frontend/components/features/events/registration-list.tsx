'use client';

import { useState, useCallback } from 'react';
import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Search, Users, ChevronLeft, ChevronRight } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useEventRegistrations } from '@/lib/hooks/use-registrations';
import type { Registration, RegistrationStatus } from '@/lib/api/types';

interface RegistrationListProps {
  eventId: string;
}

// Статусы регистрации
const REGISTRATION_STATUSES: { value: RegistrationStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Все статусы' },
  { value: 'CONFIRMED', label: 'Подтверждённые' },
  { value: 'CANCELLED', label: 'Отменённые' },
  { value: 'PENDING', label: 'Ожидают оплаты' },
  { value: 'RESERVED', label: 'Зарезервированы' },
];

// Форматирование даты
function formatDate(isoString: string): string {
  const date = parseISO(isoString);
  return format(date, 'd MMM yyyy, HH:mm', { locale: ru });
}

// Badge для статуса регистрации
function RegistrationStatusBadge({ status }: { status: RegistrationStatus }) {
  const variants: Record<RegistrationStatus, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; label: string }> = {
    CONFIRMED: { variant: 'default', label: 'Подтверждено' },
    CHECKED_IN: { variant: 'default', label: 'Отмечено' },
    CANCELLED: { variant: 'destructive', label: 'Отменено' },
    PENDING: { variant: 'secondary', label: 'Ожидает' },
    RESERVED: { variant: 'outline', label: 'Резерв' },
    EXPIRED: { variant: 'secondary', label: 'Истекло' },
  };

  const config = variants[status] || { variant: 'secondary', label: status };

  return <Badge variant={config.variant}>{config.label}</Badge>;
}

// Skeleton для загрузки
function RegistrationListSkeleton() {
  return (
    <div className="space-y-4">
      <div className="flex gap-4">
        <Skeleton className="h-10 w-[300px]" />
        <Skeleton className="h-10 w-[180px]" />
      </div>
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex items-center space-x-4 p-4">
          <div className="space-y-2 flex-1">
            <Skeleton className="h-4 w-[200px]" />
            <Skeleton className="h-4 w-[150px]" />
          </div>
          <Skeleton className="h-6 w-24" />
          <Skeleton className="h-6 w-20" />
        </div>
      ))}
    </div>
  );
}

// Пустое состояние
function EmptyState() {
  return (
    <div className="text-center py-12">
      <Users className="mx-auto h-12 w-12 text-muted-foreground opacity-50" />
      <h3 className="mt-4 text-lg font-semibold">Нет регистраций</h3>
      <p className="mt-2 text-sm text-muted-foreground">
        Пока никто не зарегистрировался на это событие
      </p>
    </div>
  );
}

export function RegistrationList({ eventId }: RegistrationListProps) {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<RegistrationStatus | 'ALL'>('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 20;

  // Debounced поиск
  const [debouncedQuery, setDebouncedQuery] = useState('');

  const handleQueryChange = useCallback((value: string) => {
    setQuery(value);
    // Простой debounce
    const timeoutId = setTimeout(() => {
      setDebouncedQuery(value);
      setPage(0);
    }, 300);
    return () => clearTimeout(timeoutId);
  }, []);

  const handleStatusChange = useCallback((value: string) => {
    setStatus(value as RegistrationStatus | 'ALL');
    setPage(0);
  }, []);

  const { data, isLoading, error } = useEventRegistrations(eventId, {
    query: debouncedQuery || undefined,
    status: status === 'ALL' ? undefined : status,
    page,
    size: pageSize,
  });

  if (isLoading && !data) {
    return <RegistrationListSkeleton />;
  }

  if (error) {
    return (
      <div className="text-center py-12 text-destructive">
        Ошибка загрузки регистраций
      </div>
    );
  }

  const registrations = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="space-y-4">
      {/* Фильтры */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Поиск по имени или email..."
            value={query}
            onChange={(e) => handleQueryChange(e.target.value)}
            className="pl-9"
            data-testid="registration-search"
          />
        </div>
        <Select value={status} onValueChange={handleStatusChange}>
          <SelectTrigger className="w-[180px]" data-testid="registration-status-filter">
            <SelectValue placeholder="Статус" />
          </SelectTrigger>
          <SelectContent>
            {REGISTRATION_STATUSES.map((s) => (
              <SelectItem key={s.value} value={s.value}>
                {s.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Таблица или пустое состояние */}
      {registrations.length === 0 ? (
        <EmptyState />
      ) : (
        <>
          <div className="rounded-md border" data-testid="registrations-table">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[200px]">Участник</TableHead>
                  <TableHead>Тип билета</TableHead>
                  <TableHead>Статус</TableHead>
                  <TableHead>Код</TableHead>
                  <TableHead>Дата регистрации</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {registrations.map((reg: Registration) => (
                  <TableRow key={reg.id}>
                    <TableCell>
                      <div>
                        <p className="font-medium">
                          {reg.firstName} {reg.lastName || ''}
                        </p>
                        <p className="text-sm text-muted-foreground">{reg.email}</p>
                      </div>
                    </TableCell>
                    <TableCell>{reg.ticketTypeName}</TableCell>
                    <TableCell>
                      <RegistrationStatusBadge status={reg.status} />
                    </TableCell>
                    <TableCell>
                      <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                        {reg.confirmationCode}
                      </code>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDate(reg.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Пагинация */}
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Показано {registrations.length} из {totalElements}
            </p>
            {totalPages > 1 && (
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <span className="text-sm">
                  {page + 1} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
