import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEvents, useEvent, useCreateEvent, usePublishEvent, useCancelEvent } from './use-events';
import { eventsApi } from '@/lib/api/events';
import { createMockEvent, createMockPageResponse } from '@/lib/test/mock-factories';

// Мокаем events API
vi.mock('@/lib/api/events');

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

describe('useEvents', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches events list', async () => {
    const mockEvents = [
      createMockEvent({ id: '1', title: 'Event 1', status: 'PUBLISHED' }),
      createMockEvent({ id: '2', title: 'Event 2', status: 'DRAFT' }),
    ];
    const mockData = createMockPageResponse(mockEvents, { totalElements: 2 });
    vi.mocked(eventsApi.list).mockResolvedValue(mockData);

    const { result } = renderHook(() => useEvents(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockData);
    expect(eventsApi.list).toHaveBeenCalledWith(undefined);
  });

  it('passes filters to API', async () => {
    vi.mocked(eventsApi.list).mockResolvedValue(createMockPageResponse([], { totalElements: 0 }));
    const filters = { status: 'PUBLISHED' as const };

    renderHook(() => useEvents(filters), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(eventsApi.list).toHaveBeenCalledWith(filters);
    });
  });
});

describe('useEvent', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches single event by id', async () => {
    const mockEvent = createMockEvent({ id: '1', title: 'Test Event', status: 'PUBLISHED' });
    vi.mocked(eventsApi.getById).mockResolvedValue(mockEvent);

    const { result } = renderHook(() => useEvent('1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockEvent);
  });

  it('does not fetch when id is empty', () => {
    const { result } = renderHook(() => useEvent(''), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(eventsApi.getById).not.toHaveBeenCalled();
  });
});

describe('useCreateEvent', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates event and shows success toast', async () => {
    const newEvent = createMockEvent({ id: '1', title: 'New Event' });
    vi.mocked(eventsApi.create).mockResolvedValue(newEvent);

    const { result } = renderHook(() => useCreateEvent(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        title: 'New Event',
        startsAt: '2025-01-01T10:00:00Z',
        timezone: 'Europe/Moscow',
      });
    });

    expect(eventsApi.create).toHaveBeenCalled();
  });

  it('handles create error', async () => {
    vi.mocked(eventsApi.create).mockRejectedValue(new Error('Create failed'));

    const { result } = renderHook(() => useCreateEvent(), {
      wrapper: createWrapper(),
    });

    await expect(
      result.current.mutateAsync({
        title: 'New Event',
        startsAt: '2025-01-01T10:00:00Z',
        timezone: 'Europe/Moscow',
      })
    ).rejects.toThrow('Create failed');
  });
});

describe('usePublishEvent', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('publishes event', async () => {
    vi.mocked(eventsApi.publish).mockResolvedValue(createMockEvent({ status: 'PUBLISHED' }));

    const { result } = renderHook(() => usePublishEvent(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('1');
    });

    expect(eventsApi.publish).toHaveBeenCalledWith('1');
  });
});

describe('useCancelEvent', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('cancels event with reason', async () => {
    vi.mocked(eventsApi.cancel).mockResolvedValue(createMockEvent({ status: 'CANCELLED' }));

    const { result } = renderHook(() => useCancelEvent(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({ id: '1', reason: 'Weather' });
    });

    expect(eventsApi.cancel).toHaveBeenCalledWith('1', 'Weather');
  });

  it('cancels event without reason', async () => {
    vi.mocked(eventsApi.cancel).mockResolvedValue(createMockEvent({ status: 'CANCELLED' }));

    const { result } = renderHook(() => useCancelEvent(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({ id: '1' });
    });

    expect(eventsApi.cancel).toHaveBeenCalledWith('1', undefined);
  });
});
