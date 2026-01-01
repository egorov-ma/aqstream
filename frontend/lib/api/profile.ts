import { apiClient } from './client';
import type { User, UpdateProfileRequest, ChangePasswordRequest } from './types';

/**
 * API для работы с профилем пользователя.
 */
export const profileApi = {
  /**
   * Обновляет профиль пользователя.
   */
  updateProfile: async (data: UpdateProfileRequest): Promise<User> => {
    const response = await apiClient.patch<User>('/api/v1/users/me', data);
    return response.data;
  },

  /**
   * Изменяет пароль пользователя.
   */
  changePassword: async (data: ChangePasswordRequest): Promise<void> => {
    await apiClient.post('/api/v1/auth/change-password', data);
  },
};
