import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/lib/store/auth-store';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  // Отправлять cookies с запросами (для httpOnly refresh token)
  withCredentials: true,
});

// Request interceptor — добавляем токен
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — обработка ошибок и refresh token
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

// Обработка очереди запросов после refresh
const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    } else {
      // Token отсутствует — отклоняем запрос
      prom.reject(new Error('Не удалось обновить токен'));
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;

    // Auth endpoints не должны триггерить refresh token логику
    // (они специально возвращают 401 для неверных credentials)
    const isAuthEndpoint = originalRequest?.url?.includes('/api/v1/auth/');

    // 401 — попытка refresh token (только для НЕ-auth endpoints)
    if (error.response?.status === 401 && originalRequest && !isAuthEndpoint) {
      const authStore = useAuthStore.getState();

      // Если уже идёт refresh — ставим запрос в очередь
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(apiClient(originalRequest));
            },
            reject,
          });
        });
      }

      // Пробуем обновить токен (refresh token в httpOnly cookie отправится автоматически)
      if (authStore.isAuthenticated) {
        isRefreshing = true;
        try {
          // Refresh token передаётся автоматически через httpOnly cookie
          const response = await axios.post(
            `${API_URL}/api/v1/auth/refresh`,
            {},
            { withCredentials: true }
          );

          const { accessToken } = response.data;
          authStore.setAccessToken(accessToken);

          // Обрабатываем очередь с новым токеном
          processQueue(null, accessToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        } catch (refreshError) {
          // Refresh failed — отклоняем все запросы в очереди и logout
          processQueue(refreshError, null);
          authStore.logout();
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      } else {
        authStore.logout();
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
      }
    }

    return Promise.reject(error);
  }
);
