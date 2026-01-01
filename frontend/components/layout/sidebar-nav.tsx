'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import {
  CalendarDays,
  Users,
  Settings,
  BarChart3,
  LayoutDashboard,
  Ticket,
  User,
  Building2,
  type LucideIcon,
} from 'lucide-react';
import { usePermissions } from '@/lib/hooks/use-permissions';
import { Button } from '@/components/ui/button';

interface NavItem {
  name: string;
  href: string;
  icon: LucideIcon;
}

// Навигация для организаторов
const organizerNavigation: NavItem[] = [
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

// Навигация для участников (личный кабинет)
const participantNavigation: NavItem[] = [
  {
    name: 'Мои билеты',
    href: '/dashboard/my-registrations',
    icon: Ticket,
  },
  {
    name: 'Профиль',
    href: '/dashboard/account/profile',
    icon: User,
  },
];

interface SidebarNavProps {
  className?: string;
}

export function SidebarNav({ className }: SidebarNavProps) {
  const pathname = usePathname();
  const { isOrganizer } = usePermissions();

  return (
    <nav className={cn('grid items-start gap-1', className)}>
      {/* Секция организатора */}
      {isOrganizer && (
        <>
          <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Организатор
          </div>
          {organizerNavigation.map((item) => (
            <NavLink key={item.href} item={item} pathname={pathname} />
          ))}
          <div className="my-2 border-t" />
        </>
      )}

      {/* Секция участника */}
      <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        Личный кабинет
      </div>
      {participantNavigation.map((item) => (
        <NavLink key={item.href} item={item} pathname={pathname} />
      ))}

      {/* CTA для создания организации */}
      {!isOrganizer && <CreateOrganizationCta />}
    </nav>
  );
}

function NavLink({ item, pathname }: { item: NavItem; pathname: string }) {
  const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
  return (
    <Link
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
}

function CreateOrganizationCta() {
  return (
    <div className="mt-4 rounded-lg border border-dashed p-3">
      <div className="mb-2 flex items-center gap-2">
        <Building2 className="h-4 w-4 text-primary" />
        <span className="text-sm font-medium">Создайте организацию</span>
      </div>
      <p className="mb-2 text-xs text-muted-foreground">
        Чтобы создавать события и управлять регистрациями
      </p>
      <Button asChild size="sm" variant="outline" className="w-full">
        <Link href="/dashboard/create-organization">Создать</Link>
      </Button>
    </div>
  );
}
