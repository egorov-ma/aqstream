'use client';

import { useMemo } from 'react';
import { usePathname } from 'next/navigation';
import { useEvent } from '@/lib/hooks/use-events';
import { useRegistration } from '@/lib/hooks/use-registrations';
import {
  BREADCRUMB_SEGMENTS,
  pathToConfigKey,
  extractIdFromPath,
  findDynamicSegments,
  type EntityType,
} from '@/lib/config/breadcrumbs';

export interface BreadcrumbItem {
  /** Название сегмента */
  label: string;
  /** URL для перехода. null = текущая страница */
  href: string | null;
  /** Идёт загрузка названия */
  isLoading?: boolean;
  /** Это текущая страница */
  isCurrent: boolean;
}

interface DynamicSegmentInfo {
  entityType: EntityType;
  id: string;
  segmentIndex: number;
}

/**
 * Хук для получения breadcrumb items на основе текущего пути.
 */
export function useBreadcrumbs(): BreadcrumbItem[] {
  const pathname = usePathname();

  // Разбираем путь на сегменты
  const segments = useMemo(() => pathname.split('/').filter(Boolean), [pathname]);

  // Находим динамические сегменты для загрузки названий
  const dynamicSegments = useMemo((): DynamicSegmentInfo[] => {
    return findDynamicSegments(pathname);
  }, [pathname]);

  // Загружаем названия для событий
  const eventInfo = dynamicSegments.find((s) => s.entityType === 'event');
  const { data: event, isLoading: isEventLoading } = useEvent(eventInfo?.id ?? '');

  // Загружаем названия для регистраций
  const registrationInfo = dynamicSegments.find((s) => s.entityType === 'registration');
  const { data: registration, isLoading: isRegistrationLoading } = useRegistration(
    registrationInfo?.id ?? ''
  );

  // Формируем breadcrumb items
  const items = useMemo((): BreadcrumbItem[] => {
    const result: BreadcrumbItem[] = [];

    for (let i = 0; i < segments.length; i++) {
      const partialPath = segments.slice(0, i + 1).join('/');
      const configKey = pathToConfigKey(partialPath);
      const config = BREADCRUMB_SEGMENTS[configKey];

      if (!config) {
        // Пропускаем сегменты без конфигурации (например, промежуточные ID)
        continue;
      }

      const isCurrent = i === segments.length - 1;
      let label = config.label;
      let isLoading = false;

      // Заменяем label для динамических сегментов
      if (config.entityType === 'event' && eventInfo?.segmentIndex === i) {
        label = event?.title ?? config.label;
        isLoading = isEventLoading;
      } else if (config.entityType === 'registration' && registrationInfo?.segmentIndex === i) {
        label = registration?.eventTitle ?? config.label;
        isLoading = isRegistrationLoading;
      }

      // Формируем href
      let href = config.href;
      if (href && href.includes('[id]')) {
        // Заменяем [id] на реальный ID
        const id = extractIdFromPath(pathname, i);
        if (id) {
          href = href.replace('[id]', id);
        }
      }

      result.push({
        label,
        href: isCurrent ? null : href,
        isLoading,
        isCurrent,
      });
    }

    return result;
  }, [
    segments,
    pathname,
    event,
    registration,
    eventInfo,
    registrationInfo,
    isEventLoading,
    isRegistrationLoading,
  ]);

  return items;
}
