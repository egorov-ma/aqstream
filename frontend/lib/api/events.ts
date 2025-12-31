import { apiClient } from './client';
import type {
  Event,
  CreateEventRequest,
  UpdateEventRequest,
  PageResponse,
  EventStatus,
} from './types';

export interface EventFilters {
  status?: EventStatus;
  search?: string;
  groupId?: string;
  startsAfter?: string;
  startsBefore?: string;
  page?: number;
  size?: number;
}

export const eventsApi = {
  list: async (filters?: EventFilters): Promise<PageResponse<Event>> => {
    const response = await apiClient.get<PageResponse<Event>>('/api/v1/events', {
      params: filters,
    });
    return response.data;
  },

  getById: async (id: string): Promise<Event> => {
    const response = await apiClient.get<Event>(`/api/v1/events/${id}`);
    return response.data;
  },

  getBySlug: async (slug: string): Promise<Event> => {
    const response = await apiClient.get<Event>(`/api/v1/public/events/${slug}`);
    return response.data;
  },

  create: async (data: CreateEventRequest): Promise<Event> => {
    const response = await apiClient.post<Event>('/api/v1/events', data);
    return response.data;
  },

  update: async (id: string, data: UpdateEventRequest): Promise<Event> => {
    const response = await apiClient.put<Event>(`/api/v1/events/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/events/${id}`);
  },

  publish: async (id: string): Promise<Event> => {
    const response = await apiClient.post<Event>(`/api/v1/events/${id}/publish`);
    return response.data;
  },

  cancel: async (id: string): Promise<Event> => {
    const response = await apiClient.post<Event>(`/api/v1/events/${id}/cancel`);
    return response.data;
  },

  /**
   * Снять событие с публикации (PUBLISHED → DRAFT)
   */
  unpublish: async (id: string): Promise<Event> => {
    const response = await apiClient.post<Event>(`/api/v1/events/${id}/unpublish`);
    return response.data;
  },

  /**
   * Завершить событие (PUBLISHED → COMPLETED)
   */
  complete: async (id: string): Promise<Event> => {
    const response = await apiClient.post<Event>(`/api/v1/events/${id}/complete`);
    return response.data;
  },
};
