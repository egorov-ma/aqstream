'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { CalendarDays, Users, Settings, BarChart3, LayoutDashboard } from 'lucide-react';

const navigation = [
  {
    name: 'Обзор',
    href: '/dashboard',
    icon: LayoutDashboard,
  },
  {
    name: 'События',
    href: '/dashboard/events',
    icon: CalendarDays,
  },
  {
    name: 'Регистрации',
    href: '/dashboard/registrations',
    icon: Users,
  },
  {
    name: 'Аналитика',
    href: '/dashboard/analytics',
    icon: BarChart3,
  },
  {
    name: 'Настройки',
    href: '/dashboard/settings',
    icon: Settings,
  },
];

interface SidebarNavProps {
  className?: string;
}

export function SidebarNav({ className }: SidebarNavProps) {
  const pathname = usePathname();

  return (
    <nav className={cn('grid items-start gap-1', className)}>
      {navigation.map((item) => {
        const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
        return (
          <Link
            key={item.name}
            href={item.href}
            className={cn(
              'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all hover:bg-accent hover:text-accent-foreground',
              isActive ? 'bg-accent text-accent-foreground' : 'text-muted-foreground'
            )}
          >
            <item.icon className="h-4 w-4" />
            {item.name}
          </Link>
        );
      })}
    </nav>
  );
}
