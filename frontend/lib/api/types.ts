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

// Alias для консистентности с backend
export type AuthResponse = LoginResponse;

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface ResendVerificationRequest {
  email: string;
}

// Telegram Login Widget data
export interface TelegramAuthRequest {
  id: number;
  first_name: string;
  last_name?: string;
  username?: string;
  photo_url?: string;
  auth_date: number;
  hash: string;
}

// User

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string | null;
  avatarUrl: string | null;
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

// Dashboard Stats

export interface DashboardStats {
  activeEventsCount: number;
  totalRegistrations: number;
  checkedInCount: number;
  attendanceRate: number;
  upcomingEvents: Event[];
}

// User Notifications

export type UserNotificationType =
  | 'NEW_REGISTRATION'
  | 'EVENT_UPDATE'
  | 'EVENT_CANCELLED'
  | 'EVENT_REMINDER'
  | 'SYSTEM';

export interface UserNotification {
  id: string;
  type: UserNotificationType;
  title: string;
  message: string;
  isRead: boolean;
  linkedEntity: {
    entityType: string;
    entityId: string;
  } | null;
  createdAt: string;
}

export interface UnreadCountResponse {
  count: number;
}

// Organization Switch

export interface SwitchOrganizationResponse {
  accessToken: string;
  refreshToken: string;
}
