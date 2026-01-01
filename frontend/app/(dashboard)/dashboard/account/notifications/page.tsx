import { NotificationSettingsForm } from '@/components/features/account/notification-settings';

export default function NotificationsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Уведомления</h1>
        <p className="text-muted-foreground">
          Настройте типы уведомлений, которые вы хотите получать
        </p>
      </div>

      <NotificationSettingsForm />
    </div>
  );
}
