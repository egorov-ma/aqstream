import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { checkInApi, type CheckInInfo } from '@/lib/api/check-in';
import {
  savePendingCheckIn,
  removePendingCheckIn,
  getPendingCheckIns,
} from '@/lib/pwa/offline-storage';

/**
 * Получить информацию о регистрации по коду.
 */
export function useCheckInInfo(confirmationCode: string | null) {
  return useQuery({
    queryKey: ['check-in', confirmationCode],
    queryFn: () => checkInApi.getInfo(confirmationCode!),
    enabled: !!confirmationCode,
    retry: false,
  });
}

/**
 * Выполнить check-in.
 */
export function useConfirmCheckIn() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (confirmationCode: string) => {
      // Если офлайн, сохраняем для синхронизации
      if (!navigator.onLine) {
        // Получаем info из кеша
        const info = queryClient.getQueryData<CheckInInfo>(['check-in', confirmationCode]);
        if (info) {
          await savePendingCheckIn(info.registrationId, info.eventId, confirmationCode);
          return {
            registrationId: info.registrationId,
            confirmationCode,
            eventTitle: info.eventTitle,
            ticketTypeName: info.ticketTypeName,
            firstName: info.firstName,
            lastName: info.lastName,
            checkedInAt: new Date().toISOString(),
            message: 'Check-in сохранён (синхронизируется при восстановлении сети)',
          };
        }
        throw new Error('Нет данных для офлайн check-in');
      }

      return checkInApi.confirm(confirmationCode);
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['check-in', result.confirmationCode] });
      toast.success(result.message);
    },
    onError: (error) => {
      const message = error instanceof Error ? error.message : 'Ошибка check-in';
      toast.error(message);
    },
  });
}

/**
 * Синхронизировать pending check-ins.
 */
export function useSyncCheckIns() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const pending = await getPendingCheckIns();
      const results: { success: string[]; failed: string[] } = {
        success: [],
        failed: [],
      };

      for (const item of pending) {
        try {
          await checkInApi.confirm(item.confirmationCode);
          await removePendingCheckIn(item.registrationId);
          results.success.push(item.confirmationCode);
        } catch {
          results.failed.push(item.confirmationCode);
        }
      }

      return results;
    },
    onSuccess: (results) => {
      if (results.success.length > 0) {
        toast.success(`Синхронизировано: ${results.success.length} check-in(s)`);
      }
      if (results.failed.length > 0) {
        toast.error(`Ошибка синхронизации: ${results.failed.length} check-in(s)`);
      }
      queryClient.invalidateQueries({ queryKey: ['check-in'] });
    },
  });
}
