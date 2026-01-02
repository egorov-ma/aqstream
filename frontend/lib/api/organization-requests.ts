import { apiClient } from './client';
import type {
  OrganizationRequest,
  OrganizationRequestStatus,
  CreateOrganizationRequestRequest,
  RejectOrganizationRequestRequest,
  PageResponse,
} from './types';

export const organizationRequestsApi = {
  // === User endpoints ===

  /**
   * Подать заявку на создание организации
   */
  create: async (data: CreateOrganizationRequestRequest): Promise<OrganizationRequest> => {
    const response = await apiClient.post<OrganizationRequest>(
      '/api/v1/organization-requests',
      data
    );
    return response.data;
  },

  /**
   * Получить свои заявки
   */
  getMyRequests: async (): Promise<OrganizationRequest[]> => {
    const response = await apiClient.get<OrganizationRequest[]>(
      '/api/v1/organization-requests/my'
    );
    return response.data;
  },

  /**
   * Получить заявку по ID
   */
  getById: async (id: string): Promise<OrganizationRequest> => {
    const response = await apiClient.get<OrganizationRequest>(
      `/api/v1/organization-requests/${id}`
    );
    return response.data;
  },

  // === Admin endpoints ===

  /**
   * Получить список всех заявок (только для админа)
   */
  listAll: async (
    page = 0,
    size = 20,
    status?: OrganizationRequestStatus
  ): Promise<PageResponse<OrganizationRequest>> => {
    const response = await apiClient.get<PageResponse<OrganizationRequest>>(
      '/api/v1/organization-requests',
      {
        params: { page, size, status },
      }
    );
    return response.data;
  },

  /**
   * Получить список pending заявок (только для админа)
   */
  listPending: async (page = 0, size = 20): Promise<PageResponse<OrganizationRequest>> => {
    const response = await apiClient.get<PageResponse<OrganizationRequest>>(
      '/api/v1/organization-requests/pending',
      {
        params: { page, size },
      }
    );
    return response.data;
  },

  /**
   * Одобрить заявку (только для админа)
   */
  approve: async (id: string): Promise<OrganizationRequest> => {
    const response = await apiClient.post<OrganizationRequest>(
      `/api/v1/organization-requests/${id}/approve`
    );
    return response.data;
  },

  /**
   * Отклонить заявку (только для админа)
   */
  reject: async (
    id: string,
    data: RejectOrganizationRequestRequest
  ): Promise<OrganizationRequest> => {
    const response = await apiClient.post<OrganizationRequest>(
      `/api/v1/organization-requests/${id}/reject`,
      data
    );
    return response.data;
  },
};
