import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Настройки - AqStream',
  description: 'Настройки аккаунта',
};

export default function SettingsPage() {
  return (
    <div>
      <h1 className="mb-8 text-2xl font-bold">Настройки</h1>

      {/* Placeholder для настроек — будет реализовано в Phase 2 */}
      <div className="space-y-6">
        <section className="rounded-lg border bg-card p-6">
          <h2 className="mb-4 text-lg font-semibold">Профиль</h2>
          <p className="text-muted-foreground">Настройки профиля будут доступны здесь.</p>
        </section>

        <section className="rounded-lg border bg-card p-6">
          <h2 className="mb-4 text-lg font-semibold">Организация</h2>
          <p className="text-muted-foreground">Управление организацией будет доступно здесь.</p>
        </section>

        <section className="rounded-lg border bg-card p-6">
          <h2 className="mb-4 text-lg font-semibold">Уведомления</h2>
          <p className="text-muted-foreground">Настройки уведомлений будут доступны здесь.</p>
        </section>
      </div>
    </div>
  );
}
