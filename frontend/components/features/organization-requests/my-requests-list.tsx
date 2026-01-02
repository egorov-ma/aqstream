'use client';

import { useMyOrganizationRequests } from '@/lib/hooks/use-organization-requests';
import { RequestStatusBadge } from './request-status-badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { formatDistanceToNow } from 'date-fns';
import { ru } from 'date-fns/locale';

function MyRequestsSkeleton() {
  return (
    <div className="space-y-4">
      {[1, 2].map((i) => (
        <Card key={i}>
          <CardHeader>
            <Skeleton className="h-5 w-48" />
            <Skeleton className="h-4 w-32" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-4 w-24" />
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

function EmptyState() {
  return (
    <Card>
      <CardContent className="py-8 text-center text-muted-foreground">
        У вас пока нет заявок на создание организации
      </CardContent>
    </Card>
  );
}

export function MyRequestsList() {
  const { data: requests, isLoading, error } = useMyOrganizationRequests();

  if (isLoading) {
    return <MyRequestsSkeleton />;
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

  if (!requests || requests.length === 0) {
    return <EmptyState />;
  }

  return (
    <div className="space-y-4" data-testid="my-requests-list">
      {requests.map((request) => (
        <Card key={request.id}>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">{request.name}</CardTitle>
              <RequestStatusBadge status={request.status} />
            </div>
            <CardDescription>
              Slug: {request.slug} &bull;{' '}
              {formatDistanceToNow(new Date(request.createdAt), {
                addSuffix: true,
                locale: ru,
              })}
            </CardDescription>
          </CardHeader>
          {(request.description || request.reviewComment) && (
            <CardContent className="space-y-2">
              {request.description && (
                <p className="text-sm text-muted-foreground">{request.description}</p>
              )}
              {request.status === 'REJECTED' && request.reviewComment && (
                <div className="rounded-md bg-destructive/10 p-3 text-sm">
                  <span className="font-medium text-destructive">Причина отклонения: </span>
                  {request.reviewComment}
                </div>
              )}
            </CardContent>
          )}
        </Card>
      ))}
    </div>
  );
}
