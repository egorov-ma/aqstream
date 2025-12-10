import Link from 'next/link';

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="text-center">
        <h1 className="mb-2 text-6xl font-bold">404</h1>
        <h2 className="mb-4 text-2xl font-semibold">Страница не найдена</h2>
        <p className="mb-8 text-muted-foreground">
          Запрошенная страница не существует или была удалена.
        </p>
        <Link
          href="/"
          className="rounded-md bg-primary px-6 py-3 text-primary-foreground hover:bg-primary/90"
        >
          На главную
        </Link>
      </div>
    </main>
  );
}
