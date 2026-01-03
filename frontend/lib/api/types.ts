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
  hasNext?: boolean;
  hasPrevious?: boolean;
}

// Auth

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string | null; // null когда передаётся через httpOnly cookie
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

// Telegram Bot Auth (авторизация через бота)
export type TelegramAuthTokenStatus = 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'USED';

export interface TelegramAuthInitResponse {
  token: string;
  deeplink: string;
  expiresAt: string;
}

export interface TelegramAuthStatusResponse {
  status: TelegramAuthTokenStatus;
  accessToken?: string;
  user?: User;
}

// User

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string | null;
  avatarUrl: string | null;
  telegramId: string | null;
  emailVerified: boolean;
  isAdmin: boolean;
  createdAt: string;
}

// Profile

export interface UpdateProfileRequest {
  firstName: string;
  lastName?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// Organization

export interface Organization {
  id: string;
  name: string;
  slug: string;
  ownerId: string;
  createdAt: string;
}

// Organization Role & Membership

export type OrganizationRole = 'OWNER' | 'MODERATOR';

export interface OrganizationMember {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  userAvatarUrl: string | null;
  role: OrganizationRole;
  invitedById: string | null;
  invitedByName: string | null;
  joinedAt: string;
  createdAt: string;
}

// Organization Request

export type OrganizationRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface OrganizationRequest {
  id: string;
  userId: string;
  userName: string;
  name: string;
  slug: string;
  description?: string;
  status: OrganizationRequestStatus;
  reviewedById?: string;
  reviewComment?: string;
  reviewedAt?: string;
  createdAt: string;
}

export interface CreateOrganizationRequestRequest {
  name: string;
  slug: string;
  description?: string;
}

export interface RejectOrganizationRequestRequest {
  comment: string;
}

// Group

export interface Group {
  id: string;
  organizationId: string;
  organizationName: string;
  name: string;
  description: string | null;
  inviteCode: string;
  createdById: string;
  createdByName: string;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface JoinGroupResponse {
  groupId: string;
  groupName: string;
  organizationId: string;
  organizationName: string;
  joinedAt: string;
}

// Event

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';
export type LocationType = 'ONLINE' | 'OFFLINE' | 'HYBRID';
export type ParticipantsVisibility = 'CLOSED' | 'OPEN';

// Registration Form Configuration
export type CustomFieldType = 'text' | 'email' | 'tel' | 'select';

export interface CustomFieldConfig {
  name: string;
  label: string;
  type: CustomFieldType;
  required: boolean;
  options?: string[];
}

export interface RegistrationFormConfig {
  customFields?: CustomFieldConfig[];
}

export interface Event {
  id: string;
  tenantId: string;
  organizerName?: string; // Название организации (для публичной страницы)
  title: string;
  slug: string;
  description?: string;
  status: EventStatus;
  startsAt: string;
  endsAt?: string;
  timezone: string;
  locationType: LocationType;
  locationAddress?: string;
  onlineUrl?: string;
  maxCapacity?: number;
  registrationOpensAt?: string;
  registrationClosesAt?: string;
  isPublic: boolean;
  participantsVisibility: ParticipantsVisibility;
  groupId?: string;
  coverImageUrl?: string;
  registrationFormConfig?: RegistrationFormConfig; // Конфигурация формы регистрации
  cancelReason?: string;
  cancelledAt?: string;
  // Recurring events
  recurrenceRule?: RecurrenceRule;
  parentEventId?: string;
  instanceDate?: string;
  createdAt: string;
  updatedAt: string;
  // Обратная совместимость: location используется в старом коде
  location?: string;
}

/**
 * Публичное событие для карточки на главной странице.
 * Содержит только поля, необходимые для отображения карточки.
 */
export interface PublicEventSummary {
  id: string;
  title: string;
  slug: string;
  description: string | null;
  startsAt: string;
  timezone: string;
  locationType: LocationType;
  locationAddress?: string;
  coverImageUrl?: string;
  organizerName?: string;
}

export interface CreateRecurrenceRuleRequest {
  frequency: RecurrenceFrequency;
  interval?: number;
  endsAt?: string;
  occurrenceCount?: number;
  byDay?: string;
  byMonthDay?: number;
  excludedDates?: string[];
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  startsAt: string;
  endsAt?: string;
  timezone?: string;
  locationType?: LocationType;
  locationAddress?: string;
  onlineUrl?: string;
  maxCapacity?: number;
  registrationOpensAt?: string;
  registrationClosesAt?: string;
  isPublic?: boolean;
  participantsVisibility?: ParticipantsVisibility;
  groupId?: string;
  coverImageUrl?: string;
  recurrenceRule?: CreateRecurrenceRuleRequest;
  /**
   * ID организации для создания события.
   * Используется только админами для выбора организации.
   * Обычные пользователи используют свою текущую организацию из JWT.
   */
  organizationId?: string;
  // Обратная совместимость
  location?: string;
}

export interface UpdateEventRequest extends Partial<CreateEventRequest> {}

// Recurrence (Повторяющиеся события)

export type RecurrenceFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface RecurrenceRule {
  id?: string;
  frequency: RecurrenceFrequency;
  interval: number;
  endsAt?: string;
  occurrenceCount?: number;
  byDay?: string; // "MO,WE,FR" для WEEKLY
  byMonthDay?: number; // 1-31 для MONTHLY
  excludedDates?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface RecurringEventInfo {
  recurrenceRuleId?: string;
  parentEventId?: string;
  instanceDate?: string;
  isRecurring: boolean;
  isInstance: boolean;
}

// Ticket Type

export interface TicketType {
  id: string;
  eventId: string;
  name: string;
  description?: string;
  priceCents: number;
  currency: string;
  quantity?: number;
  soldCount: number;
  reservedCount: number;
  available?: number;
  salesStart?: string;
  salesEnd?: string;
  sortOrder: number;
  isActive: boolean;
  isSoldOut: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTicketTypeRequest {
  name: string;
  description?: string;
  quantity?: number;
  salesStart?: string;
  salesEnd?: string;
  sortOrder?: number;
}

export interface UpdateTicketTypeRequest extends Partial<CreateTicketTypeRequest> {
  isActive?: boolean;
}

// Registration

export type RegistrationStatus = 'CONFIRMED' | 'CANCELLED' | 'RESERVED' | 'PENDING' | 'EXPIRED' | 'CHECKED_IN';

export interface Registration {
  id: string;
  eventId: string;
  eventTitle: string;
  eventSlug: string;
  eventStartsAt: string;
  ticketTypeId: string;
  ticketTypeName: string;
  userId: string;
  status: RegistrationStatus;
  confirmationCode: string;
  firstName: string;
  lastName?: string;
  email: string;
  customFields?: Record<string, string>;
  cancelledAt?: string;
  cancellationReason?: string;
  createdAt: string;
}

// Event Audit Log

export type EventAuditAction =
  | 'CREATED'
  | 'UPDATED'
  | 'PUBLISHED'
  | 'UNPUBLISHED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'DELETED';

export interface FieldChange {
  from: string | null;
  to: string | null;
}

export interface EventAuditLog {
  id: string;
  eventId: string;
  action: EventAuditAction;
  actorId: string | null;
  actorEmail: string | null;
  changedFields: Record<string, FieldChange> | null;
  description: string | null;
  createdAt: string;
}

// Media

export interface UploadResponse {
  url: string;
  filename: string;
  contentType: string;
  size: number;
}

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
  refreshToken?: string | null; // null когда передаётся через httpOnly cookie
}
