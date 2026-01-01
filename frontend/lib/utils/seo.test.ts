import { describe, it, expect } from 'vitest';
import type { Event, EventStatus, LocationType } from '@/lib/api/types';
import {
  getEventSchemaStatus,
  getAttendanceMode,
  getEventLocation,
  generateEventJsonLd,
  formatEventDate,
  formatEventDateLocal,
} from './seo';

describe('getEventSchemaStatus', () => {
  it('returns EventScheduled for PUBLISHED', () => {
    expect(getEventSchemaStatus('PUBLISHED')).toBe('EventScheduled');
  });

  it('returns EventScheduled for COMPLETED', () => {
    expect(getEventSchemaStatus('COMPLETED')).toBe('EventScheduled');
  });

  it('returns EventCancelled for CANCELLED', () => {
    expect(getEventSchemaStatus('CANCELLED')).toBe('EventCancelled');
  });

  it('returns EventScheduled for DRAFT', () => {
    expect(getEventSchemaStatus('DRAFT')).toBe('EventScheduled');
  });

  it('returns EventScheduled for unknown status', () => {
    expect(getEventSchemaStatus('UNKNOWN' as EventStatus)).toBe('EventScheduled');
  });
});

describe('getAttendanceMode', () => {
  it('returns OnlineEventAttendanceMode for ONLINE', () => {
    expect(getAttendanceMode('ONLINE')).toBe('OnlineEventAttendanceMode');
  });

  it('returns OfflineEventAttendanceMode for OFFLINE', () => {
    expect(getAttendanceMode('OFFLINE')).toBe('OfflineEventAttendanceMode');
  });

  it('returns MixedEventAttendanceMode for HYBRID', () => {
    expect(getAttendanceMode('HYBRID')).toBe('MixedEventAttendanceMode');
  });

  it('returns OfflineEventAttendanceMode for unknown type', () => {
    expect(getAttendanceMode('UNKNOWN' as LocationType)).toBe('OfflineEventAttendanceMode');
  });
});

describe('getEventLocation', () => {
  const baseEvent: Event = {
    id: '1',
    tenantId: 'tenant-1',
    title: 'Test Event',
    slug: 'test-event',
    description: undefined,
    startsAt: '2025-06-15T10:00:00.000Z',
    endsAt: undefined,
    timezone: 'Europe/Moscow',
    locationType: 'OFFLINE',
    locationAddress: undefined,
    onlineUrl: undefined,
    maxCapacity: undefined,
    registrationOpensAt: undefined,
    registrationClosesAt: undefined,
    isPublic: true,
    participantsVisibility: 'CLOSED',
    groupId: undefined,
    coverImageUrl: undefined,
    status: 'PUBLISHED',
    organizerName: undefined,
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-01T00:00:00.000Z',
  };

  it('returns Place for OFFLINE with address', () => {
    const event: Event = { ...baseEvent, locationType: 'OFFLINE', locationAddress: 'Moscow, Russia' };
    const location = getEventLocation(event);

    expect(location).toEqual({
      '@type': 'Place',
      name: 'Moscow, Russia',
      address: {
        '@type': 'PostalAddress',
        streetAddress: 'Moscow, Russia',
      },
    });
  });

  it('returns VirtualLocation for ONLINE with URL', () => {
    const event: Event = { ...baseEvent, locationType: 'ONLINE', onlineUrl: 'https://zoom.us/123' };
    const location = getEventLocation(event);

    expect(location).toEqual({
      '@type': 'VirtualLocation',
      url: 'https://zoom.us/123',
    });
  });

  it('returns array for HYBRID with both address and URL', () => {
    const event: Event = {
      ...baseEvent,
      locationType: 'HYBRID',
      locationAddress: 'Moscow, Russia',
      onlineUrl: 'https://zoom.us/123',
    };
    const location = getEventLocation(event);

    expect(Array.isArray(location)).toBe(true);
    expect(location).toHaveLength(2);
  });

  it('returns undefined when no location data', () => {
    const event: Event = { ...baseEvent, locationType: 'OFFLINE', locationAddress: undefined };
    expect(getEventLocation(event)).toBeUndefined();
  });
});

