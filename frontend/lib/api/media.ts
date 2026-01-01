import { apiClient } from './client';
import type { UploadResponse } from './types';

export type MediaPurpose = 'USER_AVATAR' | 'EVENT_COVER' | 'ORGANIZATION_LOGO' | 'GENERAL';

export const mediaApi = {
  /**
   * Загрузить файл.
   */
  upload: async (file: File, purpose: MediaPurpose = 'GENERAL'): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('purpose', purpose);

    const response = await apiClient.post<UploadResponse>('/api/v1/media', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  /**
   * Загрузить аватар пользователя.
   */
  uploadAvatar: async (file: File): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<UploadResponse>('/api/v1/media/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  /**
   * Загрузить обложку события.
   */
  uploadEventCover: async (file: File): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('purpose', 'EVENT_COVER');

    const response = await apiClient.post<UploadResponse>('/api/v1/media', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  /**
   * Удалить файл.
   */
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/media/${id}`);
  },
};
