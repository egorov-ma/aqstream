import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { OrganizationSwitcher } from './organization-switcher';
import type { Organization } from '@/lib/api/types';

// Мок данные
const mockOrganizations: Organization[] = [
  { id: 'org-1', name: 'Организация 1', slug: 'org-1', ownerId: 'user-1', createdAt: '2024-01-01' },
  { id: 'org-2', name: 'Организация 2', slug: 'org-2', ownerId: 'user-1', createdAt: '2024-01-01' },
];

const singleOrganization: Organization[] = [
  { id: 'org-1', name: 'Единственная организация', slug: 'org-1', ownerId: 'user-1', createdAt: '2024-01-01' },
];

// Мок состояния
const mockSetCurrentOrganization = vi.fn();
let mockCurrentOrganization: Organization | null = null;

// Мок мутации
const mockMutate = vi.fn();
let mockIsPending = false;
let mockIsError = false;

// Мок данных организаций
let mockData: Organization[] | undefined = mockOrganizations;
let mockIsLoading = false;

// Мокаем hooks
vi.mock('@/lib/hooks/use-organizations', () => ({
  useOrganizations: () => ({
    data: mockData,
    isLoading: mockIsLoading,
  }),
  useSwitchOrganization: () => ({
    mutate: mockMutate,
    isPending: mockIsPending,
    isError: mockIsError,
  }),
}));

vi.mock('@/lib/store/organization-store', () => ({
  useOrganizationStore: () => ({
    currentOrganization: mockCurrentOrganization,
    setCurrentOrganization: mockSetCurrentOrganization,
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

describe('OrganizationSwitcher', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockData = mockOrganizations;
    mockIsLoading = false;
    mockCurrentOrganization = mockOrganizations[0];
    mockIsPending = false;
    mockIsError = false;
  });

  it('renders loading skeleton when loading', () => {
    mockIsLoading = true;
    renderWithProviders(<OrganizationSwitcher />);

    // Skeleton должен быть виден при загрузке
    const skeleton = document.querySelector('.animate-pulse');
    expect(skeleton).toBeInTheDocument();
  });

  it('renders single organization name without dropdown', () => {
    mockData = singleOrganization;
    mockCurrentOrganization = singleOrganization[0];
    renderWithProviders(<OrganizationSwitcher />);

    // Должно показать название организации
    expect(screen.getByText('Единственная организация')).toBeInTheDocument();
    // Не должно быть кнопки dropdown
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('renders dropdown button when multiple organizations', () => {
    renderWithProviders(<OrganizationSwitcher />);

    // Должна быть кнопка с названием текущей организации
    const button = screen.getByRole('button');
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent('Организация 1');
  });

  it('shows organization list in dropdown', async () => {
    renderWithProviders(<OrganizationSwitcher />);
    const user = userEvent.setup();

    // Открываем dropdown
    await user.click(screen.getByRole('button'));

    // Должны быть обе организации (используем getAllByText так как текст может быть в нескольких местах)
    expect(screen.getAllByText('Организация 1').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Организация 2').length).toBeGreaterThan(0);
  });

  it('shows check mark for current organization', async () => {
    renderWithProviders(<OrganizationSwitcher />);
    const user = userEvent.setup();

    // Открываем dropdown
    await user.click(screen.getByRole('button'));

    // Проверяем что текущая организация отмечена
    const menuItems = screen.getAllByRole('menuitem');
    expect(menuItems[0]).toContainElement(document.querySelector('.lucide-check'));
  });

  it('calls switchOrganization when clicking different organization', async () => {
    renderWithProviders(<OrganizationSwitcher />);
    const user = userEvent.setup();

    // Открываем dropdown
    await user.click(screen.getByRole('button'));

    // Кликаем на вторую организацию
    await user.click(screen.getByText('Организация 2'));

    expect(mockMutate).toHaveBeenCalledWith('org-2');
  });

  it('does not call switchOrganization when clicking current organization', async () => {
    renderWithProviders(<OrganizationSwitcher />);
    const user = userEvent.setup();

    // Открываем dropdown
    await user.click(screen.getByRole('button'));

    // Кликаем на текущую организацию (выбираем menu item)
    const menuItems = screen.getAllByRole('menuitem');
    await user.click(menuItems[0]); // Первый элемент - текущая организация

    expect(mockMutate).not.toHaveBeenCalled();
  });

  it('shows loading state when switching', () => {
    mockIsPending = true;
    renderWithProviders(<OrganizationSwitcher />);

    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    // Проверяем что есть анимация загрузки
    expect(document.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('shows error state when switch fails', () => {
    mockIsError = true;
    renderWithProviders(<OrganizationSwitcher />);

    const button = screen.getByRole('button');
    expect(button).toHaveClass('border-destructive');
  });

  it('returns null when no organizations and no current', () => {
    mockData = [];
    mockCurrentOrganization = null;
    const { container } = renderWithProviders(<OrganizationSwitcher />);

    expect(container.firstChild).toBeNull();
  });

  it('sets first organization as default when none selected', () => {
    mockCurrentOrganization = null;
    renderWithProviders(<OrganizationSwitcher />);

    // useEffect должен вызвать setCurrentOrganization с первой организацией
    expect(mockSetCurrentOrganization).toHaveBeenCalledWith(mockOrganizations[0]);
  });
});
