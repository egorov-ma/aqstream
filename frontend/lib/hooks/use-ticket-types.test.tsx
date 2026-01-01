import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useTicketTypes,
  useTicketType,
  useCreateTicketType,
  useUpdateTicketType,
  useDeleteTicketType,
  useDeactivateTicketType,
} from './use-ticket-types';
import { ticketTypesApi } from '@/lib/api/ticket-types';
import { createMockTicketType } from '@/lib/test/mock-factories';

// Мокаем ticket-types API
vi.mock('@/lib/api/ticket-types');

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

describe('useTicketTypes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches ticket types for event', async () => {
    const mockData = [
      createMockTicketType({ id: '1', name: 'Standard', priceCents: 0 }),
      createMockTicketType({ id: '2', name: 'VIP', priceCents: 10000 }),
    ];
    vi.mocked(ticketTypesApi.list).mockResolvedValue(mockData);

    const { result } = renderHook(() => useTicketTypes('event-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockData);
    expect(ticketTypesApi.list).toHaveBeenCalledWith('event-1');
  });

  it('does not fetch when eventId is undefined', () => {
    const { result } = renderHook(() => useTicketTypes(undefined), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(ticketTypesApi.list).not.toHaveBeenCalled();
  });
});

describe('useTicketType', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches single ticket type', async () => {
    const mockTicketType = createMockTicketType({ id: '1', name: 'Standard', priceCents: 0 });
    vi.mocked(ticketTypesApi.getById).mockResolvedValue(mockTicketType);

    const { result } = renderHook(() => useTicketType('event-1', 'tt-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockTicketType);
    expect(ticketTypesApi.getById).toHaveBeenCalledWith('event-1', 'tt-1');
  });

  it('does not fetch when eventId is undefined', () => {
    const { result } = renderHook(() => useTicketType(undefined, 'tt-1'), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(ticketTypesApi.getById).not.toHaveBeenCalled();
  });

  it('does not fetch when ticketTypeId is undefined', () => {
    const { result } = renderHook(() => useTicketType('event-1', undefined), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(ticketTypesApi.getById).not.toHaveBeenCalled();
  });
});

describe('useCreateTicketType', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates ticket type', async () => {
    const newTicketType = createMockTicketType({ id: '1', name: 'New Type', priceCents: 0 });
    vi.mocked(ticketTypesApi.create).mockResolvedValue(newTicketType);

    const { result } = renderHook(() => useCreateTicketType('event-1'), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        name: 'New Type',
        description: 'Description',
        quantity: 100,
      });
    });

    expect(ticketTypesApi.create).toHaveBeenCalledWith('event-1', {
      name: 'New Type',
      description: 'Description',
      quantity: 100,
    });
  });

  it('handles create error', async () => {
    vi.mocked(ticketTypesApi.create).mockRejectedValue(new Error('Create failed'));

    const { result } = renderHook(() => useCreateTicketType('event-1'), {
      wrapper: createWrapper(),
    });

    await expect(
      result.current.mutateAsync({ name: 'Test', description: '', quantity: 10 })
    ).rejects.toThrow();
  });
});

describe('useUpdateTicketType', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('updates ticket type', async () => {
    vi.mocked(ticketTypesApi.update).mockResolvedValue(createMockTicketType());

    const { result } = renderHook(() => useUpdateTicketType('event-1'), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync({
        ticketTypeId: 'tt-1',
        data: { name: 'Updated Name' },
      });
    });

    expect(ticketTypesApi.update).toHaveBeenCalledWith('event-1', 'tt-1', {
      name: 'Updated Name',
    });
  });
});

describe('useDeleteTicketType', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('deletes ticket type', async () => {
    vi.mocked(ticketTypesApi.delete).mockResolvedValue();

    const { result } = renderHook(() => useDeleteTicketType('event-1'), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('tt-1');
    });

    expect(ticketTypesApi.delete).toHaveBeenCalledWith('event-1', 'tt-1');
  });
});

describe('useDeactivateTicketType', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('deactivates ticket type', async () => {
    vi.mocked(ticketTypesApi.deactivate).mockResolvedValue(createMockTicketType({ isActive: false }));

    const { result } = renderHook(() => useDeactivateTicketType('event-1'), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('tt-1');
    });

    expect(ticketTypesApi.deactivate).toHaveBeenCalledWith('event-1', 'tt-1');
  });
});
