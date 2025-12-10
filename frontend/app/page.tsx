import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="text-center">
        <h1 className="mb-4 text-4xl font-bold">AqStream</h1>
        <p className="mb-8 text-xl text-muted-foreground">Платформа для управления мероприятиями</p>

        <div className="flex justify-center gap-4">
          <Button size="lg" asChild>
            <Link href="/login">Войти</Link>
          </Button>
          <Button size="lg" variant="outline" asChild>
            <Link href="/register">Регистрация</Link>
          </Button>
        </div>
      </div>
    </main>
  );
}
