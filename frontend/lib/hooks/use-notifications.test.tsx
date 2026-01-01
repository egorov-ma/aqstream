import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useNotifications,
  useUnreadCount,
  useMarkAsRead,
  useMarkAllAsRead,
  useNotificationPreferences,
  useUpdateNotificationPreferences,
} from './use-notifications';
import { notificationsApi } from '@/lib/api/notifications';
import { useAuthStore } from '@/lib/store/auth-store';
import {
  createMockUserNotification,
  createMockPageResponse,
  createMockNotificationPreferences,
} from '@/lib/test/mock-factories';

// Мокаем auth store
vi.mock('@/lib/store/auth-store');

// Мокаем notifications API
vi.mock('@/lib/api/notifications');

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

describe('useNotifications', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuthStore).mockReturnValue({
      isAuthenticated: true,
    } as ReturnType<typeof useAuthStore>);
  });

  it('fetches notifications when authenticated', async () => {
    const mockNotification = createMockUserNotification({ id: '1', title: 'Notification 1' });
    const mockData = createMockPageResponse([mockNotification], { totalElements: 1 });
    vi.mocked(notificationsApi.list).mockResolvedValue(mockData);

    const { result } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockData);
    expect(notificationsApi.list).toHaveBeenCalledWith(0, 20);
  });

  it('passes custom pagination', async () => {
    vi.mocked(notificationsApi.list).mockResolvedValue(
      createMockPageResponse([], { totalElements: 0 })
    );

    renderHook(() => useNotifications(2, 10), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(notificationsApi.list).toHaveBeenCalledWith(2, 10);
    });
  });

  it('does not fetch when not authenticated', () => {
    vi.mocked(useAuthStore).mockReturnValue({
      isAuthenticated: false,
    } as ReturnType<typeof useAuthStore>);

    const { result } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(notificationsApi.list).not.toHaveBeenCalled();
  });
});

describe('useUnreadCount', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuthStore).mockReturnValue({
      isAuthenticated: true,
    } as ReturnType<typeof useAuthStore>);
  });

  it('fetches unread count', async () => {
    vi.mocked(notificationsApi.getUnreadCount).mockResolvedValue(5);

    const { result } = renderHook(() => useUnreadCount(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(5);
  });
});

describe('useMarkAsRead', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('marks notification as read', async () => {
    vi.mocked(notificationsApi.markAsRead).mockResolvedValue();

    const { result } = renderHook(() => useMarkAsRead(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('1');
    });

    expect(notificationsApi.markAsRead).toHaveBeenCalled();
    expect(vi.mocked(notificationsApi.markAsRead).mock.calls[0][0]).toBe('1');
  });
});

describe('useMarkAllAsRead', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('marks all notifications as read', async () => {
    vi.mocked(notificationsApi.markAllAsRead).mockResolvedValue();

    const { result } = renderHook(() => useMarkAllAsRead(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(notificationsApi.markAllAsRead).toHaveBeenCalled();
  });
});

describe('useNotificationPreferences', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuthStore).mockReturnValue({
      isAuthenticated: true,
    } as ReturnType<typeof useAuthStore>);
  });

  it('fetches notification preferences', async () => {
    const mockPrefs = createMockNotificationPreferences({
      settings: {
        eventReminders: true,
        registrationUpdates: false,
        organizationNews: true,
      },
    });
    vi.mocked(notificationsApi.getPreferences).mockResolvedValue(mockPrefs);

    const { result } = renderHook(() => useNotificationPreferences(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockPrefs);
  });
});

describe('useUpdateNotificationPreferences', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('updates notification preferences', async () => {
    const updatedPrefs = createMockNotificationPreferences({
      settings: {
        eventReminders: false,
        registrationUpdates: true,
        organizationNews: false,
      },
    });
    vi.mocked(notificationsApi.updatePreferences).mockResolvedValue(updatedPrefs);

    const { result } = renderHook(() => useUpdateNotificationPreferences(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        settings: {
          eventReminders: false,
          registrationUpdates: true,
          organizationNews: false,
        },
      });
    });

    expect(notificationsApi.updatePreferences).toHaveBeenCalled();
  });
});
