'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Calendar, Ticket, XCircle } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination';

import { useMyRegistrations, useCancelRegistration } from '@/lib/hooks/use-registrations';
import { formatEventDate } from '@/lib/utils/date-time';
import { ROUTES } from '@/lib/routes';
import type { RegistrationStatus } from '@/lib/api/types';

const PAGE_SIZE = 10;

/**
 * Цвета для статусов регистрации
 */
function getStatusBadgeVariant(
  status: RegistrationStatus
): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status) {
    case 'CONFIRMED':
      return 'default';
    case 'CANCELLED':
      return 'destructive';
    case 'RESERVED':
    case 'PENDING':
      return 'secondary';
    case 'EXPIRED':
      return 'outline';
    default:
      return 'outline';
  }
}

/**
 * Названия статусов на русском
 */
function getStatusLabel(status: RegistrationStatus): string {
  switch (status) {
    case 'CONFIRMED':
      return 'Подтверждена';
    case 'CANCELLED':
      return 'Отменена';
    case 'RESERVED':
      return 'Забронирована';
    case 'PENDING':
      return 'Ожидает оплаты';
    case 'EXPIRED':
      return 'Истекла';
    default:
      return status;
  }
}

/**
 * Генерирует массив номеров страниц для отображения
 */
function getPageNumbers(currentPage: number, totalPages: number): (number | 'ellipsis')[] {
  const pages: (number | 'ellipsis')[] = [];

  if (totalPages <= 5) {
    // Показываем все страницы
    for (let i = 0; i < totalPages; i++) {
      pages.push(i);
    }
  } else {
    // Всегда показываем первую страницу
    pages.push(0);

    if (currentPage > 2) {
      pages.push('ellipsis');
    }

    // Показываем страницы вокруг текущей
    const start = Math.max(1, currentPage - 1);
    const end = Math.min(totalPages - 2, currentPage + 1);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    if (currentPage < totalPages - 3) {
      pages.push('ellipsis');
    }

    // Всегда показываем последнюю страницу
    pages.push(totalPages - 1);
  }

  return pages;
}

export default function MyRegistrationsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useMyRegistrations({ page, size: PAGE_SIZE });
  const cancelMutation = useCancelRegistration();

  const registrations = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const hasNext = data?.hasNext ?? false;
  const hasPrevious = data?.hasPrevious ?? false;

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">Мои регистрации</h1>
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-4 w-60" />
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">Мои регистрации</h1>
        </div>
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-muted-foreground">
              Произошла ошибка при загрузке регистраций. Попробуйте обновить страницу.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Мои регистрации</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Ticket className="h-5 w-5" />
            Ваши билеты
          </CardTitle>
          <CardDescription>
            Список ваших регистраций на события
          </CardDescription>
        </CardHeader>
        <CardContent>
          {registrations.length === 0 ? (
            <div className="py-8 text-center">
              <Calendar className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground mb-4">
                У вас пока нет регистраций на события
              </p>
              <Button asChild>
                <Link href={ROUTES.EVENTS}>Найти события</Link>
              </Button>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Событие</TableHead>
                    <TableHead>Дата события</TableHead>
                    <TableHead>Тип билета</TableHead>
                    <TableHead>Код</TableHead>
                    <TableHead>Статус</TableHead>
                    <TableHead className="text-right">Действия</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {registrations.map((registration) => (
                    <TableRow key={registration.id}>
                      <TableCell className="font-medium">
                        <Link
                          href={ROUTES.EVENT(registration.eventSlug)}
                          className="hover:underline"
                        >
                          {registration.eventTitle}
                        </Link>
                      </TableCell>
                      <TableCell>
                        {formatEventDate(registration.eventStartsAt)}
                      </TableCell>
                      <TableCell>{registration.ticketTypeName}</TableCell>
                      <TableCell>
                        <code className="font-mono text-sm bg-muted px-2 py-1 rounded">
                          {registration.confirmationCode}
                        </code>
                      </TableCell>
                      <TableCell>
                        <Badge variant={getStatusBadgeVariant(registration.status)}>
                          {getStatusLabel(registration.status)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        {registration.status === 'CONFIRMED' && (
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="text-destructive hover:text-destructive"
                                disabled={cancelMutation.isPending}
                              >
                                <XCircle className="h-4 w-4 mr-1" />
                                Отменить
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>Отменить регистрацию?</AlertDialogTitle>
                                <AlertDialogDescription>
                                  Вы уверены, что хотите отменить регистрацию на событие &quot;
                                  {registration.eventTitle}&quot;? Это действие нельзя отменить.
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>Нет, оставить</AlertDialogCancel>
                                <AlertDialogAction
                                  onClick={() => cancelMutation.mutate(registration.id)}
                                  className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                >
                                  Да, отменить
                                </AlertDialogAction>
                              </AlertDialogFooter>
                            </AlertDialogContent>
                          </AlertDialog>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Пагинация */}
              {totalPages > 1 && (
                <div className="mt-6">
                  <Pagination>
                    <PaginationContent>
                      <PaginationItem>
                        <PaginationPrevious
                          onClick={() => setPage((p) => Math.max(0, p - 1))}
                          className={!hasPrevious ? 'pointer-events-none opacity-50' : 'cursor-pointer'}
                        />
                      </PaginationItem>

                      {getPageNumbers(page, totalPages).map((pageNum, idx) =>
                        pageNum === 'ellipsis' ? (
                          <PaginationItem key={`ellipsis-${idx}`}>
                            <span className="flex h-9 w-9 items-center justify-center">...</span>
                          </PaginationItem>
                        ) : (
                          <PaginationItem key={pageNum}>
                            <PaginationLink
                              onClick={() => setPage(pageNum)}
                              isActive={page === pageNum}
                              className="cursor-pointer"
                            >
                              {pageNum + 1}
                            </PaginationLink>
                          </PaginationItem>
                        )
                      )}

                      <PaginationItem>
                        <PaginationNext
                          onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                          className={!hasNext ? 'pointer-events-none opacity-50' : 'cursor-pointer'}
                        />
                      </PaginationItem>
                    </PaginationContent>
                  </Pagination>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
