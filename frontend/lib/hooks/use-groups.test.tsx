import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMyGroups, useGroup, useJoinGroup, useLeaveGroup } from './use-groups';
import { groupsApi } from '@/lib/api/groups';
import { useAuthStore } from '@/lib/store/auth-store';
import { createMockGroup, createMockJoinGroupResponse } from '@/lib/test/mock-factories';

// Мокаем auth store
vi.mock('@/lib/store/auth-store');

// Мокаем groups API
vi.mock('@/lib/api/groups');

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

describe('useMyGroups', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuthStore).mockReturnValue({ isAuthenticated: true } as ReturnType<typeof useAuthStore>);
  });

  it('fetches user groups when authenticated', async () => {
    const mockData = [
      createMockGroup({ id: '1', name: 'Group 1' }),
      createMockGroup({ id: '2', name: 'Group 2' }),
    ];
    vi.mocked(groupsApi.getMyGroups).mockResolvedValue(mockData);

    const { result } = renderHook(() => useMyGroups(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockData);
  });

  it('does not fetch when not authenticated', () => {
    vi.mocked(useAuthStore).mockReturnValue({ isAuthenticated: false } as ReturnType<typeof useAuthStore>);

    const { result } = renderHook(() => useMyGroups(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(groupsApi.getMyGroups).not.toHaveBeenCalled();
  });
});

describe('useGroup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuthStore).mockReturnValue({ isAuthenticated: true } as ReturnType<typeof useAuthStore>);
  });

  it('fetches group by id when authenticated', async () => {
    const mockGroup = createMockGroup({ id: '1', name: 'Test Group' });
    vi.mocked(groupsApi.getById).mockResolvedValue(mockGroup);

    const { result } = renderHook(() => useGroup('1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockGroup);
  });

  it('does not fetch when id is empty', () => {
    const { result } = renderHook(() => useGroup(''), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(groupsApi.getById).not.toHaveBeenCalled();
  });

  it('does not fetch when not authenticated', () => {
    vi.mocked(useAuthStore).mockReturnValue({ isAuthenticated: false } as ReturnType<typeof useAuthStore>);

    const { result } = renderHook(() => useGroup('1'), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(groupsApi.getById).not.toHaveBeenCalled();
  });
});

describe('useJoinGroup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('joins group with invite code', async () => {
    const mockResponse = createMockJoinGroupResponse({ groupId: '1', groupName: 'Test Group' });
    vi.mocked(groupsApi.join).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => useJoinGroup(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('invite-code-123');
    });

    expect(groupsApi.join).toHaveBeenCalled();
    expect(vi.mocked(groupsApi.join).mock.calls[0][0]).toBe('invite-code-123');
  });

  it('handles join error', async () => {
    vi.mocked(groupsApi.join).mockRejectedValue(new Error('Invalid invite code'));

    const { result } = renderHook(() => useJoinGroup(), {
      wrapper: createWrapper(),
    });

    await expect(result.current.mutateAsync('invalid-code')).rejects.toThrow();
  });
});

describe('useLeaveGroup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('leaves group', async () => {
    vi.mocked(groupsApi.leave).mockResolvedValue();

    const { result } = renderHook(() => useLeaveGroup(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('1');
    });

    expect(groupsApi.leave).toHaveBeenCalled();
    expect(vi.mocked(groupsApi.leave).mock.calls[0][0]).toBe('1');
  });
});
