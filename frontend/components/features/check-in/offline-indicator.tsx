'use client';

import { useEffect, useState } from 'react';
import { Wifi, WifiOff, RefreshCw } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { hasPendingCheckIns, getPendingCheckIns } from '@/lib/pwa/offline-storage';

interface OfflineIndicatorProps {
  onSync?: () => void;
  isSyncing?: boolean;
}

/**
 * Индикатор офлайн-режима и статуса синхронизации.
 */
export function OfflineIndicator({ onSync, isSyncing = false }: OfflineIndicatorProps) {
  const [isOnline, setIsOnline] = useState(true);
  const [pendingCount, setPendingCount] = useState(0);

  useEffect(() => {
    // Проверяем начальное состояние
    setIsOnline(navigator.onLine);

    // Проверяем pending check-ins
    const checkPending = async () => {
      const pending = await getPendingCheckIns();
      setPendingCount(pending.length);
    };
    checkPending();

    // Подписываемся на изменения сети
    const handleOnline = () => {
      setIsOnline(true);
      // При восстановлении сети автоматически синхронизируем
      if (onSync) {
        setTimeout(onSync, 1000);
      }
    };

    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Периодически проверяем pending
    const interval = setInterval(checkPending, 5000);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      clearInterval(interval);
    };
  }, [onSync]);

  return (
    <div className="flex items-center gap-2">
      {isOnline ? (
        <Badge variant="outline" className="border-green-500 text-green-600">
          <Wifi className="mr-1 h-3 w-3" />
          Онлайн
        </Badge>
      ) : (
        <Badge variant="destructive">
          <WifiOff className="mr-1 h-3 w-3" />
          Офлайн
        </Badge>
      )}

      {pendingCount > 0 && (
        <>
          <Badge variant="secondary">
            {pendingCount} ожидает синхронизации
          </Badge>

          {isOnline && onSync && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onSync}
              disabled={isSyncing}
            >
              <RefreshCw className={`h-4 w-4 ${isSyncing ? 'animate-spin' : ''}`} />
            </Button>
          )}
        </>
      )}
    </div>
  );
}
