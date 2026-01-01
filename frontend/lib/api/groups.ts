import { apiClient } from './client';
import type { Group, JoinGroupResponse } from './types';

export const groupsApi = {
  /**
   * Получить список групп пользователя.
   */
  getMyGroups: async (): Promise<Group[]> => {
    const response = await apiClient.get<Group[]>('/api/v1/groups/my');
    return response.data;
  },

  /**
   * Получить группу по ID.
   */
  getById: async (id: string): Promise<Group> => {
    const response = await apiClient.get<Group>(`/api/v1/groups/${id}`);
    return response.data;
  },

  /**
   * Присоединиться к группе по инвайт-коду.
   */
  join: async (inviteCode: string): Promise<JoinGroupResponse> => {
    const response = await apiClient.post<JoinGroupResponse>(
      `/api/v1/groups/join/${inviteCode}`
    );
    return response.data;
  },

  /**
   * Выйти из группы.
   */
  leave: async (groupId: string): Promise<void> => {
    await apiClient.post(`/api/v1/groups/${groupId}/leave`);
  },
};
