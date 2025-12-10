import Link from 'next/link';

export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="text-center">
        <h1 className="mb-4 text-4xl font-bold">AqStream</h1>
        <p className="mb-8 text-xl text-muted-foreground">Платформа для управления мероприятиями</p>

        <div className="flex justify-center gap-4">
          <Link
            href="/login"
            className="rounded-md bg-primary px-6 py-3 text-primary-foreground hover:bg-primary/90"
          >
            Войти
          </Link>
          <Link
            href="/register"
            className="rounded-md border border-input px-6 py-3 hover:bg-accent hover:text-accent-foreground"
          >
            Регистрация
          </Link>
        </div>
      </div>
    </main>
  );
}
