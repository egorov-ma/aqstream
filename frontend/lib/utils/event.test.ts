import { describe, it, expect } from 'vitest';
import {
  getEventLocation,
  getEventLocationForCalendar,
  getLocationTypeLabel,
  getLocationTypeLabelFull,
} from './event';

describe('getEventLocation', () => {
  it('returns "Онлайн" for online events without URL', () => {
    const event = {
      locationType: 'ONLINE' as const,
      locationAddress: undefined,
      onlineUrl: undefined,
      location: undefined,
    };

    expect(getEventLocation(event)).toBe('Онлайн');
  });

  it('returns "Онлайн" for online events even with URL by default', () => {
    const event = {
      locationType: 'ONLINE' as const,
      locationAddress: undefined,
      onlineUrl: 'https://zoom.us/meeting',
      location: undefined,
    };

    expect(getEventLocation(event)).toBe('Онлайн');
  });

  it('returns URL for online events when showOnlineUrl is true', () => {
    const event = {
      locationType: 'ONLINE' as const,
      locationAddress: undefined,
      onlineUrl: 'https://zoom.us/meeting',
      location: undefined,
    };

    expect(getEventLocation(event, { showOnlineUrl: true })).toBe('https://zoom.us/meeting');
  });

  it('returns locationAddress for offline events', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: 'Москва, ул. Пушкина, д. 10',
      onlineUrl: undefined,
      location: undefined,
    };

    expect(getEventLocation(event)).toBe('Москва, ул. Пушкина, д. 10');
  });

  it('falls back to location field for backwards compatibility', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: undefined,
      onlineUrl: undefined,
      location: 'Старый формат адреса',
    };

    expect(getEventLocation(event)).toBe('Старый формат адреса');
  });

  it('returns default fallback when no location is specified', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: undefined,
      onlineUrl: undefined,
      location: undefined,
    };

    expect(getEventLocation(event)).toBe('Место будет уточнено');
  });

  it('returns custom fallback when specified', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: undefined,
      onlineUrl: undefined,
      location: undefined,
    };

    expect(getEventLocation(event, { fallback: 'TBD' })).toBe('TBD');
  });

  it('handles hybrid events same as offline', () => {
    const event = {
      locationType: 'HYBRID' as const,
      locationAddress: 'Конференц-зал',
      onlineUrl: 'https://zoom.us/meeting',
      location: undefined,
    };

    expect(getEventLocation(event)).toBe('Конференц-зал');
  });

  it('prefers locationAddress over location', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: 'Новый адрес',
      onlineUrl: undefined,
      location: 'Старый адрес',
    };

    expect(getEventLocation(event)).toBe('Новый адрес');
  });
});

describe('getEventLocationForCalendar', () => {
  it('returns URL for online events', () => {
    const event = {
      locationType: 'ONLINE' as const,
      locationAddress: undefined,
      onlineUrl: 'https://zoom.us/meeting',
      location: undefined,
    };

    expect(getEventLocationForCalendar(event)).toBe('https://zoom.us/meeting');
  });

  it('returns "Онлайн" for online events without URL', () => {
    const event = {
      locationType: 'ONLINE' as const,
      locationAddress: undefined,
      onlineUrl: undefined,
      location: undefined,
    };

    expect(getEventLocationForCalendar(event)).toBe('Онлайн');
  });

  it('returns locationAddress for offline events', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: 'Москва, ул. Пушкина, д. 10',
      onlineUrl: undefined,
      location: undefined,
    };

    expect(getEventLocationForCalendar(event)).toBe('Москва, ул. Пушкина, д. 10');
  });

  it('returns undefined when no location is specified for offline events', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: undefined,
      onlineUrl: undefined,
      location: undefined,
    };

    expect(getEventLocationForCalendar(event)).toBeUndefined();
  });

  it('falls back to location field for backwards compatibility', () => {
    const event = {
      locationType: 'OFFLINE' as const,
      locationAddress: undefined,
      onlineUrl: undefined,
      location: 'Старый формат адреса',
    };

    expect(getEventLocationForCalendar(event)).toBe('Старый формат адреса');
  });
});

describe('getLocationTypeLabel', () => {
  it('returns "Онлайн" for ONLINE', () => {
    expect(getLocationTypeLabel('ONLINE')).toBe('Онлайн');
  });

  it('returns "Офлайн" for OFFLINE', () => {
    expect(getLocationTypeLabel('OFFLINE')).toBe('Офлайн');
  });

  it('returns "Гибрид" for HYBRID', () => {
    expect(getLocationTypeLabel('HYBRID')).toBe('Гибрид');
  });
});

describe('getLocationTypeLabelFull', () => {
  it('returns "Онлайн" for ONLINE', () => {
    expect(getLocationTypeLabelFull('ONLINE')).toBe('Онлайн');
  });

  it('returns "Офлайн" for OFFLINE', () => {
    expect(getLocationTypeLabelFull('OFFLINE')).toBe('Офлайн');
  });

  it('returns full label for HYBRID', () => {
    expect(getLocationTypeLabelFull('HYBRID')).toBe('Гибрид (онлайн + офлайн)');
  });
});
