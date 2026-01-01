import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useUploadMedia, useUploadAvatar, useUploadEventCover, useDeleteMedia } from './use-media';
import { mediaApi } from '@/lib/api/media';
import { createMockUploadResponse } from '@/lib/test/mock-factories';

// Мокаем media API
vi.mock('@/lib/api/media');

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

function createMockFile(name = 'test.jpg', type = 'image/jpeg') {
  return new File(['test content'], name, { type });
}

describe('useUploadMedia', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('uploads file successfully', async () => {
    const mockResult = createMockUploadResponse();
    vi.mocked(mediaApi.upload).mockResolvedValue(mockResult);

    const { result } = renderHook(() => useUploadMedia(), {
      wrapper: createWrapper(),
    });

    const file = createMockFile();

    await act(async () => {
      const res = await result.current.mutateAsync({ file });
      expect(res).toEqual(mockResult);
    });

    expect(mediaApi.upload).toHaveBeenCalledWith(file, undefined);
  });

  it('uploads file with purpose', async () => {
    const mockResult = createMockUploadResponse();
    vi.mocked(mediaApi.upload).mockResolvedValue(mockResult);

    const { result } = renderHook(() => useUploadMedia(), {
      wrapper: createWrapper(),
    });

    const file = createMockFile();

    await act(async () => {
      await result.current.mutateAsync({ file, purpose: 'EVENT_COVER' });
    });

    expect(mediaApi.upload).toHaveBeenCalledWith(file, 'EVENT_COVER');
  });

  it('handles upload error', async () => {
    vi.mocked(mediaApi.upload).mockRejectedValue(new Error('Upload failed'));

    const { result } = renderHook(() => useUploadMedia(), {
      wrapper: createWrapper(),
    });

    const file = createMockFile();

    await expect(result.current.mutateAsync({ file })).rejects.toThrow();
  });
});

describe('useUploadAvatar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('uploads avatar successfully', async () => {
    const mockResult = createMockUploadResponse({ filename: 'avatar.jpg' });
    vi.mocked(mediaApi.uploadAvatar).mockResolvedValue(mockResult);

    const { result } = renderHook(() => useUploadAvatar(), {
      wrapper: createWrapper(),
    });

    const file = createMockFile('avatar.jpg');

    await act(async () => {
      await result.current.mutateAsync(file);
    });

    expect(mediaApi.uploadAvatar).toHaveBeenCalledWith(file);
  });
});

describe('useUploadEventCover', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('uploads event cover successfully', async () => {
    const mockResult = createMockUploadResponse({ filename: 'cover.jpg' });
    vi.mocked(mediaApi.uploadEventCover).mockResolvedValue(mockResult);

    const { result } = renderHook(() => useUploadEventCover(), {
      wrapper: createWrapper(),
    });

    const file = createMockFile('cover.jpg');

    await act(async () => {
      await result.current.mutateAsync(file);
    });

    expect(mediaApi.uploadEventCover).toHaveBeenCalledWith(file);
  });
});

describe('useDeleteMedia', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('deletes file successfully', async () => {
    vi.mocked(mediaApi.delete).mockResolvedValue(undefined);

    const { result } = renderHook(() => useDeleteMedia(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await result.current.mutateAsync('file-1');
    });

    expect(mediaApi.delete).toHaveBeenCalledWith('file-1');
  });
});
