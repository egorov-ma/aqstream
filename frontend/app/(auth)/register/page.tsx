import Link from 'next/link';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Регистрация - AqStream',
  description: 'Создайте аккаунт AqStream',
};

export default function RegisterPage() {
  return (
    <div className="rounded-lg border bg-card p-8 shadow-sm">
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold">Регистрация</h1>
        <p className="text-sm text-muted-foreground">Создайте новый аккаунт</p>
      </div>

      {/* Placeholder для формы регистрации — будет реализовано в Phase 2 */}
      <div className="space-y-4">
        <div>
          <label htmlFor="name" className="mb-2 block text-sm font-medium">
            Имя
          </label>
          <input
            id="name"
            type="text"
            placeholder="Ваше имя"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            disabled
          />
        </div>

        <div>
          <label htmlFor="email" className="mb-2 block text-sm font-medium">
            Email
          </label>
          <input
            id="email"
            type="email"
            placeholder="email@example.com"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            disabled
          />
        </div>

        <div>
          <label htmlFor="password" className="mb-2 block text-sm font-medium">
            Пароль
          </label>
          <input
            id="password"
            type="password"
            placeholder="••••••••"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            disabled
          />
        </div>

        <button
          type="button"
          className="w-full rounded-md bg-primary px-4 py-2 text-primary-foreground opacity-50"
          disabled
        >
          Зарегистрироваться
        </button>
      </div>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Уже есть аккаунт?{' '}
        <Link href="/login" className="text-primary hover:underline">
          Войти
        </Link>
      </p>
    </div>
  );
}
