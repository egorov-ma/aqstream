import { apiClient } from './client';
import type {
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ResendVerificationRequest,
  ResetPasswordRequest,
  TelegramAuthRequest,
  User,
  VerifyEmailRequest,
} from './types';

export const authApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/login', data);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/register', data);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/api/v1/auth/logout');
  },

  me: async (): Promise<User> => {
    const response = await apiClient.get<User>('/api/v1/users/me');
    return response.data;
  },

  // Refresh вызывается автоматически через interceptor в client.ts
  // refreshToken передаётся через httpOnly cookie
  refresh: async (): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/refresh', {});
    return response.data;
  },

  forgotPassword: async (data: ForgotPasswordRequest): Promise<void> => {
    await apiClient.post('/api/v1/auth/forgot-password', data);
  },

  resetPassword: async (data: ResetPasswordRequest): Promise<void> => {
    await apiClient.post('/api/v1/auth/reset-password', data);
  },

  verifyEmail: async (data: VerifyEmailRequest): Promise<void> => {
    await apiClient.post('/api/v1/auth/verify-email', data);
  },

  resendVerification: async (data: ResendVerificationRequest): Promise<void> => {
    await apiClient.post('/api/v1/auth/resend-verification', data);
  },

  telegramAuth: async (data: TelegramAuthRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/telegram', data);
    return response.data;
  },
};
