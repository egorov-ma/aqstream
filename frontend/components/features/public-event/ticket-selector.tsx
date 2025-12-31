'use client';

import { TicketTypeCard } from './ticket-type-card';
import type { TicketType } from '@/lib/api/types';

interface TicketSelectorProps {
  ticketTypes: TicketType[];
  selectedId?: string;
  onSelect: (ticketTypeId: string) => void;
  disabled?: boolean;
}

/**
 * Компонент выбора типа билета
 * Отображает список карточек типов билетов как RadioGroup
 */
export function TicketSelector({
  ticketTypes,
  selectedId,
  onSelect,
  disabled = false,
}: TicketSelectorProps) {
  // Сортируем по sortOrder
  const sortedTicketTypes = [...ticketTypes].sort((a, b) => a.sortOrder - b.sortOrder);

  // Фильтруем только активные типы билетов
  const activeTicketTypes = sortedTicketTypes.filter((tt) => tt.isActive);

  if (activeTicketTypes.length === 0) {
    return (
      <div className="text-center py-4 text-muted-foreground" data-testid="no-tickets-message">
        Нет доступных типов билетов
      </div>
    );
  }

  return (
    <div className="space-y-3" data-testid="ticket-selector">
      {activeTicketTypes.map((ticketType) => (
        <TicketTypeCard
          key={ticketType.id}
          ticketType={ticketType}
          isSelected={selectedId === ticketType.id}
          onSelect={() => onSelect(ticketType.id)}
          disabled={disabled}
        />
      ))}
    </div>
  );
}
