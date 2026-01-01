'use client';

import { Ticket } from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { TicketList } from '@/components/features/account/ticket-list';

import { useMyRegistrations, useCancelRegistration } from '@/lib/hooks/use-registrations';

// Загружаем все регистрации для корректной работы табов (фильтрация на клиенте)
const PAGE_SIZE = 100;

export default function MyRegistrationsPage() {
  const { data, isLoading, error } = useMyRegistrations({ size: PAGE_SIZE });
  const cancelMutation = useCancelRegistration();

  const registrations = data?.content ?? [];

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">Мои билеты</h1>
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-4 w-60" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-10 w-full mb-4" />
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-24 w-full" />
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
          <h1 className="text-2xl font-bold">Мои билеты</h1>
        </div>
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-muted-foreground">
              Произошла ошибка при загрузке билетов. Попробуйте обновить страницу.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Мои билеты</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Ticket className="h-5 w-5" />
            Ваши регистрации на события
          </CardTitle>
          <CardDescription>
            Управляйте своими регистрациями и просматривайте билеты
          </CardDescription>
        </CardHeader>
        <CardContent>
          <TicketList
            registrations={registrations}
            onCancel={(id) => cancelMutation.mutate(id)}
            isCancelling={cancelMutation.isPending}
          />
        </CardContent>
      </Card>
    </div>
  );
}
