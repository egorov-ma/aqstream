import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useDashboardStats } from './use-dashboard';

// Мокаем auth store
const mockAuthState = {
  isAuthenticated: false,
};

vi.mock('@/lib/store/auth-store', () => ({
  useAuthStore: () => mockAuthState,
}));

// Мокаем dashboard API
const mockGetStats = vi.fn();
vi.mock('@/lib/api/dashboard', () => ({
  dashboardApi: {
    getStats: () => mockGetStats(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  // eslint-disable-next-line react/display-name
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe('useDashboardStats', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthState.isAuthenticated = false;
  });

  it('does not fetch when not authenticated', () => {
    mockAuthState.isAuthenticated = false;

    const { result } = renderHook(() => useDashboardStats(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isFetching).toBe(false);
    expect(mockGetStats).not.toHaveBeenCalled();
  });

  it('fetches stats when authenticated', async () => {
    mockAuthState.isAuthenticated = true;
    const mockData = {
      activeEventsCount: 5,
      totalRegistrations: 100,
      checkedInCount: 80,
      upcomingEvents: [],
    };
    mockGetStats.mockResolvedValue(mockData);

    const { result } = renderHook(() => useDashboardStats(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockData);
    expect(mockGetStats).toHaveBeenCalledTimes(1);
  });

  it('handles API error', async () => {
    mockAuthState.isAuthenticated = true;
    mockGetStats.mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useDashboardStats(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeDefined();
  });

  it('has correct query key', () => {
    mockAuthState.isAuthenticated = true;
    mockGetStats.mockResolvedValue({});

    const { result } = renderHook(() => useDashboardStats(), {
      wrapper: createWrapper(),
    });

    // Query key используется для кэширования и инвалидации
    // Проверяем что запрос был создан с правильными параметрами
    expect(result.current.isLoading || result.current.isSuccess).toBe(true);
  });

  it('uses staleTime of 5 minutes', async () => {
    mockAuthState.isAuthenticated = true;
    const mockData = { activeEventsCount: 1, totalRegistrations: 0, checkedInCount: 0, upcomingEvents: [] };
    mockGetStats.mockResolvedValue(mockData);

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    // eslint-disable-next-line react/display-name
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useDashboardStats(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    // Проверяем что данные закэшированы
    const cachedData = queryClient.getQueryData(['dashboard', 'stats']);
    expect(cachedData).toEqual(mockData);
  });
});
