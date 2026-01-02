import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/lib/store/auth-store';
import { useOrganizationStore } from '@/lib/store/organization-store';
import { ROUTES } from '@/lib/routes';
import type {
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  TelegramAuthRequest,
} from '@/lib/api/types';
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
  const { clear: clearOrganization } = useOrganizationStore();
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (response) => {
      // Очищаем organization store от предыдущего пользователя
      clearOrganization();
      // refreshToken передаётся через httpOnly cookie, не храним в store
      login(response.user, response.accessToken);
      queryClient.invalidateQueries({ queryKey: ['user'] });
      toast.success('Вы успешно вошли в систему');
      router.push('/dashboard');
    },
    // Убираем onError — обработка в форме для лучшего UX
  });
}

export function useRegister() {
  const router = useRouter();

  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: () => {
      toast.success('Регистрация успешна! Проверьте email для подтверждения');
      router.push(ROUTES.VERIFY_EMAIL_SENT);
    },
    // Убираем onError — обработка в форме для лучшего UX
  });
}

export function useLogout() {
  const { logout } = useAuthStore();
  const { clear: clearOrganization } = useOrganizationStore();
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSuccess: () => {
      logout();
      clearOrganization();
      queryClient.clear();
      toast.success('Вы вышли из системы');
      router.push('/login');
    },
    onError: () => {
      // Даже при ошибке logout локально
      logout();
      clearOrganization();
      queryClient.clear();
      router.push('/login');
    },
  });
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (data: ForgotPasswordRequest) => authApi.forgotPassword(data),
    onSuccess: () => {
      toast.success('Инструкции отправлены на email');
    },
    // Не показываем ошибку — для безопасности всегда "успех"
  });
}

export function useResetPassword() {
  const router = useRouter();

  return useMutation({
    mutationFn: (data: ResetPasswordRequest) => authApi.resetPassword(data),
    onSuccess: () => {
      toast.success('Пароль успешно изменён');
      router.push('/login?reset=success');
    },
    // Ошибки обрабатываются в форме
  });
}

export function useTelegramAuth() {
  const { login } = useAuthStore();
  const { clear: clearOrganization } = useOrganizationStore();
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: (data: TelegramAuthRequest) => authApi.telegramAuth(data),
    onSuccess: (response) => {
      // Очищаем organization store от предыдущего пользователя
      clearOrganization();
      // refreshToken передаётся через httpOnly cookie, не храним в store
      login(response.user, response.accessToken);
      queryClient.invalidateQueries({ queryKey: ['user'] });
      toast.success('Вы успешно вошли через Telegram');
      router.push('/dashboard');
    },
    // Ошибки обрабатываются в компоненте
  });
}

export function useResendVerification() {
  return useMutation({
    mutationFn: (email: string) => authApi.resendVerification({ email }),
    onSuccess: () => {
      toast.success('Письмо с подтверждением отправлено');
    },
    onError: () => {
      toast.error('Не удалось отправить письмо. Попробуйте позже');
    },
  });
}
