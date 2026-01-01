'use client';

import { Bell, Calendar, Users, Newspaper } from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Skeleton } from '@/components/ui/skeleton';

import {
  useNotificationPreferences,
  useUpdateNotificationPreferences,
} from '@/lib/hooks/use-notifications';
import type { NotificationSettings } from '@/lib/api/notifications';

/**
 * Компонент настроек уведомлений.
 */
export function NotificationSettingsForm() {
  const { data: prefs, isLoading } = useNotificationPreferences();
  const updatePrefs = useUpdateNotificationPreferences();

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-4 w-64" />
        </CardHeader>
        <CardContent className="space-y-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center justify-between">
              <Skeleton className="h-4 w-40" />
              <Skeleton className="h-6 w-10" />
            </div>
          ))}
        </CardContent>
      </Card>
    );
  }

  const settings = prefs?.settings || {
    eventReminders: true,
    registrationUpdates: true,
    organizationNews: true,
  };

  const handleChange = (key: keyof NotificationSettings, value: boolean) => {
    updatePrefs.mutate({
      settings: {
        ...settings,
        [key]: value,
      },
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bell className="h-5 w-5" />
          Настройки уведомлений
        </CardTitle>
        <CardDescription>
          Управляйте типами уведомлений, которые вы хотите получать
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Напоминания о событиях */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Calendar className="h-5 w-5 text-muted-foreground" />
            <div>
              <Label htmlFor="eventReminders" className="font-medium">
                Напоминания о событиях
              </Label>
              <p className="text-sm text-muted-foreground">
                Напоминания за день и за час до начала
              </p>
            </div>
          </div>
          <Switch
            id="eventReminders"
            checked={settings.eventReminders}
            onCheckedChange={(checked) => handleChange('eventReminders', checked)}
            disabled={updatePrefs.isPending}
          />
        </div>

        {/* Обновления регистраций */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Users className="h-5 w-5 text-muted-foreground" />
            <div>
              <Label htmlFor="registrationUpdates" className="font-medium">
                Обновления регистраций
              </Label>
              <p className="text-sm text-muted-foreground">
                Подтверждения, изменения и отмены
              </p>
            </div>
          </div>
          <Switch
            id="registrationUpdates"
            checked={settings.registrationUpdates}
            onCheckedChange={(checked) => handleChange('registrationUpdates', checked)}
            disabled={updatePrefs.isPending}
          />
        </div>

        {/* Новости организаций */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Newspaper className="h-5 w-5 text-muted-foreground" />
            <div>
              <Label htmlFor="organizationNews" className="font-medium">
                Новости организаций
              </Label>
              <p className="text-sm text-muted-foreground">
                Новые события и анонсы от организаторов
              </p>
            </div>
          </div>
          <Switch
            id="organizationNews"
            checked={settings.organizationNews}
            onCheckedChange={(checked) => handleChange('organizationNews', checked)}
            disabled={updatePrefs.isPending}
          />
        </div>
      </CardContent>
    </Card>
  );
}
