// Общие типы API

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, string>;
}

export interface PageResponse<T> {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// Auth

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

// User

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  createdAt: string;
}

// Organization

export interface Organization {
  id: string;
  name: string;
  slug: string;
  ownerId: string;
  createdAt: string;
}

// Event

export interface Event {
  id: string;
  title: string;
  description?: string;
  slug: string;
  status: EventStatus;
  startsAt: string;
  endsAt?: string;
  timezone: string;
  location?: string;
  createdAt: string;
  updatedAt: string;
}

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';

export interface CreateEventRequest {
  title: string;
  description?: string;
  startsAt: string;
  endsAt?: string;
  timezone: string;
  location?: string;
}

export interface UpdateEventRequest extends Partial<CreateEventRequest> {}
