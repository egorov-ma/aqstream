import type { Event, LocationType } from '@/lib/api/types';

/**
 * Получает текстовое представление местоположения события
 * @param event - событие
 * @param options - опции форматирования
 * @returns строка с местоположением
 */
export function getEventLocation(
  event: Pick<Event, 'locationType' | 'locationAddress' | 'onlineUrl' | 'location'>,
  options?: {
    /** Показывать URL для онлайн события (по умолчанию false — только "Онлайн") */
    showOnlineUrl?: boolean;
    /** Текст по умолчанию если место не указано */
    fallback?: string;
  }
): string {
  const { showOnlineUrl = false, fallback = 'Место будет уточнено' } = options ?? {};

  if (event.locationType === 'ONLINE') {
    if (showOnlineUrl && event.onlineUrl) {
      return event.onlineUrl;
    }
    return 'Онлайн';
  }

  // OFFLINE или HYBRID
  return event.locationAddress || event.location || fallback;
}

/**
 * Получает местоположение для ICS календаря
 * Для онлайн событий возвращает URL, для офлайн — адрес
 */
export function getEventLocationForCalendar(
  event: Pick<Event, 'locationType' | 'locationAddress' | 'onlineUrl' | 'location'>
): string | undefined {
  if (event.locationType === 'ONLINE') {
    return event.onlineUrl || 'Онлайн';
  }
  return event.locationAddress || event.location || undefined;
}

/**
 * Получает короткий label для типа локации
 */
export function getLocationTypeLabel(locationType: LocationType): string {
  switch (locationType) {
    case 'ONLINE':
      return 'Онлайн';
    case 'OFFLINE':
      return 'Офлайн';
    case 'HYBRID':
      return 'Гибрид';
    default:
      return '';
  }
}

/**
 * Получает полный label для типа локации
 */
export function getLocationTypeLabelFull(locationType: LocationType): string {
  switch (locationType) {
    case 'ONLINE':
      return 'Онлайн';
    case 'OFFLINE':
      return 'Офлайн';
    case 'HYBRID':
      return 'Гибрид (онлайн + офлайн)';
    default:
      return '';
  }
}
