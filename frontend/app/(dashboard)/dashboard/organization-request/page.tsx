'use client';

import { Building2 } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { CreateRequestForm, MyRequestsList } from '@/components/features/organization-requests';

export default function OrganizationRequestPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold">Заявка на организацию</h1>
        <p className="text-muted-foreground">
          Подайте заявку на создание организации для проведения мероприятий
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Building2 className="h-5 w-5" />
              Новая заявка
            </CardTitle>
            <CardDescription>
              Заполните форму для подачи заявки на создание организации
            </CardDescription>
          </CardHeader>
          <CardContent>
            <CreateRequestForm />
          </CardContent>
        </Card>

        <div className="flex flex-col gap-4">
          <h2 className="text-lg font-semibold">Мои заявки</h2>
          <MyRequestsList />
        </div>
      </div>
    </div>
  );
}
