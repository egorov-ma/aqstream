import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="text-center">
        <h1 className="mb-2 text-6xl font-bold">404</h1>
        <h2 className="mb-4 text-2xl font-semibold">Страница не найдена</h2>
        <p className="mb-8 text-muted-foreground">
          Запрошенная страница не существует или была удалена.
        </p>
        <Button size="lg" asChild>
          <Link href="/">На главную</Link>
        </Button>
      </div>
    </main>
  );
}
