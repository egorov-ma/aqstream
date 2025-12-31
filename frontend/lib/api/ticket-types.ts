import { apiClient } from './client';
import type {
  TicketType,
  CreateTicketTypeRequest,
  UpdateTicketTypeRequest,
} from './types';

// API клиент для работы с типами билетов

export const ticketTypesApi = {
  /**
   * Получить список типов билетов события
   */
  list: async (eventId: string): Promise<TicketType[]> => {
    const response = await apiClient.get<TicketType[]>(
      `/api/v1/events/${eventId}/ticket-types`
    );
    return response.data;
  },

  /**
   * Получить тип билета по ID
   */
  getById: async (eventId: string, ticketTypeId: string): Promise<TicketType> => {
    const response = await apiClient.get<TicketType>(
      `/api/v1/events/${eventId}/ticket-types/${ticketTypeId}`
    );
    return response.data;
  },

  /**
   * Создать новый тип билета
   */
  create: async (
    eventId: string,
    data: CreateTicketTypeRequest
  ): Promise<TicketType> => {
    const response = await apiClient.post<TicketType>(
      `/api/v1/events/${eventId}/ticket-types`,
      data
    );
    return response.data;
  },

  /**
   * Обновить тип билета
   */
  update: async (
    eventId: string,
    ticketTypeId: string,
    data: UpdateTicketTypeRequest
  ): Promise<TicketType> => {
    const response = await apiClient.put<TicketType>(
      `/api/v1/events/${eventId}/ticket-types/${ticketTypeId}`,
      data
    );
    return response.data;
  },

  /**
   * Удалить тип билета (только если нет регистраций)
   */
  delete: async (eventId: string, ticketTypeId: string): Promise<void> => {
    await apiClient.delete(
      `/api/v1/events/${eventId}/ticket-types/${ticketTypeId}`
    );
  },

  /**
   * Деактивировать тип билета (если есть регистрации)
   */
  deactivate: async (
    eventId: string,
    ticketTypeId: string
  ): Promise<TicketType> => {
    const response = await apiClient.post<TicketType>(
      `/api/v1/events/${eventId}/ticket-types/${ticketTypeId}/deactivate`
    );
    return response.data;
  },
};
