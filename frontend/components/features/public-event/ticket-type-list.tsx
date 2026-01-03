'use client';

import { TicketTypeCard } from './ticket-type-card';
import type { TicketType } from '@/lib/api/types';

interface TicketTypeListProps {
  ticketTypes: TicketType[];
}

/**
 * Read-only список доступных типов билетов.
 * Показывается неавторизованным пользователям.
 */
export function TicketTypeList({ ticketTypes }: TicketTypeListProps) {
  const activeTicketTypes = [...ticketTypes]
    .filter((tt) => tt.isActive)
    .sort((a, b) => a.sortOrder - b.sortOrder);

  if (activeTicketTypes.length === 0) {
    return (
      <div className="text-center py-4 text-muted-foreground" data-testid="no-tickets-message">
        Нет доступных типов билетов
      </div>
    );
  }

  return (
    <div className="space-y-3" data-testid="ticket-type-list">
      {activeTicketTypes.map((ticketType) => (
        <TicketTypeCard
          key={ticketType.id}
          ticketType={ticketType}
          isSelected={false}
          disabled={true}
        />
      ))}
    </div>
  );
}
