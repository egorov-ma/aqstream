'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet';
import { SidebarNav } from './sidebar-nav';
import { OrganizationSwitcher } from './organization-switcher';

export function MobileNav() {
  const [open, setOpen] = useState(false);

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="lg:hidden"
          data-testid="mobile-nav-trigger"
        >
          <Menu className="h-5 w-5" />
          <span className="sr-only">Открыть меню</span>
        </Button>
      </SheetTrigger>
      <SheetContent side="left" className="w-72 p-0">
        <div className="flex h-14 items-center border-b px-6">
          <Link href="/" className="flex items-center gap-2 font-semibold">
            <span>AqStream</span>
          </Link>
        </div>

        {/* OrganizationSwitcher */}
        <div className="border-b px-4 py-3" data-testid="mobile-nav-organization-switcher">
          <OrganizationSwitcher onSwitch={() => setOpen(false)} />
        </div>

        {/* Навигация */}
        <div className="px-4 py-4">
          <SidebarNav />
        </div>
      </SheetContent>
    </Sheet>
  );
}
