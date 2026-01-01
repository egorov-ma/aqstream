'use client';

import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { TicketDetail } from '@/components/features/account/ticket-detail';

import { useRegistration, useCancelRegistration } from '@/lib/hooks/use-registrations';

export default function TicketDetailPage() {
  const params = useParams();
  const router = useRouter();
  const registrationId = params.id as string;

  const { data: registration, isLoading, error } = useRegistration(registrationId);
  const cancelMutation = useCancelRegistration();

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Skeleton className="h-10 w-10" />
          <div className="space-y-2">
            <Skeleton className="h-6 w-64" />
            <Skeleton className="h-4 w-40" />
          </div>
        </div>
        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardContent className="flex flex-col items-center gap-4 py-6">
              <Skeleton className="h-64 w-64" />
              <Skeleton className="h-8 w-32" />
            </CardContent>
          </Card>
          <Card>
            <CardContent className="space-y-4 py-6">
              <Skeleton className="h-6 w-full" />
              <Skeleton className="h-6 w-full" />
              <Skeleton className="h-6 w-full" />
              <Skeleton className="h-10 w-full" />
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  if (error || !registration) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/dashboard/my-registrations">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <h1 className="text-2xl font-bold">Билет не найден</h1>
        </div>
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-muted-foreground mb-4">
              Регистрация не найдена или у вас нет доступа к ней.
            </p>
            <Button asChild>
              <Link href="/dashboard/my-registrations">Вернуться к билетам</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <TicketDetail
      registration={registration}
      onCancel={(id) => {
        cancelMutation.mutate(id);
        router.push('/dashboard/my-registrations');
      }}
      isCancelling={cancelMutation.isPending}
    />
  );
}
