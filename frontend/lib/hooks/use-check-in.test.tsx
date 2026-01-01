import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useCheckInInfo, useConfirmCheckIn, useSyncCheckIns } from './use-check-in';
import { checkInApi } from '@/lib/api/check-in';
import { createMockCheckInInfo, createMockCheckInResult } from '@/lib/test/mock-factories';

// Мокаем check-in API
vi.mock('@/lib/api/check-in');

// Мокаем offline storage
vi.mock('@/lib/pwa/offline-storage', () => ({
  savePendingCheckIn: vi.fn(),
  removePendingCheckIn: vi.fn(),
  getPendingCheckIns: vi.fn().mockResolvedValue([]),
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

describe('useCheckInInfo', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches check-in info when code provided', async () => {
    const mockInfo = createMockCheckInInfo({
      registrationId: '1',
      eventId: 'e1',
      eventTitle: 'Test Event',
      ticketTypeName: 'Standard',
      firstName: 'John',
      lastName: 'Doe',
      isCheckedIn: false,
    });
    vi.mocked(checkInApi.getInfo).mockResolvedValue(mockInfo);

    const { result } = renderHook(() => useCheckInInfo('ABC123'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockInfo);
    expect(checkInApi.getInfo).toHaveBeenCalledWith('ABC123');
  });

  it('does not fetch when code is null', () => {
    const { result } = renderHook(() => useCheckInInfo(null), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(checkInApi.getInfo).not.toHaveBeenCalled();
  });

  it('handles invalid code error', async () => {
    vi.mocked(checkInApi.getInfo).mockRejectedValue(new Error('Invalid code'));

    const { result } = renderHook(() => useCheckInInfo('INVALID'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeDefined();
  });
});

describe('useConfirmCheckIn', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Default: online
    Object.defineProperty(navigator, 'onLine', {
      value: true,
      configurable: true,
    });
  });

  it('confirms check-in online', async () => {
    const mockResult = createMockCheckInResult({
      registrationId: '1',
      confirmationCode: 'ABC123',
      message: 'Check-in успешен',
    });
    vi.mocked(checkInApi.confirm).mockResolvedValue(mockResult);

    const { result } = renderHook(() => useConfirmCheckIn(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('ABC123');
    });

    expect(checkInApi.confirm).toHaveBeenCalledWith('ABC123');
  });

  it('handles check-in error', async () => {
    vi.mocked(checkInApi.confirm).mockRejectedValue(new Error('Already checked in'));

    const { result } = renderHook(() => useConfirmCheckIn(), {
      wrapper: createWrapper(),
    });

    await expect(result.current.mutateAsync('ABC123')).rejects.toThrow();
  });
});

describe('useSyncCheckIns', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('syncs pending check-ins', async () => {
    const { getPendingCheckIns } = await import('@/lib/pwa/offline-storage');
    vi.mocked(getPendingCheckIns).mockResolvedValue([]);

    const { result } = renderHook(() => useSyncCheckIns(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync();
    });

    expect(getPendingCheckIns).toHaveBeenCalled();
  });
});
