import Link from 'next/link';
import { SidebarNav } from './sidebar-nav';

export function Sidebar() {
  return (
    <div className="hidden border-r bg-muted/40 lg:block">
      <div className="flex h-full max-h-screen flex-col gap-2">
        <div className="flex h-14 items-center border-b px-4 lg:h-[60px] lg:px-6">
          <Link href="/" className="flex items-center gap-2 font-semibold">
            <span>AqStream</span>
          </Link>
        </div>
        <div className="flex-1 px-2 lg:px-4">
          <SidebarNav className="py-2" />
        </div>
      </div>
    </div>
  );
}
