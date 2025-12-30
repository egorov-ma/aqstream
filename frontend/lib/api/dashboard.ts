import { apiClient } from './client';
import type { DashboardStats } from './types';

export const dashboardApi = {
  /**
   * Получить статистику для dashboard.
   */
  getStats: async (): Promise<DashboardStats> => {
    const response = await apiClient.get<DashboardStats>('/api/v1/dashboard/stats');
    return response.data;
  },
};
