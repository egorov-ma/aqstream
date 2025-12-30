import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LoginForm } from './login-form';

// Мокаем next/navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}));

// Мокаем sonner
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Мокаем auth store
vi.mock('@/lib/store/auth-store', () => ({
  useAuthStore: () => ({
    login: vi.fn(),
  }),
}));

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe('LoginForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders email and password fields', () => {
    renderWithProviders(<LoginForm />);

    expect(screen.getByTestId('email-input')).toBeInTheDocument();
    expect(screen.getByTestId('password-input')).toBeInTheDocument();
    expect(screen.getByTestId('login-submit')).toBeInTheDocument();
    expect(screen.getByTestId('forgot-password-link')).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    renderWithProviders(<LoginForm />);
    const user = userEvent.setup();

    await user.click(screen.getByTestId('login-submit'));

    await waitFor(() => {
      expect(screen.getByText(/email обязателен/i)).toBeInTheDocument();
      expect(screen.getByText(/пароль обязателен/i)).toBeInTheDocument();
    });
  });

  // Тест для invalid email перенесён в E2E тесты (Playwright)
  // так как асинхронная валидация Zod + React Hook Form нестабильна в unit тестах

  it('has working forgot password link', () => {
    renderWithProviders(<LoginForm />);

    const link = screen.getByTestId('forgot-password-link');
    expect(link).toHaveAttribute('href', '/forgot-password');
  });

  it('submit button is enabled initially', () => {
    renderWithProviders(<LoginForm />);

    const submitButton = screen.getByTestId('login-submit');
    expect(submitButton).toBeEnabled();
    expect(submitButton).toHaveTextContent('Войти');
  });
});
