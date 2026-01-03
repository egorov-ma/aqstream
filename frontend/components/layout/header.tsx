'use client';

import Link from 'next/link';
import { MobileNav } from './mobile-nav';
import { UserNav } from './user-nav';
import { OrganizationSwitcher } from './organization-switcher';
import { ThemeToggle } from './theme-toggle';
import { Notifications } from './notifications';

export function Header() {
  return (
    <header className="sticky top-0 z-50 flex h-14 items-center gap-4 border-b bg-background px-4 lg:h-[60px] lg:px-6">
      {/* Бургер слева на мобильных */}
      <MobileNav />

      {/* Название по центру на мобильных, организация на десктопе */}
      <div className="flex-1 flex items-center gap-4">
        {/* Название AqStream - центрировано на мобильных */}
        <Link
          href="/"
          className="flex items-center gap-2 font-semibold lg:hidden absolute left-1/2 -translate-x-1/2"
        >
          <span>AqStream</span>
        </Link>

        {/* Переключатель организаций на десктопе */}
        <div className="hidden md:block">
          <OrganizationSwitcher />
        </div>
      </div>

      {/* Меню справа */}
      <div className="flex items-center gap-2" data-testid="dashboard-header-menu">
        <Notifications />
        <ThemeToggle />
        <UserNav />
      </div>
    </header>
  );
}
