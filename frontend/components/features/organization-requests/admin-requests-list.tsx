'use client';

import { useState } from 'react';
import { usePendingOrganizationRequests, useApproveOrganizationRequest, useRejectOrganizationRequest } from '@/lib/hooks/use-organization-requests';
import { RequestStatusBadge } from './request-status-badge';
import { RejectDialog } from './reject-dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Card, CardContent } from '@/components/ui/card';
import { Check, X } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { ru } from 'date-fns/locale';
import type { OrganizationRequest } from '@/lib/api/types';

function AdminRequestsSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-10 w-full" />
      {[1, 2, 3].map((i) => (
        <Skeleton key={i} className="h-16 w-full" />
      ))}
    </div>
  );
}

function EmptyState() {
  return (
    <Card>
      <CardContent className="py-8 text-center text-muted-foreground">
        Нет заявок на рассмотрение
      </CardContent>
    </Card>
  );
}

interface AdminRequestsListProps {
  page?: number;
  size?: number;
  onPageChange?: (page: number) => void;
}

export function AdminRequestsList({ page = 0, size = 20, onPageChange }: AdminRequestsListProps) {
  const { data, isLoading, error } = usePendingOrganizationRequests(page, size);
  const approveMutation = useApproveOrganizationRequest();
  const rejectMutation = useRejectOrganizationRequest();

  const [rejectingRequest, setRejectingRequest] = useState<OrganizationRequest | null>(null);

  if (isLoading) {
    return <AdminRequestsSkeleton />;
  }

  if (error) {
    return (
      <Card>
        <CardContent className="py-8 text-center text-destructive">
          Ошибка при загрузке заявок
        </CardContent>
      </Card>
    );
  }

  if (!data || data.data.length === 0) {
    return <EmptyState />;
  }

  const handleApprove = async (id: string) => {
    await approveMutation.mutateAsync(id);
  };

  const handleReject = async (id: string, comment: string) => {
    await rejectMutation.mutateAsync({ id, data: { comment } });
    setRejectingRequest(null);
  };

  return (
    <>
      <div className="rounded-md border" data-testid="admin-requests-table">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Пользователь</TableHead>
              <TableHead>Организация</TableHead>
              <TableHead>Slug</TableHead>
              <TableHead>Дата</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead className="text-right">Действия</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.data.map((request) => (
              <TableRow key={request.id}>
                <TableCell>
                  <div className="font-medium">{request.userName}</div>
                </TableCell>
                <TableCell>
                  <div className="font-medium">{request.name}</div>
                  {request.description && (
                    <div className="text-sm text-muted-foreground line-clamp-1">
                      {request.description}
                    </div>
                  )}
                </TableCell>
                <TableCell className="font-mono text-sm">{request.slug}</TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {formatDistanceToNow(new Date(request.createdAt), {
                    addSuffix: true,
                    locale: ru,
                  })}
                </TableCell>
                <TableCell>
                  <RequestStatusBadge status={request.status} />
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      className="text-green-600 hover:text-green-700 hover:bg-green-50"
                      onClick={() => handleApprove(request.id)}
                      disabled={approveMutation.isPending}
                      data-testid={`request-approve-${request.id}`}
                    >
                      <Check className="h-4 w-4 mr-1" />
                      Одобрить
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="text-destructive hover:text-destructive hover:bg-destructive/10"
                      onClick={() => setRejectingRequest(request)}
                      disabled={rejectMutation.isPending}
                      data-testid={`request-reject-${request.id}`}
                    >
                      <X className="h-4 w-4 mr-1" />
                      Отклонить
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Пагинация */}
      {data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-muted-foreground">
            Показано {data.data.length} из {data.totalElements}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange?.(page - 1)}
              disabled={!data.hasPrevious}
            >
              Назад
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange?.(page + 1)}
              disabled={!data.hasNext}
            >
              Вперёд
            </Button>
          </div>
        </div>
      )}

      {/* Диалог отклонения */}
      <RejectDialog
        request={rejectingRequest}
        open={!!rejectingRequest}
        onOpenChange={(open) => !open && setRejectingRequest(null)}
        onConfirm={handleReject}
        isPending={rejectMutation.isPending}
      />
    </>
  );
}
