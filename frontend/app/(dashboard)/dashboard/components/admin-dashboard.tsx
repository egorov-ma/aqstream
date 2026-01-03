'use client';

import Link from 'next/link';
import { formatDistanceToNow } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ShieldCheck, FileText, ArrowRight, Clock } from 'lucide-react';
import { usePendingOrganizationRequests } from '@/lib/hooks/use-organization-requests';
import type { OrganizationRequest } from '@/lib/api/types';

/**
 * Дашборд для администратора платформы (без выбранной организации).
 * Показывает заявки на организации и статистику платформы.
 */
export function AdminDashboard() {
  const { data: requests, isLoading } = usePendingOrganizationRequests(0, 5);

  if (isLoading) {
    return <AdminDashboardSkeleton />;
  }

  const pendingCount = requests?.totalElements ?? 0;

  return (
    <div className="flex flex-col gap-6">
      {/* Заголовок */}
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <ShieldCheck className="h-6 w-6" />
          Панель администратора
        </h1>
        <p className="text-muted-foreground">Модерация платформы AqStream</p>
      </div>

      {/* Заявки на организации */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <FileText className="h-5 w-5" />
                Заявки на рассмотрение
              </CardTitle>
              <CardDescription>
                {pendingCount > 0
                  ? `${pendingCount} ${getRequestsLabel(pendingCount)} ожидают рассмотрения`
                  : 'Нет заявок на рассмотрение'}
              </CardDescription>
            </div>
            {pendingCount > 0 && (
              <Badge variant="secondary" className="text-lg px-3 py-1">
                {pendingCount}
              </Badge>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {requests?.data && requests.data.length > 0 ? (
            <div className="space-y-3">
              {requests.data.map((request) => (
                <RequestItem key={request.id} request={request} />
              ))}
              <div className="pt-2">
                <Button variant="outline" asChild className="w-full">
                  <Link
                    href="/dashboard/admin/organization-requests"
                    data-testid="all-requests-link"
                  >
                    Все заявки
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Link>
                </Button>
              </div>
            </div>
          ) : (
            <EmptyRequestsState />
          )}
        </CardContent>
      </Card>

      {/* Подсказка для выбора организации */}
      <Card className="border-dashed">
        <CardContent className="pt-6">
          <p className="text-sm text-muted-foreground text-center">
            Чтобы управлять событиями, выберите организацию в переключателе вверху страницы
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

interface RequestItemProps {
  request: OrganizationRequest;
}

function RequestItem({ request }: RequestItemProps) {
  const timeAgo = formatDistanceToNow(new Date(request.createdAt), {
    addSuffix: true,
    locale: ru,
  });

  return (
    <div
      className="flex items-center justify-between p-3 rounded-lg border"
      data-testid={`request-item-${request.id}`}
    >
      <div className="min-w-0 flex-1">
        <h3 className="font-medium truncate">{request.name}</h3>
        <p className="text-sm text-muted-foreground">
          от {request.userName}
        </p>
      </div>
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Clock className="h-4 w-4" />
        <span className="hidden sm:inline">{timeAgo}</span>
      </div>
    </div>
  );
}

function EmptyRequestsState() {
  return (
    <div className="text-center py-6">
      <ShieldCheck className="h-12 w-12 mx-auto mb-3 text-muted-foreground" />
      <h3 className="font-medium mb-1">Нет новых заявок</h3>
      <p className="text-sm text-muted-foreground">
        Все заявки на создание организаций рассмотрены
      </p>
    </div>
  );
}

function AdminDashboardSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <Skeleton className="h-8 w-64 mb-2" />
        <Skeleton className="h-4 w-48" />
      </div>
      <Skeleton className="h-64" />
    </div>
  );
}

/**
 * Склонение слова "заявка"
 */
function getRequestsLabel(count: number): string {
  const lastDigit = count % 10;
  const lastTwoDigits = count % 100;

  if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
    return 'заявок';
  }

  if (lastDigit === 1) {
    return 'заявка';
  }

  if (lastDigit >= 2 && lastDigit <= 4) {
    return 'заявки';
  }

  return 'заявок';
}
