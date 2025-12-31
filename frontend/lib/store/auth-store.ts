import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '@/lib/api/types';

/**
 * Состояние аутентификации.
 *
 * ВАЖНО: refreshToken НЕ хранится на клиенте — он передаётся через httpOnly cookie.
 * Это защищает от XSS атак.
 */
interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  setUser: (user: User) => void;
  setAccessToken: (accessToken: string) => void;
  login: (user: User, accessToken: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,

      setUser: (user) => set({ user }),

      setAccessToken: (accessToken) => set({ accessToken }),

      login: (user, accessToken) =>
        set({
          user,
          accessToken,
          isAuthenticated: true,
        }),

      logout: () =>
        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
        }),
    }),
    {
      name: 'auth-storage',
      // Не сохраняем refreshToken — он в httpOnly cookie
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
