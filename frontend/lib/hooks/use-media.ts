import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { mediaApi, type MediaPurpose } from '@/lib/api/media';

/**
 * Хук для загрузки файла.
 */
export function useUploadMedia() {
  return useMutation({
    mutationFn: ({ file, purpose }: { file: File; purpose?: MediaPurpose }) =>
      mediaApi.upload(file, purpose),
    onError: () => {
      toast.error('Ошибка при загрузке файла');
    },
  });
}

/**
 * Хук для загрузки аватара.
 */
export function useUploadAvatar() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (file: File) => mediaApi.uploadAvatar(file),
    onSuccess: () => {
      // Инвалидируем данные пользователя для обновления аватара
      queryClient.invalidateQueries({ queryKey: ['user'] });
      toast.success('Аватар загружен');
    },
    onError: () => {
      toast.error('Ошибка при загрузке аватара');
    },
  });
}

/**
 * Хук для загрузки обложки события.
 */
export function useUploadEventCover() {
  return useMutation({
    mutationFn: (file: File) => mediaApi.uploadEventCover(file),
    onSuccess: () => {
      toast.success('Обложка загружена');
    },
    onError: () => {
      toast.error('Ошибка при загрузке обложки');
    },
  });
}

/**
 * Хук для удаления файла.
 */
export function useDeleteMedia() {
  return useMutation({
    mutationFn: (id: string) => mediaApi.delete(id),
    onSuccess: () => {
      toast.success('Файл удалён');
    },
    onError: () => {
      toast.error('Ошибка при удалении файла');
    },
  });
}
