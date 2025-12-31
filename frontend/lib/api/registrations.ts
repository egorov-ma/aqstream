import { apiClient } from './client';
import type { PageResponse, Registration, RegistrationStatus } from './types';

export interface RegistrationFilters {
  status?: RegistrationStatus;
  ticketTypeId?: string;
  query?: string;
  page?: number;
  size?: number;
}

export const registrationsApi = {
  /**
   * Получить список регистраций события (для организаторов)
   */
  listByEvent: async (
    eventId: string,
    filters?: RegistrationFilters
  ): Promise<PageResponse<Registration>> => {
    const response = await apiClient.get(`/events/${eventId}/registrations`, {
      params: filters,
    });
    return response.data;
  },

  /**
   * Получить регистрацию по ID
   */
  getById: async (registrationId: string): Promise<Registration> => {
    const response = await apiClient.get(`/registrations/${registrationId}`);
    return response.data;
  },

  /**
   * Получить мои регистрации
   */
  getMy: async (params?: {
    page?: number;
    size?: number;
  }): Promise<PageResponse<Registration>> => {
    const response = await apiClient.get('/registrations/my', { params });
    return response.data;
  },
};
