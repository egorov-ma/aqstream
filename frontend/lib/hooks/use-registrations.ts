import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  registrationsApi,
  type RegistrationFilters,
  type CreateRegistrationRequest,
} from '@/lib/api/registrations';
import type { Registration, PageResponse } from '@/lib/api/types';
import { toast } from 'sonner';

/**
 * Получить регистрации события (для организаторов)
 */
export function useEventRegistrations(eventId: string, filters?: RegistrationFilters) {
  return useQuery({
    queryKey: ['registrations', 'event', eventId, filters],
    queryFn: () => registrationsApi.listByEvent(eventId, filters),
    enabled: !!eventId,
  });
}

/**
 * Получить регистрацию по ID
 */
export function useRegistration(registrationId: string) {
  return useQuery({
    queryKey: ['registrations', registrationId],
    queryFn: () => registrationsApi.getById(registrationId),
    enabled: !!registrationId,
  });
}

/**
 * Получить мои регистрации
 */
export function useMyRegistrations(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['registrations', 'my', params],
    queryFn: () => registrationsApi.getMy(params),
  });
}

/**
 * Создать регистрацию на событие (для пользователей того же tenant)
 * Toast не показывается — пользователь редиректится на success page
 */
export function useCreateRegistration(eventId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateRegistrationRequest) =>
      registrationsApi.create(eventId, data),
    onSuccess: () => {
      // Инвалидируем списки регистраций
      queryClient.invalidateQueries({ queryKey: ['registrations', 'my'] });
      queryClient.invalidateQueries({ queryKey: ['registrations', 'event', eventId] });
      // Toast не показываем — success page уже информирует пользователя
    },
    // Ошибки обрабатываются в компоненте формы для показа конкретных сообщений
  });
}

/**
 * Создать регистрацию на публичное событие (по slug).
 * Позволяет пользователям из любого tenant регистрироваться на публичные события.
 */
export function useCreatePublicRegistration(slug: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateRegistrationRequest) =>
      registrationsApi.createForPublicEvent(slug, data),
    onSuccess: () => {
      // Инвалидируем списки регистраций
      queryClient.invalidateQueries({ queryKey: ['registrations', 'my'] });
      // Toast не показываем — success page уже информирует пользователя
    },
    // Ошибки обрабатываются в компоненте формы для показа конкретных сообщений
  });
}

/**
 * Отменить регистрацию
 */
export function useCancelRegistration() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (registrationId: string) => registrationsApi.cancel(registrationId),
    // Optimistic update
    onMutate: async (registrationId) => {
      // Отменяем исходящие запросы
      await queryClient.cancelQueries({ queryKey: ['registrations', 'my'] });

      // Сохраняем предыдущие данные
      const previousQueries = queryClient.getQueriesData<PageResponse<Registration>>({
        queryKey: ['registrations', 'my'],
      });

      // Оптимистично обновляем статус
      queryClient.setQueriesData<PageResponse<Registration>>(
        { queryKey: ['registrations', 'my'] },
        (old) => {
          if (!old) return old;
          return {
            ...old,
            data: old.data.map((reg) =>
              reg.id === registrationId
                ? { ...reg, status: 'CANCELLED' as const, cancelledAt: new Date().toISOString() }
                : reg
            ),
          };
        }
      );

      return { previousQueries };
    },
    onError: (_error, _registrationId, context) => {
      // Откатываем при ошибке
      if (context?.previousQueries) {
        context.previousQueries.forEach(([queryKey, data]) => {
          queryClient.setQueryData(queryKey, data);
        });
      }
      toast.error('Ошибка при отмене регистрации');
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['registrations'] });
      toast.success('Регистрация отменена');
    },
  });
}

/**
 * Повторно отправить билет в Telegram
 */
export function useResendTicket() {
  return useMutation({
    mutationFn: (registrationId: string) => registrationsApi.resendTicket(registrationId),
    onSuccess: () => {
      toast.success('Билет отправлен в Telegram');
    },
    onError: () => {
      toast.error('Ошибка при отправке билета');
    },
  });
}
