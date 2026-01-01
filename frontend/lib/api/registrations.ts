import { apiClient } from './client';
import type { PageResponse, Registration, RegistrationStatus } from './types';

export interface RegistrationFilters {
  status?: RegistrationStatus;
  ticketTypeId?: string;
  query?: string;
  page?: number;
  size?: number;
}

export interface CreateRegistrationRequest {
  ticketTypeId: string;
  firstName: string;
  lastName?: string;
  email: string;
  customFields?: Record<string, string>;
}

export const registrationsApi = {
  /**
   * Получить список регистраций события (для организаторов)
   */
  listByEvent: async (
    eventId: string,
    filters?: RegistrationFilters
  ): Promise<PageResponse<Registration>> => {
    const response = await apiClient.get(`/api/v1/events/${eventId}/registrations`, {
      params: filters,
    });
    return response.data;
  },

  /**
   * Получить регистрацию по ID
   */
  getById: async (registrationId: string): Promise<Registration> => {
    const response = await apiClient.get(`/api/v1/registrations/${registrationId}`);
    return response.data;
  },

  /**
   * Получить мои регистрации
   */
  getMy: async (params?: {
    page?: number;
    size?: number;
  }): Promise<PageResponse<Registration>> => {
    const response = await apiClient.get('/api/v1/registrations/my', { params });
    return response.data;
  },

  /**
   * Создать регистрацию на событие
   */
  create: async (
    eventId: string,
    data: CreateRegistrationRequest
  ): Promise<Registration> => {
    const response = await apiClient.post(`/api/v1/events/${eventId}/registrations`, data);
    return response.data;
  },

  /**
   * Отменить регистрацию
   */
  cancel: async (registrationId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/registrations/${registrationId}`);
  },

  /**
   * Повторно отправить билет в Telegram
   */
  resendTicket: async (registrationId: string): Promise<void> => {
    await apiClient.post(`/api/v1/registrations/${registrationId}/resend-ticket`);
  },
};
