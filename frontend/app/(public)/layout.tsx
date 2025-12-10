import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      {/* Публичный хедер */}
      <header className="border-b">
        <div className="container flex h-14 items-center justify-between">
          <Link href="/" className="font-bold">
            AqStream
          </Link>
          <nav className="flex items-center gap-4">
            <Button variant="ghost" asChild>
              <Link href="/login">Войти</Link>
            </Button>
            <Button asChild>
              <Link href="/register">Регистрация</Link>
            </Button>
          </nav>
        </div>
      </header>

      {/* Контент */}
      <main className="flex-1">{children}</main>

      {/* Футер */}
      <footer className="border-t py-6">
        <div className="container text-center text-sm text-muted-foreground">
          AqStream — платформа для управления мероприятиями
        </div>
      </footer>
    </div>
  );
}
