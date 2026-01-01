import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useUser, useLogin, useRegister, useLogout, useForgotPassword } from './use-auth';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/lib/store/auth-store';
import { createMockUser, createMockLoginResponse } from '@/lib/test/mock-factories';

// Мокаем auth store
vi.mock('@/lib/store/auth-store');

// Мокаем auth API
vi.mock('@/lib/api/auth');

// Мокаем next/navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}));

// Мокаем sonner
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  // eslint-disable-next-line react/display-name
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

// Хелпер для создания мок-стейта auth store
const mockLogin = vi.fn();
const mockLogout = vi.fn();

function mockAuthStoreState(isAuthenticated: boolean) {
  vi.mocked(useAuthStore).mockReturnValue({
    isAuthenticated,
    login: mockLogin,
    logout: mockLogout,
  } as unknown as ReturnType<typeof useAuthStore>);
}

describe('useUser', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthStoreState(false);
  });

  it('does not fetch when not authenticated', () => {
    mockAuthStoreState(false);

    const { result } = renderHook(() => useUser(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(authApi.me).not.toHaveBeenCalled();
  });

  it('fetches user when authenticated', async () => {
    mockAuthStoreState(true);
    const mockUser = createMockUser({ id: '1', email: 'test@example.com', firstName: 'Test' });
    vi.mocked(authApi.me).mockResolvedValue(mockUser);

    const { result } = renderHook(() => useUser(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockUser);
  });
});

describe('useLogin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthStoreState(false);
  });

  it('calls login API with credentials', async () => {
    const mockUser = createMockUser({ id: '1', email: 'test@example.com' });
    const mockResponse = createMockLoginResponse({
      user: mockUser,
      accessToken: 'token123',
    });
    vi.mocked(authApi.login).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => useLogin(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        email: 'test@example.com',
        password: 'password123',
      });
    });

    expect(authApi.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'password123',
    });
    expect(mockLogin).toHaveBeenCalledWith(mockResponse.user, mockResponse.accessToken);
    expect(mockPush).toHaveBeenCalledWith('/dashboard');
  });

  it('handles login error', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('Invalid credentials'));

    const { result } = renderHook(() => useLogin(), {
      wrapper: createWrapper(),
    });

    await expect(
      result.current.mutateAsync({
        email: 'test@example.com',
        password: 'wrong',
      })
    ).rejects.toThrow('Invalid credentials');
  });
});

describe('useRegister', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('registers user and redirects', async () => {
    // register возвращает сообщение о необходимости подтверждения email
    vi.mocked(authApi.register).mockResolvedValue(createMockLoginResponse());

    const { result } = renderHook(() => useRegister(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        email: 'new@example.com',
        password: 'password123',
        firstName: 'New',
        lastName: 'User',
      });
    });

    expect(authApi.register).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith('/verify-email-sent');
  });
});

describe('useLogout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthStoreState(true);
  });

  it('logs out user and redirects', async () => {
    vi.mocked(authApi.logout).mockResolvedValue();

    const { result } = renderHook(() => useLogout(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(authApi.logout).toHaveBeenCalled();
    expect(mockLogout).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith('/login');
  });

  it('logs out locally even on API error', async () => {
    vi.mocked(authApi.logout).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useLogout(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      try {
        await result.current.mutateAsync();
      } catch {
        // Expected to throw
      }
    });

    // Should still logout locally
    expect(mockLogout).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith('/login');
  });
});

describe('useForgotPassword', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends forgot password request', async () => {
    vi.mocked(authApi.forgotPassword).mockResolvedValue();

    const { result } = renderHook(() => useForgotPassword(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({ email: 'test@example.com' });
    });

    expect(authApi.forgotPassword).toHaveBeenCalledWith({
      email: 'test@example.com',
    });
  });
});
