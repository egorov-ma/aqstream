import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { organizationRequestsApi } from '@/lib/api/organization-requests';
import type {
  CreateOrganizationRequestRequest,
  RejectOrganizationRequestRequest,
  OrganizationRequestStatus,
} from '@/lib/api/types';
import { toast } from 'sonner';

// === User hooks ===

/**
 * Получить свои заявки на создание организации
 */
export function useMyOrganizationRequests() {
  return useQuery({
    queryKey: ['organization-requests', 'my'],
    queryFn: () => organizationRequestsApi.getMyRequests(),
  });
}

/**
 * Подать заявку на создание организации
 */
export function useCreateOrganizationRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateOrganizationRequestRequest) =>
      organizationRequestsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization-requests'] });
      toast.success('Заявка отправлена');
    },
    onError: () => {
      toast.error('Ошибка при отправке заявки');
    },
  });
}

// === Admin hooks ===

/**
 * Получить список всех заявок (для админа)
 */
export function useOrganizationRequests(
  page = 0,
  size = 20,
  status?: OrganizationRequestStatus
) {
  return useQuery({
    queryKey: ['organization-requests', 'all', page, size, status],
    queryFn: () => organizationRequestsApi.listAll(page, size, status),
  });
}

/**
 * Получить список pending заявок (для админа)
 */
export function usePendingOrganizationRequests(page = 0, size = 20) {
  return useQuery({
    queryKey: ['organization-requests', 'pending', page, size],
    queryFn: () => organizationRequestsApi.listPending(page, size),
  });
}

/**
 * Одобрить заявку (для админа)
 */
export function useApproveOrganizationRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => organizationRequestsApi.approve(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization-requests'] });
      toast.success('Заявка одобрена');
    },
    onError: () => {
      toast.error('Ошибка при одобрении заявки');
    },
  });
}

/**
 * Отклонить заявку (для админа)
 */
export function useRejectOrganizationRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectOrganizationRequestRequest }) =>
      organizationRequestsApi.reject(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization-requests'] });
      toast.success('Заявка отклонена');
    },
    onError: () => {
      toast.error('Ошибка при отклонении заявки');
    },
  });
}
