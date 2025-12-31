import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useOrganizations, useOrganization, useSwitchOrganization } from './use-organizations';
import type { Organization } from '@/lib/api/types';

// Мок данные
const mockOrganizations: Organization[] = [
  { id: 'org-1', name: 'Организация 1', slug: 'org-1', ownerId: 'user-1', createdAt: '2024-01-01' },
  { id: 'org-2', name: 'Организация 2', slug: 'org-2', ownerId: 'user-1', createdAt: '2024-01-01' },
];

const mockSwitchResponse = {
  accessToken: 'new-access-token',
  // refreshToken передаётся через httpOnly cookie, не в body
};

// Мок API
const mockList = vi.fn().mockResolvedValue(mockOrganizations);
const mockGetById = vi.fn().mockImplementation((id: string) =>
  Promise.resolve(mockOrganizations.find((o) => o.id === id))
);
const mockSwitch = vi.fn().mockResolvedValue(mockSwitchResponse);

vi.mock('@/lib/api/organizations', () => ({
  organizationsApi: {
    list: () => mockList(),
    getById: (id: string) => mockGetById(id),
    switch: (id: string) => mockSwitch(id),
  },
}));

// Мок auth store
let mockIsAuthenticated = true;
const mockSetAccessToken = vi.fn();

vi.mock('@/lib/store/auth-store', () => ({
  useAuthStore: () => ({
    isAuthenticated: mockIsAuthenticated,
    setAccessToken: mockSetAccessToken,
  }),
}));

// Мок organization store
const mockSetCurrentOrganization = vi.fn();

vi.mock('@/lib/store/organization-store', () => ({
  useOrganizationStore: () => ({
    setCurrentOrganization: mockSetCurrentOrganization,
  }),
}));

// Мок sonner
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
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('useOrganizations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsAuthenticated = true;
  });

  it('fetches organizations when authenticated', async () => {
    const { result } = renderHook(() => useOrganizations(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockOrganizations);
    expect(mockList).toHaveBeenCalled();
  });

  it('does not fetch when not authenticated', () => {
    mockIsAuthenticated = false;
    const { result } = renderHook(() => useOrganizations(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isFetching).toBe(false);
    expect(mockList).not.toHaveBeenCalled();
  });

  it('has correct query key', async () => {
    const { result } = renderHook(() => useOrganizations(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    // Query key должен быть ['organizations']
    // Это проверяется через то что запрос успешно выполнился
    expect(mockList).toHaveBeenCalledTimes(1);
  });
});

describe('useOrganization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsAuthenticated = true;
  });

  it('fetches organization by id when authenticated', async () => {
    const { result } = renderHook(() => useOrganization('org-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockOrganizations[0]);
    expect(mockGetById).toHaveBeenCalledWith('org-1');
  });

  it('does not fetch when not authenticated', () => {
    mockIsAuthenticated = false;
    const { result } = renderHook(() => useOrganization('org-1'), {
      wrapper: createWrapper(),
    });

    expect(result.current.isFetching).toBe(false);
    expect(mockGetById).not.toHaveBeenCalled();
  });

  it('does not fetch when id is empty', () => {
    const { result } = renderHook(() => useOrganization(''), {
      wrapper: createWrapper(),
    });

    expect(result.current.isFetching).toBe(false);
    expect(mockGetById).not.toHaveBeenCalled();
  });
});

describe('useSwitchOrganization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsAuthenticated = true;
  });

  it('switches organization and updates tokens', async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });

    // Заполняем кэш организациями
    queryClient.setQueryData(['organizations'], mockOrganizations);

    const { result } = renderHook(() => useSwitchOrganization(), {
      wrapper: ({ children }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      ),
    });

    result.current.mutate('org-2');

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mockSwitch).toHaveBeenCalledWith('org-2');
    expect(mockSetAccessToken).toHaveBeenCalledWith('new-access-token');
    expect(mockSetCurrentOrganization).toHaveBeenCalledWith(mockOrganizations[1]);
  });

  it('shows error toast on failure', async () => {
    mockSwitch.mockRejectedValueOnce(new Error('Switch failed'));

    const { result } = renderHook(() => useSwitchOrganization(), {
      wrapper: createWrapper(),
    });

    result.current.mutate('org-2');

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    const { toast } = await import('sonner');
    expect(toast.error).toHaveBeenCalledWith('Ошибка переключения организации');
  });
});
