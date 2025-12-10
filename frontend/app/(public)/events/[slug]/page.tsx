import type { Metadata } from 'next';
import { notFound } from 'next/navigation';

interface EventPageProps {
  params: Promise<{
    slug: string;
  }>;
}

export async function generateMetadata({ params }: EventPageProps): Promise<Metadata> {
  const { slug } = await params;

  // В будущем здесь будет загрузка данных события по slug
  return {
    title: `${slug} - AqStream`,
    description: 'Страница мероприятия',
  };
}

export default async function EventPage({ params }: EventPageProps) {
  const { slug } = await params;

  // Placeholder — в будущем здесь будет загрузка данных события
  // Если событие не найдено, вызываем notFound()
  if (!slug) {
    notFound();
  }

  return (
    <div className="container py-12">
      {/* Placeholder для страницы события — будет реализовано в Phase 2 */}
      <div className="mx-auto max-w-3xl">
        <div className="mb-8 rounded-lg border bg-card p-8">
          <p className="mb-2 text-sm text-muted-foreground">Событие</p>
          <h1 className="mb-4 text-3xl font-bold">{slug}</h1>
          <p className="text-muted-foreground">
            Информация о мероприятии будет отображаться здесь.
          </p>
        </div>

        <div className="rounded-lg border bg-card p-8 text-center">
          <h2 className="mb-4 text-xl font-semibold">Регистрация</h2>
          <p className="mb-6 text-muted-foreground">Форма регистрации будет доступна здесь.</p>
          <button
            type="button"
            className="rounded-md bg-primary px-6 py-3 text-primary-foreground opacity-50"
            disabled
          >
            Зарегистрироваться
          </button>
        </div>
      </div>
    </div>
  );
}
