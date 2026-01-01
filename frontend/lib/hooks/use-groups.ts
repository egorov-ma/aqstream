import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { groupsApi } from '@/lib/api/groups';
import { useAuthStore } from '@/lib/store/auth-store';

/**
 * Получить список групп пользователя.
 */
export function useMyGroups() {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['groups', 'my'],
    queryFn: () => groupsApi.getMyGroups(),
    enabled: isAuthenticated,
  });
}

/**
 * Получить группу по ID.
 */
export function useGroup(id: string) {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['groups', id],
    queryFn: () => groupsApi.getById(id),
    enabled: isAuthenticated && !!id,
  });
}

/**
 * Присоединиться к группе по инвайт-коду.
 */
export function useJoinGroup() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: groupsApi.join,
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      toast.success(`Вы присоединились к группе "${response.groupName}"`);
    },
    onError: () => {
      toast.error('Ошибка при присоединении к группе');
    },
  });
}

/**
 * Выйти из группы.
 */
export function useLeaveGroup() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: groupsApi.leave,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      toast.success('Вы вышли из группы');
    },
    onError: () => {
      toast.error('Ошибка при выходе из группы');
    },
  });
}
