'use client';

import Link from 'next/link';
import { Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetTrigger, SheetClose } from '@/components/ui/sheet';

/**
 * Hamburger меню для public header на mobile.
 * Показывает навигационные ссылки (События, Мои билеты, Личный кабинет)
 * для авторизованных пользователей.
 */
export function PublicMobileNav() {
  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden"
          data-testid="public-mobile-nav-trigger"
        >
          <Menu className="h-5 w-5" />
          <span className="sr-only">Открыть меню</span>
        </Button>
      </SheetTrigger>
      <SheetContent side="left" className="w-72">
        <div className="flex flex-col gap-4 py-4">
          <SheetClose asChild>
            <Link
              href="/events"
              className="text-lg font-medium hover:text-primary"
              data-testid="public-mobile-nav-events"
            >
              События
            </Link>
          </SheetClose>
          <SheetClose asChild>
            <Link
              href="/dashboard/my-registrations"
              className="text-lg font-medium hover:text-primary"
              data-testid="public-mobile-nav-tickets"
            >
              Мои билеты
            </Link>
          </SheetClose>
          <SheetClose asChild>
            <Link
              href="/dashboard"
              className="text-lg font-medium hover:text-primary"
              data-testid="public-mobile-nav-personal-cabinet"
            >
              Личный кабинет
            </Link>
          </SheetClose>
        </div>
      </SheetContent>
    </Sheet>
  );
}
