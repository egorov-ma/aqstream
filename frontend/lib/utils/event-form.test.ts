import { describe, it, expect } from 'vitest';
import type { Event, EventStatus } from '@/lib/api/types';
import type { EventFormData } from '@/lib/validations/events';
import {
  isEventEditable,
  isEventPublishable,
  isEventCancellable,
  isEventCompletable,
  mapEventToFormData,
  mapFormDataToCreateRequest,
  mapFormDataToUpdateRequest,
  getStatusLabel,
  getStatusColor,
  getLocationTypeLabel,
  requiresAddress,
  requiresOnlineUrl,
  createEmptyTicketType,
  sortTicketTypes,
} from './event-form';

describe('isEventEditable', () => {
  it('returns true for DRAFT status', () => {
    expect(isEventEditable('DRAFT')).toBe(true);
  });

  it('returns true for PUBLISHED status', () => {
    expect(isEventEditable('PUBLISHED')).toBe(true);
  });

  it('returns false for CANCELLED status', () => {
    expect(isEventEditable('CANCELLED')).toBe(false);
  });

  it('returns false for COMPLETED status', () => {
    expect(isEventEditable('COMPLETED')).toBe(false);
  });
});

describe('isEventPublishable', () => {
  it('returns true for DRAFT status', () => {
    expect(isEventPublishable('DRAFT')).toBe(true);
  });

  it('returns false for PUBLISHED status', () => {
    expect(isEventPublishable('PUBLISHED')).toBe(false);
  });

  it('returns false for CANCELLED status', () => {
    expect(isEventPublishable('CANCELLED')).toBe(false);
  });
});

describe('isEventCancellable', () => {
  it('returns true for DRAFT status', () => {
    expect(isEventCancellable('DRAFT')).toBe(true);
  });

  it('returns true for PUBLISHED status', () => {
    expect(isEventCancellable('PUBLISHED')).toBe(true);
  });

  it('returns false for CANCELLED status', () => {
    expect(isEventCancellable('CANCELLED')).toBe(false);
  });

  it('returns false for COMPLETED status', () => {
    expect(isEventCancellable('COMPLETED')).toBe(false);
  });
});

describe('isEventCompletable', () => {
  it('returns true for PUBLISHED status', () => {
    expect(isEventCompletable('PUBLISHED')).toBe(true);
  });

  it('returns false for DRAFT status', () => {
    expect(isEventCompletable('DRAFT')).toBe(false);
  });

  it('returns false for CANCELLED status', () => {
    expect(isEventCompletable('CANCELLED')).toBe(false);
  });
});

describe('mapEventToFormData', () => {
  const baseEvent: Event = {
    id: '1',
    tenantId: 'tenant-1',
    title: 'Test Event',
    slug: 'test-event',
    description: 'Event description',
    startsAt: '2025-06-15T10:00:00.000Z',
    endsAt: '2025-06-15T18:00:00.000Z',
    timezone: 'Europe/Moscow',
    locationType: 'OFFLINE',
    locationAddress: 'Moscow, Russia',
    onlineUrl: undefined,
    maxCapacity: 100,
    registrationOpensAt: '2025-05-01T00:00:00.000Z',
    registrationClosesAt: '2025-06-14T23:59:59.000Z',
    isPublic: true,
    participantsVisibility: 'CLOSED',
    groupId: undefined,
    coverImageUrl: 'https://example.com/cover.jpg',
    status: 'PUBLISHED',
    organizerName: 'Test Org',
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-02T00:00:00.000Z',
  };

  it('maps all event fields to form data', () => {
    const formData = mapEventToFormData(baseEvent);

    expect(formData.title).toBe('Test Event');
    expect(formData.description).toBe('Event description');
    expect(formData.startsAt).toBe('2025-06-15T10:00:00.000Z');
    expect(formData.timezone).toBe('Europe/Moscow');
    expect(formData.locationType).toBe('OFFLINE');
    expect(formData.maxCapacity).toBe(100);
    expect(formData.isPublic).toBe(true);
  });

  it('handles undefined values correctly', () => {
    const eventWithNulls: Event = {
      ...baseEvent,
      description: undefined,
      endsAt: undefined,
      locationAddress: undefined,
      onlineUrl: undefined,
      maxCapacity: undefined,
      groupId: undefined,
      coverImageUrl: undefined,
    };

    const formData = mapEventToFormData(eventWithNulls);

    expect(formData.description).toBe('');
    expect(formData.endsAt).toBe('');
    expect(formData.locationAddress).toBe('');
    expect(formData.maxCapacity).toBeNull();
  });
});

describe('mapFormDataToCreateRequest', () => {
  const formData: EventFormData = {
    title: 'Test Event',
    description: 'Description',
    startsAt: '2025-06-15T10:00:00.000Z',
    endsAt: '2025-06-15T18:00:00.000Z',
    timezone: 'Europe/Moscow',
    locationType: 'OFFLINE',
    locationAddress: 'Moscow',
    onlineUrl: '',
    maxCapacity: 100,
    registrationOpensAt: '',
    registrationClosesAt: '',
    isPublic: true,
    participantsVisibility: 'CLOSED',
    groupId: '',
    coverImageUrl: '',
    ticketTypes: [],
  };

  it('maps form data to create request', () => {
    const request = mapFormDataToCreateRequest(formData);

    expect(request.title).toBe('Test Event');
    expect(request.description).toBe('Description');
    expect(request.startsAt).toBe('2025-06-15T10:00:00.000Z');
    expect(request.maxCapacity).toBe(100);
  });

  it('converts empty strings to undefined', () => {
    const request = mapFormDataToCreateRequest(formData);

    expect(request.onlineUrl).toBeUndefined();
    expect(request.groupId).toBeUndefined();
    expect(request.coverImageUrl).toBeUndefined();
  });
});

