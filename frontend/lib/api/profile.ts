import { apiClient } from './client';
import type { User, UpdateProfileRequest, ChangePasswordRequest } from './types';

export interface TelegramLinkTokenResponse {
  token: string;
  botLink: string;
}

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

  /**
   * Генерирует токен для привязки Telegram.
   */
  generateTelegramLinkToken: async (): Promise<TelegramLinkTokenResponse> => {
    const response = await apiClient.post<TelegramLinkTokenResponse>(
      '/api/v1/users/me/telegram/link-token'
    );
    return response.data;
  },
};
