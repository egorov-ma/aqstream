import { apiClient } from './client';
import type { PageResponse, UnreadCountResponse, UserNotification } from './types';

export interface NotificationSettings {
  eventReminders: boolean;
  registrationUpdates: boolean;
  organizationNews: boolean;
}

export interface NotificationPreferencesDto {
  settings: NotificationSettings;
}

export interface UpdatePreferencesRequest {
  settings: NotificationSettings;
}

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

  /**
   * Получить настройки уведомлений.
   */
  getPreferences: async (): Promise<NotificationPreferencesDto> => {
    const response = await apiClient.get<NotificationPreferencesDto>(
      '/api/v1/notifications/preferences'
    );
    return response.data;
  },

  /**
   * Обновить настройки уведомлений.
   */
  updatePreferences: async (data: UpdatePreferencesRequest): Promise<NotificationPreferencesDto> => {
    const response = await apiClient.put<NotificationPreferencesDto>(
      '/api/v1/notifications/preferences',
      data
    );
    return response.data;
  },
};
