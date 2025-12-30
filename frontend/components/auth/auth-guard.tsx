'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/auth-store';
import { Skeleton } from '@/components/ui/skeleton';

interface AuthGuardProps {
  children: React.ReactNode;
}

/**
 * Время ожидания гидратации Zustand store (мс).
 * Увеличено для надёжности на медленных соединениях.
 */
const HYDRATION_TIMEOUT_MS = 150;

/**
 * Компонент защиты приватных роутов.
 * Проверяет авторизацию и редиректит на /login если пользователь не авторизован.
 */
export function AuthGuard({ children }: AuthGuardProps) {
  const router = useRouter();
  const { isAuthenticated, accessToken } = useAuthStore();
  const [isLoading, setIsLoading] = useState(true);
  const [hasChecked, setHasChecked] = useState(false);

  const checkAuth = useCallback(() => {
    const { isAuthenticated: isAuth, accessToken: token } = useAuthStore.getState();

    setHasChecked(true);

    if (!isAuth || !token) {
      router.replace('/login');
    } else {
      setIsLoading(false);
    }
  }, [router]);

  useEffect(() => {
    // Даём время для гидратации Zustand store из localStorage
    const timer = setTimeout(checkAuth, HYDRATION_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [checkAuth]);

  // Показываем скелет пока проверяем авторизацию
  if (isLoading) {
    return <AuthGuardSkeleton />;
  }

  // Если не авторизован, не рендерим children (redirect уже запущен)
  if (!isAuthenticated || !accessToken) {
    return <AuthGuardSkeleton />;
  }

  return <>{children}</>;
}

/**
 * Скелет загрузки для AuthGuard.
 */
function AuthGuardSkeleton() {
  return (
    <div className="flex min-h-screen w-full">
      {/* Sidebar skeleton */}
      <div className="hidden md:block w-[220px] lg:w-[280px] border-r p-4">
        <Skeleton className="h-8 w-32 mb-6" />
        <div className="space-y-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      </div>

      {/* Content skeleton */}
      <div className="flex-1 flex flex-col">
        {/* Header skeleton */}
        <div className="h-14 border-b px-4 flex items-center justify-between">
          <Skeleton className="h-8 w-8 md:hidden" />
          <div className="flex-1" />
          <Skeleton className="h-8 w-8 rounded-full" />
        </div>

        {/* Main content skeleton */}
        <div className="flex-1 p-4 lg:p-6">
          <Skeleton className="h-8 w-48 mb-6" />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
          </div>
        </div>
      </div>
    </div>
  );
}
