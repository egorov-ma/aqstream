import { apiClient } from './client';
import type { Event, CreateEventRequest, UpdateEventRequest, PageResponse } from './types';

export interface EventFilters {
  status?: string;
  search?: string;
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
    const response = await apiClient.get<Event>(`/api/v1/events/public/${slug}`);
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
};
