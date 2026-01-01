import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useMyRegistrations,
  useEventRegistrations,
  useCreateRegistration,
  useCancelRegistration,
} from './use-registrations';
import { registrationsApi } from '@/lib/api/registrations';
import { createMockRegistration, createMockPageResponse } from '@/lib/test/mock-factories';

// Мокаем registrations API
vi.mock('@/lib/api/registrations');

// Мокаем sonner
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Мокаем next/navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
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

describe('useMyRegistrations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches user registrations', async () => {
    const mockReg = createMockRegistration({ id: '1', eventTitle: 'Event 1' });
    const mockData = createMockPageResponse([mockReg], { totalElements: 1 });
    vi.mocked(registrationsApi.getMy).mockResolvedValue(mockData);

    const { result } = renderHook(() => useMyRegistrations(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockData);
    expect(registrationsApi.getMy).toHaveBeenCalled();
  });

  it('passes pagination params', async () => {
    const mockData = createMockPageResponse([], { totalElements: 0 });
    vi.mocked(registrationsApi.getMy).mockResolvedValue(mockData);

    renderHook(() => useMyRegistrations({ page: 1, size: 10 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(registrationsApi.getMy).toHaveBeenCalledWith({ page: 1, size: 10 });
    });
  });
});

describe('useEventRegistrations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches event registrations', async () => {
    const mockReg = createMockRegistration({ id: '1', firstName: 'John', lastName: 'Doe' });
    const mockData = createMockPageResponse([mockReg], { totalElements: 1 });
    vi.mocked(registrationsApi.listByEvent).mockResolvedValue(mockData);

    const { result } = renderHook(() => useEventRegistrations('event-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockData);
    expect(registrationsApi.listByEvent).toHaveBeenCalledWith('event-1', undefined);
  });

  it('does not fetch when eventId is empty', () => {
    const { result } = renderHook(() => useEventRegistrations(''), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(registrationsApi.listByEvent).not.toHaveBeenCalled();
  });
});

describe('useCreateRegistration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates registration successfully', async () => {
    const mockResult = createMockRegistration({
      id: '1',
      confirmationCode: 'ABC123',
      status: 'CONFIRMED',
    });
    vi.mocked(registrationsApi.create).mockResolvedValue(mockResult);

    const { result } = renderHook(() => useCreateRegistration('event-1'), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      const res = await result.current.mutateAsync({
        ticketTypeId: 't1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
      });
      expect(res).toEqual(mockResult);
    });

    expect(registrationsApi.create).toHaveBeenCalledWith('event-1', {
      ticketTypeId: 't1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
    });
  });

  it('handles registration error', async () => {
    vi.mocked(registrationsApi.create).mockRejectedValue(new Error('No seats available'));

    const { result } = renderHook(() => useCreateRegistration('event-1'), {
      wrapper: createWrapper(),
    });

    await expect(
      result.current.mutateAsync({
        ticketTypeId: 't1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
      })
    ).rejects.toThrow();
  });
});

describe('useCancelRegistration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('cancels registration', async () => {
    vi.mocked(registrationsApi.cancel).mockResolvedValue();

    const { result } = renderHook(() => useCancelRegistration(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('reg-1');
    });

    expect(registrationsApi.cancel).toHaveBeenCalledWith('reg-1');
  });

  it('handles cancel error', async () => {
    vi.mocked(registrationsApi.cancel).mockRejectedValue(new Error('Cannot cancel'));

    const { result } = renderHook(() => useCancelRegistration(), {
      wrapper: createWrapper(),
    });

    await expect(result.current.mutateAsync('reg-1')).rejects.toThrow();
  });
});
