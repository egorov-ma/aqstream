'use client';

import * as React from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PermissionGuard } from '@/components/auth';
import { useCreateEvent, usePublishEvent } from '@/lib/hooks/use-events';
import { useAllOrganizations } from '@/lib/hooks/use-organizations';
import { useAuthStore } from '@/lib/store/auth-store';
import { ticketTypesApi } from '@/lib/api/ticket-types';
import { EventForm } from '@/components/features/events';
import type { EventFormData } from '@/lib/validations/events';
import { toast } from 'sonner';

export default function NewEventPage() {
  const router = useRouter();
  const createEvent = useCreateEvent();
  const publishEvent = usePublishEvent();
  const user = useAuthStore((state) => state.user);
  const isAdmin = user?.isAdmin ?? false;

  // Загружаем организации для админа
  const { data: organizations } = useAllOrganizations(isAdmin);

  const handleSubmit = async (data: EventFormData, publish: boolean) => {
    try {
      // Валидация: для публикации нужен хотя бы один тип билета
      if (publish && data.ticketTypes.length === 0) {
        toast.error('Добавьте хотя бы один тип билета для публикации');
        return;
      }

      // 1. Создаём событие
      const event = await createEvent.mutateAsync({
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
        // organizationId только для админов
        organizationId: isAdmin ? (data.organizationId || undefined) : undefined,
      });

      // 2. Создаём типы билетов
      if (data.ticketTypes.length > 0) {
        for (const ticketType of data.ticketTypes) {
          await ticketTypesApi.create(event.id, {
            name: ticketType.name,
            description: ticketType.description || undefined,
            quantity: ticketType.quantity ?? undefined,
            salesStart: ticketType.salesStart || undefined,
            salesEnd: ticketType.salesEnd || undefined,
            sortOrder: ticketType.sortOrder,
          });
        }
      }

      // 3. Публикуем если нужно
      if (publish) {
        await publishEvent.mutateAsync(event.id);
        toast.success('Событие опубликовано');
      } else {
        toast.success('Событие сохранено как черновик');
      }

      // 4. Переходим на страницу события
      router.push(`/dashboard/events/${event.id}`);
    } catch (error) {
      // Ошибки уже обрабатываются в хуках через toast
      console.error('Ошибка при создании события:', error);
    }
  };

  const isLoading = createEvent.isPending || publishEvent.isPending;

  return (
    <PermissionGuard requiredPermission="canCreateEvent">
      <div className="flex flex-col gap-6">
        {/* Заголовок */}
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/dashboard/events">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            <h1 className="text-2xl font-bold">Создание события</h1>
            <p className="text-sm text-muted-foreground">
              Заполните информацию о новом мероприятии
            </p>
          </div>
        </div>

        {/* Форма */}
        <EventForm
          onSubmit={handleSubmit}
          isLoading={isLoading}
          organizations={organizations}
          isAdmin={isAdmin}
        />
      </div>
    </PermissionGuard>
  );
}
