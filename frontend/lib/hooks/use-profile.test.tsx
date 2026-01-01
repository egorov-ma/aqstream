import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useUpdateProfile, useChangePassword, useGenerateTelegramLinkToken } from './use-profile';
import { profileApi } from '@/lib/api/profile';
import { createMockUser, createMockTelegramLinkToken } from '@/lib/test/mock-factories';

// Мокаем auth store
const mockSetUser = vi.fn();
vi.mock('@/lib/store/auth-store', () => ({
  useAuthStore: () => ({
    setUser: mockSetUser,
  }),
}));

// Мокаем profile API
vi.mock('@/lib/api/profile');

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

describe('useUpdateProfile', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('updates profile and updates store', async () => {
    const updatedUser = createMockUser({
      id: '1',
      email: 'test@example.com',
      firstName: 'Updated',
      lastName: 'User',
    });
    vi.mocked(profileApi.updateProfile).mockResolvedValue(updatedUser);

    const { result } = renderHook(() => useUpdateProfile(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        firstName: 'Updated',
        lastName: 'User',
      });
    });

    expect(profileApi.updateProfile).toHaveBeenCalled();
    expect(mockSetUser).toHaveBeenCalledWith(updatedUser);
  });

  it('handles update error', async () => {
    vi.mocked(profileApi.updateProfile).mockRejectedValue(new Error('Update failed'));

    const { result } = renderHook(() => useUpdateProfile(), {
      wrapper: createWrapper(),
    });

    await expect(
      result.current.mutateAsync({
        firstName: 'Test',
      })
    ).rejects.toThrow();
  });
});

describe('useChangePassword', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('changes password successfully', async () => {
    vi.mocked(profileApi.changePassword).mockResolvedValue(undefined);

    const { result } = renderHook(() => useChangePassword(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        currentPassword: 'old123',
        newPassword: 'new456',
      });
    });

    expect(profileApi.changePassword).toHaveBeenCalledWith({
      currentPassword: 'old123',
      newPassword: 'new456',
    });
  });

  it('handles wrong current password', async () => {
    vi.mocked(profileApi.changePassword).mockRejectedValue(
      new Error('Incorrect current password')
    );

    const { result } = renderHook(() => useChangePassword(), {
      wrapper: createWrapper(),
    });

    await expect(
      result.current.mutateAsync({
        currentPassword: 'wrong',
        newPassword: 'new456',
      })
    ).rejects.toThrow();
  });
});

describe('useGenerateTelegramLinkToken', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('generates telegram link token', async () => {
    const mockToken = createMockTelegramLinkToken({ token: 'abc123' });
    vi.mocked(profileApi.generateTelegramLinkToken).mockResolvedValue(mockToken);

    const { result } = renderHook(() => useGenerateTelegramLinkToken(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      const res = await result.current.mutateAsync();
      expect(res).toEqual(mockToken);
    });

    expect(profileApi.generateTelegramLinkToken).toHaveBeenCalled();
  });
});
