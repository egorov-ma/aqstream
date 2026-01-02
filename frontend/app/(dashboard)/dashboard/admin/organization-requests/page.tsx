'use client';

import { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { AdminRequestsList } from '@/components/features/organization-requests';

export default function AdminOrganizationRequestsPage() {
  const [page, setPage] = useState(0);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <ShieldCheck className="h-6 w-6" />
          Заявки на организации
        </h1>
        <p className="text-muted-foreground">
          Управление заявками на создание организаций
        </p>
      </div>

      <AdminRequestsList page={page} onPageChange={setPage} />
    </div>
  );
}
