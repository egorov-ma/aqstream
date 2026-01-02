'use client';

import { useRouter } from 'next/navigation';
import { formatDistanceToNow } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Bell, Check, CheckCheck, Info, AlertTriangle, Clock, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Skeleton } from '@/components/ui/skeleton';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  useNotifications,
  useUnreadCount,
  useMarkAsRead,
  useMarkAllAsRead,
} from '@/lib/hooks/use-notifications';
import type { UserNotification, UserNotificationType } from '@/lib/api/types';
import { cn } from '@/lib/utils';

/**
 * Иконка для типа уведомления.
 */
function NotificationIcon({ type }: { type: UserNotificationType }) {
  switch (type) {
    case 'NEW_REGISTRATION':
      return <Check className="h-4 w-4 text-green-500" />;
    case 'EVENT_UPDATE':
      return <Info className="h-4 w-4 text-blue-500" />;
    case 'EVENT_CANCELLED':
      return <AlertTriangle className="h-4 w-4 text-red-500" />;
    case 'EVENT_REMINDER':
      return <Clock className="h-4 w-4 text-orange-500" />;
    case 'SYSTEM':
    default:
      return <Bell className="h-4 w-4 text-muted-foreground" />;
  }
}

export function Notifications() {
  const router = useRouter();
  const { data: unreadCount = 0, isLoading: isCountLoading } = useUnreadCount();
  const {
    data: notifications,
    isLoading: isNotificationsLoading,
    isError: isNotificationsError,
    refetch: refetchNotifications,
  } = useNotifications(0, 10);
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();

  const handleNotificationClick = (notification: UserNotification) => {
    // Отмечаем как прочитанное
    if (!notification.isRead) {
      markAsRead.mutate(notification.id);
    }

    // Навигация к связанной сущности
    if (notification.linkedEntity) {
      const { entityType, entityId } = notification.linkedEntity;
      if (entityType === 'EVENT') {
        router.push(`/dashboard/events/${entityId}`);
      } else if (entityType === 'REGISTRATION') {
        router.push(`/dashboard/registrations`);
      }
    }
  };

  const handleMarkAllAsRead = () => {
    markAllAsRead.mutate();
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="relative h-9 w-9">
          <Bell className="h-4 w-4" />
          {!isCountLoading && unreadCount > 0 && (
            <Badge
              variant="destructive"
              className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center text-xs"
            >
              {unreadCount > 9 ? '9+' : unreadCount}
            </Badge>
          )}
          <span className="sr-only">Уведомления</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-80" align="end">
        <DropdownMenuLabel className="flex items-center justify-between">
          <span>Уведомления</span>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-auto py-1 px-2 text-xs"
              onClick={handleMarkAllAsRead}
              disabled={markAllAsRead.isPending}
            >
              <CheckCheck className="h-3 w-3 mr-1" />
              Прочитать все
            </Button>
          )}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />

        {isNotificationsLoading ? (
          <div className="p-4 space-y-3">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : isNotificationsError ? (
          <div className="p-8 text-center text-muted-foreground">
            <AlertTriangle className="h-8 w-8 mx-auto mb-2 text-destructive opacity-70" />
            <p className="text-sm mb-3">Не удалось загрузить</p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => refetchNotifications()}
            >
              <RefreshCw className="h-3 w-3 mr-1" />
              Повторить
            </Button>
          </div>
        ) : !notifications?.data || notifications.data.length === 0 ? (
          <div className="p-8 text-center text-muted-foreground">
            <Bell className="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">Нет уведомлений</p>
          </div>
        ) : (
          <ScrollArea className="h-[300px]">
            {notifications.data.map((notification) => (
              <DropdownMenuItem
                key={notification.id}
                className={cn(
                  'flex items-start gap-3 p-3 cursor-pointer',
                  !notification.isRead && 'bg-accent/50'
                )}
                onClick={() => handleNotificationClick(notification)}
              >
                <div className="mt-0.5">
                  <NotificationIcon type={notification.type} />
                </div>
                <div className="flex-1 space-y-1">
                  <p className={cn('text-sm', !notification.isRead && 'font-medium')}>
                    {notification.title}
                  </p>
                  <p className="text-xs text-muted-foreground line-clamp-2">
                    {notification.message}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatDistanceToNow(new Date(notification.createdAt), {
                      addSuffix: true,
                      locale: ru,
                    })}
                  </p>
                </div>
                {!notification.isRead && (
                  <div className="w-2 h-2 rounded-full bg-primary mt-1" />
                )}
              </DropdownMenuItem>
            ))}
          </ScrollArea>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
