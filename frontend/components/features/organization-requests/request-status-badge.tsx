'use client';

import { Badge } from '@/components/ui/badge';
import type { OrganizationRequestStatus } from '@/lib/api/types';
import { cn } from '@/lib/utils';

interface RequestStatusBadgeProps {
  status: OrganizationRequestStatus;
  className?: string;
}

const statusConfig: Record<
  OrganizationRequestStatus,
  { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }
> = {
  PENDING: { label: 'На рассмотрении', variant: 'secondary' },
  APPROVED: { label: 'Одобрено', variant: 'default' },
  REJECTED: { label: 'Отклонено', variant: 'destructive' },
};

export function RequestStatusBadge({ status, className }: RequestStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <Badge
      variant={config.variant}
      className={cn(
        // Дополнительные стили для APPROVED (зелёный)
        status === 'APPROVED' && 'bg-green-500 hover:bg-green-600',
        // Дополнительные стили для PENDING (жёлтый)
        status === 'PENDING' && 'bg-yellow-500 hover:bg-yellow-600 text-white',
        className
      )}
      data-testid={`request-status-${status.toLowerCase()}`}
    >
      {config.label}
    </Badge>
  );
}

export function getRequestStatusLabel(status: OrganizationRequestStatus): string {
  return statusConfig[status]?.label ?? status;
}
