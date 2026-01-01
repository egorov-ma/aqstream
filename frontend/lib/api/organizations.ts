import { apiClient } from './client';
import type { Organization, OrganizationMember, SwitchOrganizationResponse } from './types';

export const organizationsApi = {
  /**
   * Получить список организаций пользователя.
   */
  list: async (): Promise<Organization[]> => {
    const response = await apiClient.get<Organization[]>('/api/v1/organizations');
    return response.data;
  },

  /**
   * Получить организацию по ID.
   */
  getById: async (id: string): Promise<Organization> => {
    const response = await apiClient.get<Organization>(`/api/v1/organizations/${id}`);
    return response.data;
  },

  /**
   * Получить своё членство в организации (включая роль).
   */
  getMyMembership: async (organizationId: string): Promise<OrganizationMember> => {
    const response = await apiClient.get<OrganizationMember>(
      `/api/v1/organizations/${organizationId}/members/me`
    );
    return response.data;
  },

  /**
   * Переключиться на другую организацию.
   * Возвращает новые токены с другим tenantId.
   */
  switch: async (organizationId: string): Promise<SwitchOrganizationResponse> => {
    const response = await apiClient.post<SwitchOrganizationResponse>(
      `/api/v1/organizations/${organizationId}/switch`
    );
    return response.data;
  },
};
