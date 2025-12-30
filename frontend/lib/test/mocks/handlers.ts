import { http, HttpResponse } from 'msw';
import type { LoginRequest, RegisterRequest, LoginResponse } from '@/lib/api/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Тестовые данные
const mockUser = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  email: 'test@example.com',
  firstName: 'Иван',
  lastName: 'Иванов',
  avatarUrl: null,
  emailVerified: true,
  createdAt: new Date().toISOString(),
};

const mockAuthResponse: LoginResponse = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  user: mockUser,
};

export const handlers = [
  // Login - успешный сценарий
  http.post(`${API_URL}/api/v1/auth/login`, async ({ request }) => {
    const body = (await request.json()) as LoginRequest;

    // Симуляция неверных credentials
    if (body.email === 'wrong@example.com') {
      return HttpResponse.json(
        { code: 'invalid_credentials', message: 'Неверный email или пароль' },
        { status: 401 }
      );
    }

    // Симуляция заблокированного аккаунта
    if (body.email === 'locked@example.com') {
      return HttpResponse.json(
        { code: 'account_locked', message: 'Аккаунт заблокирован. Попробуйте через 15 минут' },
        { status: 403 }
      );
    }

    return HttpResponse.json(mockAuthResponse);
  }),

  // Register - успешный сценарий
  http.post(`${API_URL}/api/v1/auth/register`, async ({ request }) => {
    const body = (await request.json()) as RegisterRequest;

    // Симуляция существующего email
    if (body.email === 'exists@example.com') {
      return HttpResponse.json(
        { code: 'email_already_exists', message: 'Пользователь с таким email уже существует' },
        { status: 409 }
      );
    }

    return HttpResponse.json(
      {
        ...mockAuthResponse,
        user: { ...mockUser, email: body.email, firstName: body.firstName },
      },
      { status: 201 }
    );
  }),

  // Forgot password - всегда успех
  http.post(`${API_URL}/api/v1/auth/forgot-password`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Reset password
  http.post(`${API_URL}/api/v1/auth/reset-password`, async ({ request }) => {
    const body = (await request.json()) as { token: string; newPassword: string };

    // Симуляция невалидного токена
    if (body.token === 'invalid-token') {
      return HttpResponse.json(
        { code: 'invalid_token', message: 'Ссылка недействительна или срок её действия истёк' },
        { status: 400 }
      );
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // Telegram auth
  http.post(`${API_URL}/api/v1/auth/telegram`, () => {
    return HttpResponse.json(mockAuthResponse);
  }),

  // Logout
  http.post(`${API_URL}/api/v1/auth/logout`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Get current user
  http.get(`${API_URL}/api/v1/users/me`, () => {
    return HttpResponse.json(mockUser);
  }),
];
