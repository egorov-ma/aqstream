import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { organizationsApi } from '@/lib/api/organizations';
import { useAuthStore } from '@/lib/store/auth-store';
import { useOrganizationStore } from '@/lib/store/organization-store';

/**
 * Получить список организаций пользователя.
 */
export function useOrganizations() {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['organizations'],
    queryFn: () => organizationsApi.list(),
    enabled: isAuthenticated,
  });
}

/**
 * Получить организацию по ID.
 */
export function useOrganization(id: string) {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['organizations', id],
    queryFn: () => organizationsApi.getById(id),
    enabled: isAuthenticated && !!id,
  });
}

/**
 * Получить членство текущего пользователя в организации (включая роль).
 */
export function useMyMembership(organizationId: string | undefined) {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['organizations', organizationId, 'membership', 'me'],
    queryFn: () => organizationsApi.getMyMembership(organizationId!),
    enabled: isAuthenticated && !!organizationId,
  });
}

/**
 * Получить все организации (только для админов).
 * Используется для выбора организации при создании события.
 */
export function useAllOrganizations(enabled = true) {
  const { isAuthenticated, user } = useAuthStore();
  const isAdmin = user?.isAdmin ?? false;

  return useQuery({
    queryKey: ['organizations', 'all'],
    queryFn: async () => {
      const response = await organizationsApi.getAllOrganizations(0, 100);
      return response.data;
    },
    enabled: isAuthenticated && isAdmin && enabled,
  });
}

/**
 * Переключиться на другую организацию.
 * Обновляет токены, загружает роль и перезагружает данные.
 */
export function useSwitchOrganization() {
  const queryClient = useQueryClient();
  const { setAccessToken } = useAuthStore();
  const { setOrganizationWithRole } = useOrganizationStore();

  return useMutation({
    mutationFn: async (organizationId: string) => {
      // 1. Переключаем организацию (получаем новый токен)
      const switchResponse = await organizationsApi.switch(organizationId);
      // 2. Загружаем членство (включая роль)
      const membership = await organizationsApi.getMyMembership(organizationId);
      return { switchResponse, membership, organizationId };
    },
    onSuccess: ({ switchResponse, membership, organizationId }) => {
      // Обновляем access token
      setAccessToken(switchResponse.accessToken);

      // Обновляем текущую организацию и роль в store
      const organizations = queryClient.getQueryData<
        Awaited<ReturnType<typeof organizationsApi.list>>
      >(['organizations']);

      const org = organizations?.find((o) => o.id === organizationId);
      if (org) {
        setOrganizationWithRole(org, membership.role);
      }

      // Инвалидируем все tenant-specific запросы
      queryClient.invalidateQueries({
        predicate: (query) => {
          const key = query.queryKey[0];
          const tenantIndependentKeys = ['organizations', 'user'];
          return !tenantIndependentKeys.includes(key as string);
        },
      });

      toast.success('Организация переключена');
    },
    onError: () => {
      toast.error('Ошибка переключения организации');
    },
  });
}
