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
 * Переключиться на другую организацию.
 * Обновляет токены и перезагружает данные.
 */
export function useSwitchOrganization() {
  const queryClient = useQueryClient();
  const { setTokens } = useAuthStore();
  const { setCurrentOrganization } = useOrganizationStore();

  return useMutation({
    mutationFn: organizationsApi.switch,
    onSuccess: (response, organizationId) => {
      // Обновляем токены с новым tenantId
      setTokens(response.accessToken, response.refreshToken);

      // Обновляем текущую организацию в store
      // (нужно получить организацию из кэша или сделать запрос)
      const organizations = queryClient.getQueryData<
        Awaited<ReturnType<typeof organizationsApi.list>>
      >(['organizations']);

      const org = organizations?.find((o) => o.id === organizationId);
      if (org) {
        setCurrentOrganization(org);
      }

      // Инвалидируем все tenant-specific запросы чтобы перезагрузить данные
      // Исключаем organizations — они не зависят от текущего tenant
      queryClient.invalidateQueries({
        predicate: (query) => {
          const key = query.queryKey[0];
          // Список ключей, которые НЕ зависят от tenant
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
