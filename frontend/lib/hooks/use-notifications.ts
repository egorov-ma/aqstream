import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '@/lib/api/notifications';
import { useAuthStore } from '@/lib/store/auth-store';

/**
 * Получить список уведомлений.
 */
export function useNotifications(page = 0, size = 20) {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['notifications', page, size],
    queryFn: () => notificationsApi.list(page, size),
    enabled: isAuthenticated,
  });
}

/**
 * Получить количество непрочитанных уведомлений.
 */
export function useUnreadCount() {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationsApi.getUnreadCount(),
    enabled: isAuthenticated,
    // Обновляем каждую минуту
    refetchInterval: 60 * 1000,
  });
}

/**
 * Отметить уведомление как прочитанное.
 */
export function useMarkAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => {
      // Инвалидируем список и счётчик
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}

/**
 * Отметить все уведомления как прочитанные.
 */
export function useMarkAllAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => {
      // Инвалидируем список и счётчик
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}