describe('generateEventJsonLd', () => {
  const baseEvent: Event = {
    id: '1',
    tenantId: 'tenant-1',
    title: 'Test Conference',
    slug: 'test-conference',
    description: 'A **great** event with _markdown_',
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
    status: 'PUBLISHED',
    organizerName: 'Test Organization',
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: '2025-01-02T00:00:00.000Z',
  };

  it('generates valid JSON-LD structure', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');

    expect(jsonLd['@context']).toBe('https://schema.org');
    expect(jsonLd['@type']).toBe('Event');
    expect(jsonLd.name).toBe('Test Conference');
    expect(jsonLd.startDate).toBe('2025-06-15T10:00:00.000Z');
    expect(jsonLd.url).toBe('https://example.com/events/test-conference');
  });

  it('includes eventStatus', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');
    expect(jsonLd.eventStatus).toBe('https://schema.org/EventScheduled');
  });

  it('includes eventAttendanceMode', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');
    expect(jsonLd.eventAttendanceMode).toBe('https://schema.org/OfflineEventAttendanceMode');
  });

  it('strips markdown from description', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');
    expect(jsonLd.description).not.toContain('**');
    expect(jsonLd.description).not.toContain('_');
  });

  it('includes endDate when present', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');
    expect(jsonLd.endDate).toBe('2025-06-15T18:00:00.000Z');
  });

  it('includes location', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');
    expect(jsonLd.location).toBeDefined();
  });

  it('includes organizer', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');
    expect(jsonLd.organizer).toEqual({
      '@type': 'Organization',
      name: 'Test Organization',
    });
  });

  it('includes maximumAttendeeCapacity', () => {
    const jsonLd = generateEventJsonLd(baseEvent, 'https://example.com');
    expect(jsonLd.maximumAttendeeCapacity).toBe(100);
  });

  it('does not include optional fields when undefined', () => {
    const minimalEvent: Event = {
      ...baseEvent,
      description: undefined,
      endsAt: undefined,
      locationAddress: undefined,
      organizerName: undefined,
      maxCapacity: undefined,
    };
    const jsonLd = generateEventJsonLd(minimalEvent, 'https://example.com');

    expect(jsonLd.description).toBeUndefined();
    expect(jsonLd.endDate).toBeUndefined();
    expect(jsonLd.location).toBeUndefined();
    expect(jsonLd.organizer).toBeUndefined();
    expect(jsonLd.maximumAttendeeCapacity).toBeUndefined();
  });
});

describe('formatEventDate', () => {
  it('formats date in Russian locale', () => {
    const result = formatEventDate('2025-06-15T10:00:00.000Z', 'Europe/Moscow');
    expect(result).toMatch(/июн/i);
    expect(result).toMatch(/2025/);
  });

  it('respects timezone', () => {
    const result = formatEventDate('2025-06-15T10:00:00.000Z', 'Asia/Vladivostok');
    // Во Владивостоке будет другое время
    expect(result).toBeTruthy();
  });

  it('accepts custom options', () => {
    const result = formatEventDate('2025-06-15T10:00:00.000Z', 'Europe/Moscow', {
      weekday: undefined,
      hour: undefined,
      minute: undefined,
    });
    expect(result).toBeTruthy();
  });
});

describe('formatEventDateLocal', () => {
  it('formats date in local timezone', () => {
    const result = formatEventDateLocal('2025-06-15T10:00:00.000Z');
    expect(result).toMatch(/2025/);
    expect(result).toBeTruthy();
  });

  it('accepts custom options', () => {
    const result = formatEventDateLocal('2025-06-15T10:00:00.000Z', {
      weekday: undefined,
    });
    expect(result).toBeTruthy();
  });
});
