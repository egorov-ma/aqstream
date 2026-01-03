/**
 * Конфигурация breadcrumbs для dashboard маршрутов.
 *
 * Каждый ключ соответствует пути без начального слеша.
 * Динамические сегменты обозначаются как [id].
 */

export type EntityType = 'event' | 'registration';

export interface BreadcrumbSegmentConfig {
  /** Название сегмента (статичное) */
  label: string;
  /** URL для перехода. null = текущая страница (некликабельная) */
  href: string | null;
  /** Тип сущности для загрузки динамического названия */
  entityType?: EntityType;
}

/**
 * Конфигурация статичных сегментов.
 * Ключ: путь с [id] для динамических сегментов.
 */
export const BREADCRUMB_SEGMENTS: Record<string, BreadcrumbSegmentConfig> = {
  // Root
  dashboard: {
    label: 'Главная',
    href: '/dashboard',
  },

  // Events
  'dashboard/events': {
    label: 'События',
    href: '/dashboard/events',
  },
  'dashboard/events/new': {
    label: 'Новое событие',
    href: null,
  },
  'dashboard/events/[id]': {
    label: 'Событие', // fallback, будет заменено динамически
    href: '/dashboard/events/[id]',
    entityType: 'event',
  },
  'dashboard/events/[id]/edit': {
    label: 'Редактирование',
    href: null,
  },
  'dashboard/events/[id]/check-in': {
    label: 'Check-in',
    href: null,
  },

  // My Registrations
  'dashboard/my-registrations': {
    label: 'Мои билеты',
    href: '/dashboard/my-registrations',
  },
  'dashboard/my-registrations/[id]': {
    label: 'Билет', // fallback
    href: null,
    entityType: 'registration',
  },

  // Account
  'dashboard/account': {
    label: 'Аккаунт',
    href: '/dashboard/account',
  },
  'dashboard/account/profile': {
    label: 'Профиль',
    href: null,
  },
  'dashboard/account/organizations': {
    label: 'Организации',
    href: null,
  },
  'dashboard/account/telegram': {
    label: 'Telegram',
    href: null,
  },
  'dashboard/account/notifications': {
    label: 'Уведомления',
    href: null,
  },
  'dashboard/account/groups': {
    label: 'Группы',
    href: null,
  },

  // Admin
  'dashboard/admin': {
    label: 'Администрирование',
    href: null,
  },
  'dashboard/admin/organization-requests': {
    label: 'Заявки на организации',
    href: null,
  },

  // Other
  'dashboard/organization-request': {
    label: 'Подать заявку',
    href: null,
  },
  'dashboard/registrations': {
    label: 'Регистрации',
    href: null,
  },
  'dashboard/analytics': {
    label: 'Аналитика',
    href: null,
  },
  'dashboard/settings': {
    label: 'Настройки',
    href: null,
  },
};

// Regex для UUID
const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Преобразует реальный путь в ключ конфигурации.
 * Заменяет UUID-сегменты на [id].
 */
export function pathToConfigKey(path: string): string {
  const segments = path.split('/').filter(Boolean);

  return segments
    .map((segment) => {
      // Проверяем, является ли сегмент UUID
      return UUID_REGEX.test(segment) ? '[id]' : segment;
    })
    .join('/');
}

/**
 * Извлекает ID из пути для указанного индекса сегмента.
 */
export function extractIdFromPath(path: string, segmentIndex: number): string | null {
  const segments = path.split('/').filter(Boolean);
  const segment = segments[segmentIndex];

  if (segment && UUID_REGEX.test(segment)) {
    return segment;
  }

  return null;
}

/**
 * Находит все динамические сегменты в пути.
 */
export function findDynamicSegments(
  path: string
): Array<{ entityType: EntityType; id: string; segmentIndex: number }> {
  const segments = path.split('/').filter(Boolean);
  const result: Array<{ entityType: EntityType; id: string; segmentIndex: number }> = [];

  for (let i = 0; i < segments.length; i++) {
    const partialPath = segments.slice(0, i + 1).join('/');
    const configKey = pathToConfigKey(partialPath);
    const config = BREADCRUMB_SEGMENTS[configKey];

    if (config?.entityType) {
      const id = extractIdFromPath(path, i);
      if (id) {
        result.push({
          entityType: config.entityType,
          id,
          segmentIndex: i,
        });
      }
    }
  }

  return result;
}
