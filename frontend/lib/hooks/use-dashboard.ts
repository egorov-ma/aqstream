import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/lib/api/dashboard';
import { useAuthStore } from '@/lib/store/auth-store';

/**
 * Получить статистику для dashboard.
 */
export function useDashboardStats() {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: () => dashboardApi.getStats(),
    enabled: isAuthenticated,
    // Обновляем каждые 5 минут
    staleTime: 5 * 60 * 1000,
  });
}
