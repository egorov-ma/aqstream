'use client';

import * as React from 'react';
import Link from 'next/link';
import { useParams, useRouter, notFound } from 'next/navigation';
import { ArrowLeft, AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { useEvent, useUpdateEvent, usePublishEvent } from '@/lib/hooks/use-events';
import { useTicketTypes, useCreateTicketType, useUpdateTicketType, useDeleteTicketType } from '@/lib/hooks/use-ticket-types';
import { ticketTypesApi } from '@/lib/api/ticket-types';
import { EventForm } from '@/components/features/events';
import type { EventFormData, TicketTypeFormData } from '@/lib/validations/events';
import { toast } from 'sonner';

export default function EditEventPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const { data: event, isLoading: isLoadingEvent, error } = useEvent(id);
  const { data: existingTicketTypes, isLoading: isLoadingTickets } = useTicketTypes(id);
  const updateEvent = useUpdateEvent();
  const publishEvent = usePublishEvent();

  // Если событие не найдено
  if (error) {
    notFound();
  }

  // Loading state
  if (isLoadingEvent || isLoadingTickets) {
    return <EditEventSkeleton />;
  }

  if (!event) {
    return null;
  }

  // Предупреждение для опубликованных событий
  const isPublished = event.status === 'PUBLISHED';

  // Подготовка defaultValues с существующими типами билетов
  const eventWithTicketTypes = {
    ...event,
    ticketTypes: existingTicketTypes?.map((tt) => ({
      id: tt.id,
      name: tt.name,
      description: tt.description ?? '',
      quantity: tt.quantity ?? null,
      salesStart: tt.salesStart ?? null,
      salesEnd: tt.salesEnd ?? null,
      sortOrder: tt.sortOrder,
    })) ?? [],
  };

  const handleSubmit = async (data: EventFormData, publish: boolean) => {
    try {
      // 1. Обновляем событие
      await updateEvent.mutateAsync({
        id: event.id,
        data: {
          title: data.title,
          description: data.description || undefined,
          startsAt: data.startsAt,
          endsAt: data.endsAt || undefined,
          timezone: data.timezone,
          locationType: data.locationType,
          locationAddress: data.locationAddress || undefined,
          onlineUrl: data.onlineUrl || undefined,
          maxCapacity: data.maxCapacity ?? undefined,
          registrationOpensAt: data.registrationOpensAt || undefined,
          registrationClosesAt: data.registrationClosesAt || undefined,
          isPublic: data.isPublic,
          participantsVisibility: data.participantsVisibility,
          groupId: data.groupId || undefined,
          coverImageUrl: data.coverImageUrl || undefined,
        },
      });

      // 2. Синхронизируем типы билетов
      const existingIds = new Set(existingTicketTypes?.map((tt) => tt.id) ?? []);
      const newTicketTypeIds = new Set(
        data.ticketTypes.filter((tt) => tt.id).map((tt) => tt.id)
      );

      // Удаляем типы билетов, которых нет в форме
      for (const existingTt of existingTicketTypes ?? []) {
        if (!newTicketTypeIds.has(existingTt.id)) {
          try {
            await ticketTypesApi.delete(event.id, existingTt.id);
          } catch {
            // Если не удалось удалить (есть регистрации), деактивируем
            await ticketTypesApi.deactivate(event.id, existingTt.id);
          }
        }
      }

      // Обновляем или создаём типы билетов
      for (const ticketType of data.ticketTypes) {
        const ttData = {
          name: ticketType.name,
          description: ticketType.description || undefined,
          quantity: ticketType.quantity ?? undefined,
          salesStart: ticketType.salesStart || undefined,
          salesEnd: ticketType.salesEnd || undefined,
          sortOrder: ticketType.sortOrder,
        };

        if (ticketType.id && existingIds.has(ticketType.id)) {
          // Обновляем существующий
          await ticketTypesApi.update(event.id, ticketType.id, ttData);
        } else {
          // Создаём новый
          await ticketTypesApi.create(event.id, ttData);
        }
      }

      // 3. Публикуем если нужно (и если ещё не опубликовано)
      if (publish && event.status === 'DRAFT') {
        await publishEvent.mutateAsync(event.id);
        toast.success('Событие опубликовано');
      } else {
        toast.success('Изменения сохранены');
      }

      // 4. Переходим на страницу события
      router.push(`/dashboard/events/${event.id}`);
    } catch (error) {
      console.error('Ошибка при обновлении события:', error);
    }
  };

  const isLoading = updateEvent.isPending || publishEvent.isPending;

  return (
    <div className="flex flex-col gap-6">
      {/* Заголовок */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link href={`/dashboard/events/${id}`}>
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold">Редактирование события</h1>
          <p className="text-sm text-muted-foreground">{event.title}</p>
        </div>
      </div>

      {/* Предупреждение для опубликованных событий */}
      {isPublished && (
        <Alert variant="default" className="border-orange-200 bg-orange-50">
          <AlertTriangle className="h-4 w-4 text-orange-600" />
          <AlertTitle className="text-orange-800">
            Событие опубликовано
          </AlertTitle>
          <AlertDescription className="text-orange-700">
            Изменения будут видны всем участникам. Типы билетов с регистрациями
            нельзя удалить, только деактивировать.
          </AlertDescription>
        </Alert>
      )}

      {/* Форма */}
      <EventForm
        event={eventWithTicketTypes}
        onSubmit={handleSubmit}
        isLoading={isLoading}
      />
    </div>
  );
}

// Skeleton для загрузки
function EditEventSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-4">
        <Skeleton className="h-10 w-10" />
        <div className="space-y-2">
          <Skeleton className="h-8 w-[250px]" />
          <Skeleton className="h-4 w-[200px]" />
        </div>
      </div>
      <div className="space-y-6">
        <Skeleton className="h-[400px]" />
        <Skeleton className="h-[300px]" />
        <Skeleton className="h-[200px]" />
      </div>
    </div>
  );
}
