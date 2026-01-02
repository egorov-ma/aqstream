/**
 * Server-side API client for SSR.
 *
 * Используется на сервере (в Server Components и SSR функциях).
 * Обращается напрямую к Gateway по внутреннему адресу Docker network.
 */

import type { Event, TicketType, PageResponse, PublicEventSummary } from './types';

// URL для серверных запросов (Docker internal network)
const SERVER_API_URL = process.env.NEXT_PUBLIC_GATEWAY_URL || 'http://localhost:8080';

interface FetchOptions {
  headers?: Record<string, string>;
  cache?: RequestCache;
  next?: { revalidate?: number | false; tags?: string[] };
}

/**
 * Выполняет GET запрос к API на сервере.
 * Для SSR в Docker использует NEXT_PUBLIC_GATEWAY_URL (http://gateway:8080)
 */
export async function serverFetch<T>(
  path: string,
  options: FetchOptions = {}
): Promise<T> {
  const url = `${SERVER_API_URL}${path}`;

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    cache: options.cache,
    next: options.next,
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

/**
 * Server-side API для публичных событий.
 * Не требует аутентификации.
 */
export const serverEventsApi = {
  /**
   * Получить публичное событие по slug (SSR)
   */
  getBySlug: async (slug: string): Promise<Event> => {
    return serverFetch<Event>(`/api/v1/public/events/${slug}`, {
      next: { revalidate: 60 },
    });
  },

  /**
   * Получить типы билетов для события (SSR)
   */
  getPublicTicketTypes: async (slug: string): Promise<TicketType[]> => {
    return serverFetch<TicketType[]>(`/api/v1/public/events/${slug}/ticket-types`, {
      next: { revalidate: 60 },
    });
  },

  /**
   * Получить список предстоящих публичных событий (SSR)
   */
  listPublic: async (filters?: {
    page?: number;
    size?: number;
  }): Promise<PageResponse<PublicEventSummary>> => {
    const params = new URLSearchParams();
    params.set('page', String(filters?.page ?? 0));
    params.set('size', String(filters?.size ?? 12));

    return serverFetch<PageResponse<PublicEventSummary>>(
      `/api/v1/public/events?${params.toString()}`,
      { next: { revalidate: 60 } }
    );
  },
};
