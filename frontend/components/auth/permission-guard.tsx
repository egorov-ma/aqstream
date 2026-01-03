'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/auth-store';
import { usePermissions } from '@/lib/hooks/use-permissions';
import { Skeleton } from '@/components/ui/skeleton';

type PermissionKey = keyof ReturnType<typeof usePermissions>;

interface PermissionGuardProps {
  children: React.ReactNode;
  /**
   * Ключ разрешения из usePermissions.
   * Например: 'canCreateEvent', 'canEditEvent', 'canViewAnalytics'
   */
  requiredPermission: PermissionKey;
  /**
   * Путь для редиректа при отсутствии прав.
   * По умолчанию: '/dashboard'
   */
  fallbackPath?: string;
}

/**
 * Время ожидания гидратации Zustand store (мс).
 */
const HYDRATION_TIMEOUT_MS = 150;

/**
 * Компонент защиты роутов по правам доступа.
 *
 * Проверяет:
 * 1. Является ли пользователь админом платформы (isAdmin)
 * 2. Имеет ли пользователь требуемое разрешение (из usePermissions)
 *
 * Админы получают доступ ко всем защищённым страницам.
 */
export function PermissionGuard({
  children,
  requiredPermission,
  fallbackPath = '/dashboard',
}: PermissionGuardProps) {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const permissions = usePermissions();
  const [isLoading, setIsLoading] = useState(true);

  const checkPermission = useCallback(() => {
    const currentUser = useAuthStore.getState().user;
    const isAdmin = currentUser?.isAdmin ?? false;
    const hasPermission = permissions[requiredPermission];

    if (!isAdmin && !hasPermission) {
      router.replace(fallbackPath);
    } else {
      setIsLoading(false);
    }
  }, [router, permissions, requiredPermission, fallbackPath]);

  useEffect(() => {
    // Даём время для гидратации Zustand store из localStorage
    const timer = setTimeout(checkPermission, HYDRATION_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [checkPermission]);

  // Показываем скелет пока проверяем права
  if (isLoading) {
    return <PermissionGuardSkeleton />;
  }

  // Проверяем права после загрузки
  const isAdmin = user?.isAdmin ?? false;
  const hasPermission = permissions[requiredPermission];

  if (!isAdmin && !hasPermission) {
    return <PermissionGuardSkeleton />;
  }

  return <>{children}</>;
}

/**
 * Скелет загрузки для PermissionGuard.
 */
function PermissionGuardSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-4">
        <Skeleton className="h-10 w-10" />
        <div className="space-y-2">
          <Skeleton className="h-7 w-48" />
          <Skeleton className="h-4 w-64" />
        </div>
      </div>
      <div className="space-y-4">
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    </div>
  );
}
