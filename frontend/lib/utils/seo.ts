import type { Event, EventStatus, LocationType } from '@/lib/api/types';

/**
 * Маппинг статуса события в Schema.org EventStatusType
 * https://schema.org/EventStatusType
 */
export function getEventSchemaStatus(
  status: EventStatus
): 'EventScheduled' | 'EventCancelled' | 'EventPostponed' | 'EventRescheduled' {
  switch (status) {
    case 'PUBLISHED':
    case 'COMPLETED':
      return 'EventScheduled';
    case 'CANCELLED':
      return 'EventCancelled';
    case 'DRAFT':
    default:
      return 'EventScheduled';
  }
}

/**
 * Маппинг типа локации в Schema.org EventAttendanceModeEnumeration
 * https://schema.org/EventAttendanceModeEnumeration
 */
export function getAttendanceMode(
  locationType: LocationType
): 'OfflineEventAttendanceMode' | 'OnlineEventAttendanceMode' | 'MixedEventAttendanceMode' {
  switch (locationType) {
    case 'ONLINE':
      return 'OnlineEventAttendanceMode';
    case 'OFFLINE':
      return 'OfflineEventAttendanceMode';
    case 'HYBRID':
      return 'MixedEventAttendanceMode';
    default:
      return 'OfflineEventAttendanceMode';
  }
}

/**
 * Формирует location для JSON-LD Schema.org Event
 */
export function getEventLocation(event: Event): Record<string, unknown> | undefined {
  const locations: Record<string, unknown>[] = [];

  // Физическое место
  if (
    (event.locationType === 'OFFLINE' || event.locationType === 'HYBRID') &&
    event.locationAddress
  ) {
    locations.push({
      '@type': 'Place',
      name: event.locationAddress,
      address: {
        '@type': 'PostalAddress',
        streetAddress: event.locationAddress,
      },
    });
  }

  // Онлайн место
  if (
    (event.locationType === 'ONLINE' || event.locationType === 'HYBRID') &&
    event.onlineUrl
  ) {
    locations.push({
      '@type': 'VirtualLocation',
      url: event.onlineUrl,
    });
  }

  if (locations.length === 0) {
    return undefined;
  }

  if (locations.length === 1) {
    return locations[0];
  }

  // Для HYBRID возвращаем массив
  return locations as unknown as Record<string, unknown>;
}

/**
 * Генерирует JSON-LD структурированные данные для события
 * https://schema.org/Event
 */
export function generateEventJsonLd(event: Event, baseUrl: string): Record<string, unknown> {
  const eventUrl = `${baseUrl}/events/${event.slug}`;

  const jsonLd: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'Event',
    name: event.title,
    startDate: event.startsAt,
    eventStatus: `https://schema.org/${getEventSchemaStatus(event.status)}`,
    eventAttendanceMode: `https://schema.org/${getAttendanceMode(event.locationType)}`,
    url: eventUrl,
  };

  // Описание
  if (event.description) {
    // Убираем markdown разметку для description
    jsonLd.description = event.description
      .replace(/[#*_`\[\]()]/g, '')
      .slice(0, 500);
  }

  // Дата окончания
  if (event.endsAt) {
    jsonLd.endDate = event.endsAt;
  }

  // Место проведения
  const location = getEventLocation(event);
  if (location) {
    jsonLd.location = location;
  }

  // Организатор
  if (event.organizerName) {
    jsonLd.organizer = {
      '@type': 'Organization',
      name: event.organizerName,
    };
  }

  // Максимальное количество участников
  if (event.maxCapacity) {
    jsonLd.maximumAttendeeCapacity = event.maxCapacity;
  }

  return jsonLd;
}

/**
 * Форматирует дату для отображения
 */
export function formatEventDate(
  dateString: string,
  timezone: string,
  options?: Intl.DateTimeFormatOptions
): string {
  const date = new Date(dateString);
  const defaultOptions: Intl.DateTimeFormatOptions = {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: timezone,
    ...options,
  };

  return date.toLocaleString('ru-RU', defaultOptions);
}

/**
 * Форматирует дату в локальном времени пользователя
 */
export function formatEventDateLocal(
  dateString: string,
  options?: Intl.DateTimeFormatOptions
): string {
  const date = new Date(dateString);
  const defaultOptions: Intl.DateTimeFormatOptions = {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  };

  return date.toLocaleString('ru-RU', defaultOptions);
}
