import { PublicHeader } from '@/components/layout/public-header';

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      {/* Публичный хедер с условной навигацией */}
      <PublicHeader />

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
