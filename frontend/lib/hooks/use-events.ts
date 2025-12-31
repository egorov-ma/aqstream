import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { eventsApi, type EventFilters } from '@/lib/api/events';
import type {
  CreateEventRequest,
  UpdateEventRequest,
  Event,
  PageResponse,
} from '@/lib/api/types';
import { toast } from 'sonner';

export function useEvents(filters?: EventFilters) {
  return useQuery({
    queryKey: ['events', filters],
    queryFn: () => eventsApi.list(filters),
  });
}

export function useEvent(id: string) {
  return useQuery({
    queryKey: ['events', id],
    queryFn: () => eventsApi.getById(id),
    enabled: !!id,
  });
}

export function useEventBySlug(slug: string) {
  return useQuery({
    queryKey: ['events', 'slug', slug],
    queryFn: () => eventsApi.getBySlug(slug),
    enabled: !!slug,
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateEventRequest) => eventsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Событие создано');
    },
    onError: () => {
      toast.error('Ошибка при создании события');
    },
  });
}

export function useUpdateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateEventRequest }) =>
      eventsApi.update(id, data),
    // Optimistic update
    onMutate: async ({ id, data }) => {
      // Отменяем исходящие запросы
      await queryClient.cancelQueries({ queryKey: ['events', id] });

      // Сохраняем предыдущее значение
      const previousEvent = queryClient.getQueryData<Event>(['events', id]);

      // Оптимистично обновляем кеш (без recurrenceRule, т.к. типы не совместимы)
      if (previousEvent) {
        const { recurrenceRule: _, ...dataWithoutRecurrence } = data;
        queryClient.setQueryData<Event>(['events', id], {
          ...previousEvent,
          ...dataWithoutRecurrence,
          updatedAt: new Date().toISOString(),
        });
      }

      return { previousEvent };
    },
    onError: (_error, { id }, context) => {
      // Откатываем при ошибке
      if (context?.previousEvent) {
        queryClient.setQueryData(['events', id], context.previousEvent);
      }
      toast.error('Ошибка при обновлении события');
    },
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие обновлено');
    },
  });
}

export function useDeleteEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.delete(id),
    // Optimistic update - удаляем из списка сразу
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['events'] });

      // Сохраняем предыдущие данные всех списков
      const previousQueries = queryClient.getQueriesData<PageResponse<Event>>({
        queryKey: ['events'],
      });

      // Оптимистично удаляем из всех списков
      queryClient.setQueriesData<PageResponse<Event>>(
        { queryKey: ['events'] },
        (old) => {
          if (!old) return old;
          return {
            ...old,
            content: old.content.filter((e) => e.id !== id),
            totalElements: old.totalElements - 1,
          };
        }
      );

      return { previousQueries };
    },
    onError: (_error, _id, context) => {
      // Откатываем при ошибке
      if (context?.previousQueries) {
        context.previousQueries.forEach(([queryKey, data]) => {
          queryClient.setQueryData(queryKey, data);
        });
      }
      toast.error('Ошибка при удалении события');
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Событие удалено');
    },
  });
}

export function usePublishEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.publish(id),
    // Optimistic update
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['events', id] });

      const previousEvent = queryClient.getQueryData<Event>(['events', id]);

      if (previousEvent) {
        queryClient.setQueryData<Event>(['events', id], {
          ...previousEvent,
          status: 'PUBLISHED',
          updatedAt: new Date().toISOString(),
        });
      }

      return { previousEvent };
    },
    onError: (_error, id, context) => {
      if (context?.previousEvent) {
        queryClient.setQueryData(['events', id], context.previousEvent);
      }
      toast.error('Ошибка при публикации события');
    },
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие опубликовано');
    },
  });
}

export function useCancelEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      eventsApi.cancel(id, reason),
    // Optimistic update
    onMutate: async ({ id, reason }) => {
      await queryClient.cancelQueries({ queryKey: ['events', id] });

      const previousEvent = queryClient.getQueryData<Event>(['events', id]);

      if (previousEvent) {
        queryClient.setQueryData<Event>(['events', id], {
          ...previousEvent,
          status: 'CANCELLED',
          cancelReason: reason,
          cancelledAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        });
      }

      return { previousEvent };
    },
    onError: (_error, { id }, context) => {
      if (context?.previousEvent) {
        queryClient.setQueryData(['events', id], context.previousEvent);
      }
      toast.error('Ошибка при отмене события');
    },
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие отменено');
    },
  });
}

export function useUnpublishEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.unpublish(id),
    // Optimistic update
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['events', id] });

      const previousEvent = queryClient.getQueryData<Event>(['events', id]);

      if (previousEvent) {
        queryClient.setQueryData<Event>(['events', id], {
          ...previousEvent,
          status: 'DRAFT',
          updatedAt: new Date().toISOString(),
        });
      }

      return { previousEvent };
    },
    onError: (_error, id, context) => {
      if (context?.previousEvent) {
        queryClient.setQueryData(['events', id], context.previousEvent);
      }
      toast.error('Ошибка при снятии события с публикации');
    },
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие снято с публикации');
    },
  });
}

export function useCompleteEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.complete(id),
    // Optimistic update
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['events', id] });

      const previousEvent = queryClient.getQueryData<Event>(['events', id]);

      if (previousEvent) {
        queryClient.setQueryData<Event>(['events', id], {
          ...previousEvent,
          status: 'COMPLETED',
          updatedAt: new Date().toISOString(),
        });
      }

      return { previousEvent };
    },
    onError: (_error, id, context) => {
      if (context?.previousEvent) {
        queryClient.setQueryData(['events', id], context.previousEvent);
      }
      toast.error('Ошибка при завершении события');
    },
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие завершено');
    },
  });
}

export function useEventActivity(eventId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['events', eventId, 'activity', page, size],
    queryFn: () => eventsApi.getActivity(eventId, page, size),
    enabled: !!eventId,
  });
}

/**
 * Хук для получения публичных типов билетов по slug события
 */
export function usePublicTicketTypes(slug: string) {
  return useQuery({
    queryKey: ['public', 'events', slug, 'ticket-types'],
    queryFn: () => eventsApi.getPublicTicketTypes(slug),
    enabled: !!slug,
  });
}
