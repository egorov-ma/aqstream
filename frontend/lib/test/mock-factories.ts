/**
 * Type-safe фабрики для создания mock объектов в тестах.
 *
 * Использование:
 * ```typescript
 * import { createMockUser, createMockEvent } from '@/lib/test/mock-factories';
 *
 * const user = createMockUser({ email: 'test@example.com' });
 * const event = createMockEvent({ title: 'Test Event' });
 * ```
 */

import type {
  User,
  Event,
  Group,
  TicketType,
  Registration,
  PageResponse,
  UploadResponse,
  LoginResponse,
  UserNotification,
  JoinGroupResponse,
} from '@/lib/api/types';
import type { CheckInInfo, CheckInResult } from '@/lib/api/check-in';
import type { NotificationPreferencesDto } from '@/lib/api/notifications';
import type { TelegramLinkTokenResponse } from '@/lib/api/profile';

// User

export function createMockUser(overrides?: Partial<User>): User {
  return {
    id: 'user-1',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    avatarUrl: null,
    telegramId: null,
    emailVerified: true,
    isAdmin: false,
    createdAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

export function createMockLoginResponse(overrides?: Partial<LoginResponse>): LoginResponse {
  return {
    accessToken: 'mock-access-token',
    user: createMockUser(),
    ...overrides,
  };
}

// Event

export function createMockEvent(overrides?: Partial<Event>): Event {
  return {
    id: 'event-1',
    tenantId: 'tenant-1',
    title: 'Test Event',
    slug: 'test-event',
    description: 'Test event description',
    status: 'PUBLISHED',
    startsAt: '2025-06-15T10:00:00.000Z',
    endsAt: '2025-06-15T18:00:00.000Z',
    timezone: 'Europe/Moscow',
    locationType: 'OFFLINE',
    locationAddress: 'Moscow, Russia',
    onlineUrl: undefined,
    maxCapacity: 100,
    registrationOpensAt: undefined,
    registrationClosesAt: undefined,
    isPublic: true,
    participantsVisibility: 'CLOSED',
    groupId: undefined,
    coverImageUrl: undefined,
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

// Group

export function createMockGroup(overrides?: Partial<Group>): Group {
  return {
    id: 'group-1',
    organizationId: 'org-1',
    organizationName: 'Test Organization',
    name: 'Test Group',
    description: 'Test group description',
    inviteCode: 'ABC123',
    createdById: 'user-1',
    createdByName: 'Test User',
    memberCount: 10,
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

export function createMockJoinGroupResponse(
  overrides?: Partial<JoinGroupResponse>
): JoinGroupResponse {
  return {
    groupId: 'group-1',
    groupName: 'Test Group',
    organizationId: 'org-1',
    organizationName: 'Test Organization',
    joinedAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

// TicketType

export function createMockTicketType(overrides?: Partial<TicketType>): TicketType {
  return {
    id: 'tt-1',
    eventId: 'event-1',
    name: 'Standard',
    description: 'Standard ticket',
    priceCents: 0,
    currency: 'RUB',
    quantity: 100,
    soldCount: 0,
    reservedCount: 0,
    available: 100,
    salesStart: undefined,
    salesEnd: undefined,
    sortOrder: 0,
    isActive: true,
    isSoldOut: false,
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

// Registration

export function createMockRegistration(overrides?: Partial<Registration>): Registration {
  return {
    id: 'reg-1',
    eventId: 'event-1',
    eventTitle: 'Test Event',
    eventSlug: 'test-event',
    eventStartsAt: '2025-06-15T10:00:00.000Z',
    ticketTypeId: 'tt-1',
    ticketTypeName: 'Standard',
    userId: 'user-1',
    status: 'CONFIRMED',
    confirmationCode: 'ABC123',
    firstName: 'Test',
    lastName: 'User',
    email: 'test@example.com',
    customFields: undefined,
    cancelledAt: undefined,
    cancellationReason: undefined,
    createdAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

// Check-in

export function createMockCheckInInfo(overrides?: Partial<CheckInInfo>): CheckInInfo {
  return {
    registrationId: 'reg-1',
    confirmationCode: 'ABC123',
    eventId: 'event-1',
    eventTitle: 'Test Event',
    eventStartsAt: '2025-06-15T10:00:00.000Z',
    ticketTypeName: 'Standard',
    firstName: 'Test',
    lastName: 'User',
    email: 'test@example.com',
    status: 'CONFIRMED',
    isCheckedIn: false,
    checkedInAt: null,
    ...overrides,
  };
}

export function createMockCheckInResult(overrides?: Partial<CheckInResult>): CheckInResult {
  return {
    registrationId: 'reg-1',
    confirmationCode: 'ABC123',
    eventTitle: 'Test Event',
    ticketTypeName: 'Standard',
    firstName: 'Test',
    lastName: 'User',
    checkedInAt: '2025-06-15T09:00:00.000Z',
    message: 'Check-in успешен',
    ...overrides,
  };
}

// Media

export function createMockUploadResponse(overrides?: Partial<UploadResponse>): UploadResponse {
  return {
    url: 'https://example.com/file.jpg',
    filename: 'file.jpg',
    contentType: 'image/jpeg',
    size: 1024,
    ...overrides,
  };
}

// Notifications

export function createMockUserNotification(
  overrides?: Partial<UserNotification>
): UserNotification {
  return {
    id: 'notif-1',
    type: 'SYSTEM',
    title: 'Test Notification',
    message: 'This is a test notification',
    isRead: false,
    linkedEntity: null,
    createdAt: '2025-01-01T00:00:00.000Z',
    ...overrides,
  };
}

export function createMockNotificationPreferences(
  overrides?: Partial<NotificationPreferencesDto>
): NotificationPreferencesDto {
  return {
    settings: {
      eventReminders: true,
      registrationUpdates: true,
      organizationNews: true,
      ...overrides?.settings,
    },
  };
}

// Profile

export function createMockTelegramLinkToken(
  overrides?: Partial<TelegramLinkTokenResponse>
): TelegramLinkTokenResponse {
  return {
    token: 'mock-token',
    botLink: 'https://t.me/testbot?start=mock-token',
    ...overrides,
  };
}

// PageResponse

export function createMockPageResponse<T>(
  data: T[],
  overrides?: Partial<Omit<PageResponse<T>, 'data'>>
): PageResponse<T> {
  return {
    data,
    page: 0,
    size: 20,
    totalElements: data.length,
    totalPages: Math.ceil(data.length / 20) || 1,
    hasNext: false,
    hasPrevious: false,
    ...overrides,
  };
}
