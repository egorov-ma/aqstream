import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/lib/store/auth-store';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
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

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;

    // 401 — попытка refresh token (только один раз)
    if (error.response?.status === 401 && originalRequest && !isRefreshing) {
      const authStore = useAuthStore.getState();

      if (authStore.refreshToken) {
        isRefreshing = true;
        try {
          const response = await axios.post(`${API_URL}/api/v1/auth/refresh`, {
            refreshToken: authStore.refreshToken,
          });

          const { accessToken, refreshToken } = response.data;
          authStore.setTokens(accessToken, refreshToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        } catch {
          // Refresh failed — logout
          authStore.logout();
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
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
