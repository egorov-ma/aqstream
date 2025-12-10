import Link from 'next/link';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      {/* Сайдбар навигации */}
      <aside className="w-64 border-r bg-card">
        <div className="p-6">
          <Link href="/" className="text-xl font-bold">
            AqStream
          </Link>
        </div>

        <nav className="space-y-1 px-3">
          <Link
            href="/events"
            className="block rounded-md px-3 py-2 text-sm hover:bg-accent hover:text-accent-foreground"
          >
            Мероприятия
          </Link>
          <Link
            href="/settings"
            className="block rounded-md px-3 py-2 text-sm hover:bg-accent hover:text-accent-foreground"
          >
            Настройки
          </Link>
        </nav>
      </aside>

      {/* Основной контент */}
      <main className="flex-1 p-8">{children}</main>
    </div>
  );
}
