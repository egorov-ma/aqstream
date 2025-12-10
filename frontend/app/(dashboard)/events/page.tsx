import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Мероприятия - AqStream',
  description: 'Управление мероприятиями',
};

export default function EventsPage() {
  return (
    <div>
      <div className="mb-8 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Мероприятия</h1>
        <button
          type="button"
          className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground opacity-50"
          disabled
        >
          Создать мероприятие
        </button>
      </div>

      {/* Placeholder для списка мероприятий — будет реализовано в Phase 2 */}
      <div className="rounded-lg border bg-card p-8 text-center">
        <p className="text-muted-foreground">Список мероприятий будет отображаться здесь.</p>
        <p className="mt-2 text-sm text-muted-foreground">Функционал будет реализован в Phase 2.</p>
      </div>
    </div>
  );
}
