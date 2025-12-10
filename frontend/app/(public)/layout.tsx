import Link from 'next/link';

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen">
      {/* Публичный хедер */}
      <header className="border-b">
        <div className="container flex h-16 items-center justify-between">
          <Link href="/" className="text-xl font-bold">
            AqStream
          </Link>
          <nav className="flex items-center gap-4">
            <Link href="/login" className="text-sm text-muted-foreground hover:text-foreground">
              Войти
            </Link>
            <Link
              href="/register"
              className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90"
            >
              Регистрация
            </Link>
          </nav>
        </div>
      </header>

      {/* Контент */}
      <main>{children}</main>

      {/* Футер */}
      <footer className="border-t py-8">
        <div className="container text-center text-sm text-muted-foreground">
          AqStream — платформа для управления мероприятиями
        </div>
      </footer>
    </div>
  );
}
