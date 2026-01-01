import { TelegramLink } from '@/components/features/account/telegram-link';

export default function TelegramPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Telegram</h1>
        <p className="text-muted-foreground">
          Управление привязкой Telegram аккаунта
        </p>
      </div>

      <TelegramLink />
    </div>
  );
}
