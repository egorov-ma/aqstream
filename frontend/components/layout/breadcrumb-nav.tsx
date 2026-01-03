'use client';

import Link from 'next/link';
import { Home } from 'lucide-react';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
  BreadcrumbEllipsis,
} from '@/components/ui/breadcrumb';
import { Skeleton } from '@/components/ui/skeleton';
import { useBreadcrumbs, type BreadcrumbItem as BreadcrumbItemType } from '@/lib/hooks/use-breadcrumbs';
import { cn } from '@/lib/utils';
import { Fragment } from 'react';

// На мобильных показываем первый, ..., последние 2
const MAX_VISIBLE_ITEMS = 4;

export function BreadcrumbNav() {
  const items = useBreadcrumbs();

  // Не показываем breadcrumbs на главной странице dashboard
  if (items.length <= 1) {
    return null;
  }

  // Для мобильной адаптации: сворачиваем длинные пути
  const shouldCollapse = items.length > MAX_VISIBLE_ITEMS;

  return (
    <nav aria-label="Breadcrumb" className="border-b bg-muted/20 px-4 py-2 lg:px-6">
      <Breadcrumb>
        <BreadcrumbList>
          {items.map((item, index) => {
            const isFirst = index === 0;
            const isLast = index === items.length - 1;

            // При сворачивании: показываем первый, многоточие, и последние 2
            if (shouldCollapse && index > 0 && index < items.length - 2) {
              // Показываем многоточие только для первого скрытого элемента
              if (index === 1) {
                return (
                  <Fragment key="ellipsis">
                    <BreadcrumbSeparator />
                    <BreadcrumbItem>
                      <BreadcrumbEllipsis className="h-4 w-4" />
                    </BreadcrumbItem>
                  </Fragment>
                );
              }
              // Скрываем остальные
              return null;
            }

            return (
              <Fragment key={`${item.label}-${index}`}>
                {/* Разделитель */}
                {index > 0 && <BreadcrumbSeparator />}

                {/* Элемент breadcrumb */}
                <BreadcrumbItem>
                  <BreadcrumbContent item={item} isFirst={isFirst} isLast={isLast} />
                </BreadcrumbItem>
              </Fragment>
            );
          })}
        </BreadcrumbList>
      </Breadcrumb>

      {/* JSON-LD для SEO */}
      <BreadcrumbJsonLd items={items} />
    </nav>
  );
}

function BreadcrumbContent({
  item,
  isFirst,
  isLast,
}: {
  item: BreadcrumbItemType;
  isFirst: boolean;
  isLast: boolean;
}) {
  // Loading state
  if (item.isLoading) {
    return <Skeleton className="h-4 w-24" />;
  }

  // Текущая страница (некликабельная)
  if (item.isCurrent || !item.href) {
    return (
      <BreadcrumbPage className={cn('flex items-center gap-1.5', isFirst && 'font-medium')}>
        {isFirst && <Home className="h-3.5 w-3.5" />}
        <span className={cn('truncate', isLast ? 'max-w-[200px] md:max-w-[300px]' : 'max-w-[150px]')}>
          {item.label}
        </span>
      </BreadcrumbPage>
    );
  }

  // Кликабельная ссылка
  return (
    <BreadcrumbLink asChild>
      <Link href={item.href} className={cn('flex items-center gap-1.5', isFirst && 'font-medium')}>
        {isFirst && <Home className="h-3.5 w-3.5" />}
        <span className="max-w-[150px] truncate">{item.label}</span>
      </Link>
    </BreadcrumbLink>
  );
}

/**
 * JSON-LD для SEO (структурированные данные Google).
 */
function BreadcrumbJsonLd({ items }: { items: BreadcrumbItemType[] }) {
  if (typeof window === 'undefined') return null;

  const baseUrl = window.location.origin;

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: item.label,
      item: item.href ? `${baseUrl}${item.href}` : undefined,
    })),
  };

  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
    />
  );
}
