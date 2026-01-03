'use client';

import Link from 'next/link';
import { Bell, Building2, LogOut, User } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useAuthStore } from '@/lib/store/auth-store';
import { useLogout } from '@/lib/hooks/use-auth';

export function UserNav() {
  const { user } = useAuthStore();
  const logout = useLogout();

  // Формируем отображаемое имя
  const displayName = user
    ? [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email
    : 'Пользователь';

  // Получаем инициалы для аватара
  const initials = user
    ? (user.firstName?.charAt(0) || '') + (user.lastName?.charAt(0) || '')
    : 'U';

  const handleLogout = () => {
    logout.mutate();
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="relative h-8 w-8 rounded-full"
          data-testid="user-nav-trigger"
        >
          <Avatar className="h-8 w-8">
            <AvatarImage src={user?.avatarUrl || ''} alt={displayName} />
            <AvatarFallback>{initials.toUpperCase() || 'U'}</AvatarFallback>
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-56" align="end" forceMount>
        <DropdownMenuLabel className="font-normal">
          <div className="flex flex-col space-y-1">
            <p className="text-sm font-medium leading-none">{displayName}</p>
            <p className="text-xs leading-none text-muted-foreground">
              {user?.email || 'Email не указан'}
            </p>
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link
            href="/dashboard/account/profile"
            className="flex items-center"
            data-testid="user-nav-profile-link"
          >
            <User className="mr-2 h-4 w-4" />
            Профиль
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link
            href="/dashboard/account/organizations"
            className="flex items-center"
            data-testid="user-nav-organizations-link"
          >
            <Building2 className="mr-2 h-4 w-4" />
            Организации
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link
            href="/dashboard/account/notifications"
            className="flex items-center"
            data-testid="user-nav-notifications-link"
          >
            <Bell className="mr-2 h-4 w-4" />
            Уведомления
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          className="text-destructive focus:text-destructive cursor-pointer"
          onClick={handleLogout}
          disabled={logout.isPending}
          data-testid="user-nav-logout-button"
        >
          <LogOut className="mr-2 h-4 w-4" />
          {logout.isPending ? 'Выход...' : 'Выйти'}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
