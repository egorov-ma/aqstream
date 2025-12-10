import type { Metadata } from 'next';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { BarChart3 } from 'lucide-react';

export const metadata: Metadata = {
  title: 'Аналитика - AqStream',
  description: 'Аналитика мероприятий',
};

export default function AnalyticsPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Аналитика</h1>

      <Card>
        <CardHeader>
          <CardTitle>Статистика мероприятий</CardTitle>
          <CardDescription>Анализ посещаемости и регистраций</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-12">
            <BarChart3 className="h-12 w-12 text-muted-foreground" />
            <p className="mt-4 text-muted-foreground">
              Аналитика будет доступна после реализации в Phase 2.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
