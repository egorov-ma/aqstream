import Link from 'next/link';
import { MobileNav } from './mobile-nav';
import { UserNav } from './user-nav';

export function Header() {
  return (
    <header className="sticky top-0 z-50 flex h-14 items-center gap-4 border-b bg-background px-4 lg:h-[60px] lg:px-6">
      <MobileNav />
      <div className="flex-1">
        <Link href="/" className="flex items-center gap-2 font-semibold lg:hidden">
          <span>AqStream</span>
        </Link>
      </div>
      <div className="flex items-center gap-4">
        <UserNav />
      </div>
    </header>
  );
}
