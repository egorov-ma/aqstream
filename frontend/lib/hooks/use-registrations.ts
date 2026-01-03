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
      // Инвалидируем типы билетов для обновления счётчиков (soldCount, available)
      queryClient.invalidateQueries({ queryKey: ['events', eventId, 'ticket-types'] });
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
    onSuccess: (registration) => {
      // Инвалидируем списки регистраций
      queryClient.invalidateQueries({ queryKey: ['registrations', 'my'] });
      // Инвалидируем типы билетов для обновления счётчиков (soldCount, available)
      if (registration.eventId) {
        queryClient.invalidateQueries({ queryKey: ['events', registration.eventId, 'ticket-types'] });
      }
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

      // Сохраняем предыдущие данные и находим eventId
      const previousQueries = queryClient.getQueriesData<PageResponse<Registration>>({
        queryKey: ['registrations', 'my'],
      });

      // Находим eventId отменяемой регистрации
      let eventId: string | undefined;
      for (const [, data] of previousQueries) {
        if (data?.data) {
          const registration = data.data.find((reg) => reg.id === registrationId);
          if (registration?.eventId) {
            eventId = registration.eventId;
            break;
          }
        }
      }

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

      return { previousQueries, eventId };
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
    onSuccess: (_data, _registrationId, context) => {
      queryClient.invalidateQueries({ queryKey: ['registrations'] });
      // Инвалидируем типы билетов для обновления счётчиков (soldCount, available)
      if (context?.eventId) {
        queryClient.invalidateQueries({ queryKey: ['events', context.eventId, 'ticket-types'] });
      }
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
