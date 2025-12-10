/**
 * Общие типы для приложения AqStream.
 * Типы для конкретных feature будут добавляться по мере реализации.
 */

/**
 * Базовый интерфейс для пагинированных ответов API.
 */
export interface PageResponse<T> {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Структура ошибки API.
 */
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, string>;
}

/**
 * Статус события.
 */
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';

/**
 * Базовый интерфейс события (placeholder).
 */
export interface Event {
  id: string;
  title: string;
  slug: string;
  description?: string;
  status: EventStatus;
  startsAt: string;
  endsAt?: string;
  createdAt: string;
  updatedAt: string;
}
