import { apiClient } from './client';
import type { UploadResponse } from './types';

// API клиент для работы с медиафайлами

export const mediaApi = {
  /**
   * Загрузить изображение
   * @param file - файл для загрузки
   * @returns URL загруженного файла и метаданные
   */
  upload: async (file: File): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<UploadResponse>(
      '/api/v1/media/upload',
      formData
    );
    return response.data;
  },

  /**
   * Загрузить изображение обложки события
   * @param eventId - ID события (опционально, для привязки к событию)
   * @param file - файл изображения
   */
  uploadEventCover: async (
    file: File,
    eventId?: string
  ): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (eventId) {
      formData.append('entityType', 'event');
      formData.append('entityId', eventId);
    }

    const response = await apiClient.post<UploadResponse>(
      '/api/v1/media/upload',
      formData
    );
    return response.data;
  },

  /**
   * Удалить файл по URL
   * @param url - URL файла для удаления
   */
  delete: async (url: string): Promise<void> => {
    await apiClient.delete('/api/v1/media', {
      params: { url },
    });
  },
};
