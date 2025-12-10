import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/lib/store/auth-store';
import type { LoginRequest, RegisterRequest } from '@/lib/api/types';
import { toast } from 'sonner';

export function useUser() {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['user', 'me'],
    queryFn: () => authApi.me(),
    enabled: isAuthenticated,
  });
}

export function useLogin() {
  const { login } = useAuthStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (response) => {
      login(response.user, response.accessToken, response.refreshToken);
      queryClient.invalidateQueries({ queryKey: ['user'] });
      toast.success('Вы успешно вошли в систему');
    },
    onError: () => {
      toast.error('Ошибка входа. Проверьте email и пароль');
    },
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: () => {
      toast.success('Регистрация успешна! Теперь войдите в систему');
    },
    onError: () => {
      toast.error('Ошибка регистрации. Попробуйте ещё раз');
    },
  });
}

export function useLogout() {
  const { logout } = useAuthStore();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSuccess: () => {
      logout();
      queryClient.clear();
      toast.success('Вы вышли из системы');
    },
    onError: () => {
      // Даже при ошибке logout локально
      logout();
      queryClient.clear();
    },
  });
}
