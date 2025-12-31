'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { ticketTypesApi } from '@/lib/api/ticket-types';
import type { CreateTicketTypeRequest, UpdateTicketTypeRequest } from '@/lib/api/types';

/**
 * Хук для получения списка типов билетов события
 */
export function useTicketTypes(eventId: string | undefined) {
  return useQuery({
    queryKey: ['events', eventId, 'ticket-types'],
    queryFn: () => ticketTypesApi.list(eventId!),
    enabled: !!eventId,
  });
}

/**
 * Хук для получения конкретного типа билета
 */
export function useTicketType(eventId: string | undefined, ticketTypeId: string | undefined) {
  return useQuery({
    queryKey: ['events', eventId, 'ticket-types', ticketTypeId],
    queryFn: () => ticketTypesApi.getById(eventId!, ticketTypeId!),
    enabled: !!eventId && !!ticketTypeId,
  });
}

/**
 * Хук для создания типа билета
 */
export function useCreateTicketType(eventId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateTicketTypeRequest) =>
      ticketTypesApi.create(eventId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events', eventId, 'ticket-types'] });
      toast.success('Тип билета создан');
    },
    onError: () => {
      toast.error('Ошибка при создании типа билета');
    },
  });
}

/**
 * Хук для обновления типа билета
 */
export function useUpdateTicketType(eventId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      ticketTypeId,
      data,
    }: {
      ticketTypeId: string;
      data: UpdateTicketTypeRequest;
    }) => ticketTypesApi.update(eventId, ticketTypeId, data),
    onSuccess: (_, { ticketTypeId }) => {
      queryClient.invalidateQueries({ queryKey: ['events', eventId, 'ticket-types'] });
      queryClient.invalidateQueries({
        queryKey: ['events', eventId, 'ticket-types', ticketTypeId],
      });
      toast.success('Тип билета обновлён');
    },
    onError: () => {
      toast.error('Ошибка при обновлении типа билета');
    },
  });
}

/**
 * Хук для удаления типа билета
 */
export function useDeleteTicketType(eventId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ticketTypeId: string) =>
      ticketTypesApi.delete(eventId, ticketTypeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events', eventId, 'ticket-types'] });
      toast.success('Тип билета удалён');
    },
    onError: () => {
      toast.error('Ошибка при удалении типа билета');
    },
  });
}

/**
 * Хук для деактивации типа билета
 */
export function useDeactivateTicketType(eventId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ticketTypeId: string) =>
      ticketTypesApi.deactivate(eventId, ticketTypeId),
    onSuccess: (_, ticketTypeId) => {
      queryClient.invalidateQueries({ queryKey: ['events', eventId, 'ticket-types'] });
      queryClient.invalidateQueries({
        queryKey: ['events', eventId, 'ticket-types', ticketTypeId],
      });
      toast.success('Тип билета деактивирован');
    },
    onError: () => {
      toast.error('Ошибка при деактивации типа билета');
    },
  });
}
