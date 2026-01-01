import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';

import { profileApi } from '@/lib/api/profile';
import { useAuthStore } from '@/lib/store/auth-store';
import type { UpdateProfileRequest, ChangePasswordRequest } from '@/lib/api/types';

/**
 * Хук для обновления профиля пользователя.
 */
export function useUpdateProfile() {
  const queryClient = useQueryClient();
  const { setUser } = useAuthStore();

  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => profileApi.updateProfile(data),
    onSuccess: (updatedUser) => {
      // Обновляем пользователя в store
      setUser(updatedUser);
      // Инвалидируем кеш
      queryClient.invalidateQueries({ queryKey: ['user', 'me'] });
      toast.success('Профиль обновлён');
    },
    onError: () => {
      toast.error('Ошибка при обновлении профиля');
    },
  });
}

/**
 * Хук для смены пароля пользователя.
 */
export function useChangePassword() {
  return useMutation({
    mutationFn: (data: ChangePasswordRequest) => profileApi.changePassword(data),
    onSuccess: () => {
      toast.success('Пароль изменён');
    },
    // Ошибки обрабатываются в компоненте для показа конкретных сообщений
  });
}