describe('mapFormDataToUpdateRequest', () => {
  const formData: EventFormData = {
    title: 'Updated Event',
    description: '',
    startsAt: '2025-06-15T10:00:00.000Z',
    endsAt: '',
    timezone: 'Europe/Moscow',
    locationType: 'ONLINE',
    locationAddress: '',
    onlineUrl: 'https://zoom.us/123',
    maxCapacity: undefined as unknown as number,
    registrationOpensAt: '',
    registrationClosesAt: '',
    isPublic: false,
    participantsVisibility: 'OPEN',
    groupId: '',
    coverImageUrl: '',
    ticketTypes: [],
  };

  it('maps form data to update request', () => {
    const request = mapFormDataToUpdateRequest(formData);

    expect(request.title).toBe('Updated Event');
    expect(request.locationType).toBe('ONLINE');
    expect(request.onlineUrl).toBe('https://zoom.us/123');
    expect(request.isPublic).toBe(false);
  });

  it('converts null maxCapacity to undefined', () => {
    const request = mapFormDataToUpdateRequest(formData);
    expect(request.maxCapacity).toBeUndefined();
  });
});

describe('getStatusLabel', () => {
  it('returns Russian label for DRAFT', () => {
    expect(getStatusLabel('DRAFT')).toBe('Черновик');
  });

  it('returns Russian label for PUBLISHED', () => {
    expect(getStatusLabel('PUBLISHED')).toBe('Опубликовано');
  });

  it('returns Russian label for CANCELLED', () => {
    expect(getStatusLabel('CANCELLED')).toBe('Отменено');
  });

  it('returns Russian label for COMPLETED', () => {
    expect(getStatusLabel('COMPLETED')).toBe('Завершено');
  });

  it('returns status as-is for unknown status', () => {
    expect(getStatusLabel('UNKNOWN' as EventStatus)).toBe('UNKNOWN');
  });
});

describe('getStatusColor', () => {
  it('returns secondary for DRAFT', () => {
    expect(getStatusColor('DRAFT')).toBe('secondary');
  });

  it('returns default for PUBLISHED', () => {
    expect(getStatusColor('PUBLISHED')).toBe('default');
  });

  it('returns destructive for CANCELLED', () => {
    expect(getStatusColor('CANCELLED')).toBe('destructive');
  });

  it('returns outline for COMPLETED', () => {
    expect(getStatusColor('COMPLETED')).toBe('outline');
  });
});

describe('getLocationTypeLabel', () => {
  it('returns Russian label for ONLINE', () => {
    expect(getLocationTypeLabel('ONLINE')).toBe('Онлайн');
  });

  it('returns Russian label for OFFLINE', () => {
    expect(getLocationTypeLabel('OFFLINE')).toBe('Офлайн');
  });

  it('returns Russian label for HYBRID', () => {
    expect(getLocationTypeLabel('HYBRID')).toBe('Гибрид');
  });
});

describe('requiresAddress', () => {
  it('returns true for OFFLINE', () => {
    expect(requiresAddress('OFFLINE')).toBe(true);
  });

  it('returns true for HYBRID', () => {
    expect(requiresAddress('HYBRID')).toBe(true);
  });

  it('returns false for ONLINE', () => {
    expect(requiresAddress('ONLINE')).toBe(false);
  });
});

describe('requiresOnlineUrl', () => {
  it('returns true for ONLINE', () => {
    expect(requiresOnlineUrl('ONLINE')).toBe(true);
  });

  it('returns true for HYBRID', () => {
    expect(requiresOnlineUrl('HYBRID')).toBe(true);
  });

  it('returns false for OFFLINE', () => {
    expect(requiresOnlineUrl('OFFLINE')).toBe(false);
  });
});

describe('createEmptyTicketType', () => {
  it('creates empty ticket type with default sortOrder', () => {
    const ticketType = createEmptyTicketType();

    expect(ticketType.name).toBe('');
    expect(ticketType.description).toBe('');
    expect(ticketType.quantity).toBeNull();
    expect(ticketType.salesStart).toBeNull();
    expect(ticketType.salesEnd).toBeNull();
    expect(ticketType.sortOrder).toBe(0);
  });

  it('creates empty ticket type with custom sortOrder', () => {
    const ticketType = createEmptyTicketType(5);
    expect(ticketType.sortOrder).toBe(5);
  });
});

describe('sortTicketTypes', () => {
  it('sorts ticket types by sortOrder', () => {
    const ticketTypes = [
      createEmptyTicketType(2),
      createEmptyTicketType(0),
      createEmptyTicketType(1),
    ];
    ticketTypes[0].name = 'Third';
    ticketTypes[1].name = 'First';
    ticketTypes[2].name = 'Second';

    const sorted = sortTicketTypes(ticketTypes);

    expect(sorted[0].name).toBe('First');
    expect(sorted[1].name).toBe('Second');
    expect(sorted[2].name).toBe('Third');
  });

  it('does not mutate original array', () => {
    const ticketTypes = [createEmptyTicketType(2), createEmptyTicketType(1)];
    const original = [...ticketTypes];

    sortTicketTypes(ticketTypes);

    expect(ticketTypes[0].sortOrder).toBe(original[0].sortOrder);
    expect(ticketTypes[1].sortOrder).toBe(original[1].sortOrder);
  });
});
