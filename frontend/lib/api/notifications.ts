import { apiClient } from './client';
import type { PageResponse, UnreadCountResponse, UserNotification } from './types';

export const notificationsApi = {
  /**
   * Получить список уведомлений пользователя.
   */
  list: async (page = 0, size = 20): Promise<PageResponse<UserNotification>> => {
    const response = await apiClient.get<PageResponse<UserNotification>>('/api/v1/notifications', {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Получить количество непрочитанных уведомлений.
   */
  getUnreadCount: async (): Promise<number> => {
    const response = await apiClient.get<UnreadCountResponse>('/api/v1/notifications/unread-count');
    return response.data.count;
  },

  /**
   * Отметить уведомление как прочитанное.
   */
  markAsRead: async (id: string): Promise<void> => {
    await apiClient.post(`/api/v1/notifications/${id}/read`);
  },

  /**
   * Отметить все уведомления как прочитанные.
   */
  markAllAsRead: async (): Promise<void> => {
    await apiClient.post('/api/v1/notifications/read-all');
  },
};
