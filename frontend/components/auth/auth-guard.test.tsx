import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { AuthGuard } from './auth-guard';

// Мокаем next/navigation
const mockReplace = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
}));

// Создаём мок для auth store
const mockAuthState = {
  isAuthenticated: false,
  accessToken: null as string | null,
};

vi.mock('@/lib/store/auth-store', () => ({
  useAuthStore: Object.assign(
    () => mockAuthState,
    {
      getState: () => mockAuthState,
    }
  ),
}));

describe('AuthGuard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    mockAuthState.isAuthenticated = false;
    mockAuthState.accessToken = null;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows skeleton while loading', () => {
    mockAuthState.isAuthenticated = true;
    mockAuthState.accessToken = 'valid-token';

    render(
      <AuthGuard>
        <div data-testid="protected-content">Protected</div>
      </AuthGuard>
    );

    // Должен показать скелет сразу (до гидратации)
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
  });

  it('redirects to login when not authenticated', async () => {
    mockAuthState.isAuthenticated = false;
    mockAuthState.accessToken = null;

    render(
      <AuthGuard>
        <div data-testid="protected-content">Protected</div>
      </AuthGuard>
    );

    // Пропускаем таймаут гидратации (150ms)
    await act(async () => {
      vi.advanceTimersByTime(200);
    });

    expect(mockReplace).toHaveBeenCalledWith('/login');
  });

  it('redirects when accessToken is missing', async () => {
    mockAuthState.isAuthenticated = true;
    mockAuthState.accessToken = null;

    render(
      <AuthGuard>
        <div data-testid="protected-content">Protected</div>
      </AuthGuard>
    );

    await act(async () => {
      vi.advanceTimersByTime(200);
    });

    expect(mockReplace).toHaveBeenCalledWith('/login');
  });

  it('renders children when authenticated', async () => {
    mockAuthState.isAuthenticated = true;
    mockAuthState.accessToken = 'valid-token';

    render(
      <AuthGuard>
        <div data-testid="protected-content">Protected</div>
      </AuthGuard>
    );

    // Пропускаем таймаут гидратации
    await act(async () => {
      vi.advanceTimersByTime(200);
    });

    expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('does not redirect before hydration timeout', () => {
    mockAuthState.isAuthenticated = false;
    mockAuthState.accessToken = null;

    render(
      <AuthGuard>
        <div data-testid="protected-content">Protected</div>
      </AuthGuard>
    );

    // Продвигаем время, но не достаточно для срабатывания таймаута
    vi.advanceTimersByTime(100);

    // Редирект ещё не должен произойти
    expect(mockReplace).not.toHaveBeenCalled();
  });
});
