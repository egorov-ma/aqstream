import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { eventsApi, type EventFilters } from '@/lib/api/events';
import type { CreateEventRequest, UpdateEventRequest } from '@/lib/api/types';
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
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие обновлено');
    },
    onError: () => {
      toast.error('Ошибка при обновлении события');
    },
  });
}

export function useDeleteEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      toast.success('Событие удалено');
    },
    onError: () => {
      toast.error('Ошибка при удалении события');
    },
  });
}

export function usePublishEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.publish(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие опубликовано');
    },
    onError: () => {
      toast.error('Ошибка при публикации события');
    },
  });
}

export function useCancelEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventsApi.cancel(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events', id] });
      toast.success('Событие отменено');
    },
    onError: () => {
      toast.error('Ошибка при отмене события');
    },
  });
}
