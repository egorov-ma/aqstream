import type { LocationType } from '@/lib/api/types';

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
