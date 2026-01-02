'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { TicketType } from '@/lib/api/types';

interface TicketTypeCardProps {
  ticketType: TicketType;
  isSelected?: boolean;
  onSelect?: () => void;
  disabled?: boolean;
}

/**
 * Форматирует цену в рубли
 */
function formatPrice(priceCents: number): string {
  if (priceCents === 0) {
    return 'Бесплатно';
  }
  const rubles = priceCents / 100;
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(rubles);
}

/**
 * Проверяет, началась ли продажа билетов
 */
function isSalesStarted(salesStart?: string): boolean {
  if (!salesStart) return true;
  return new Date(salesStart) <= new Date();
}

/**
 * Проверяет, заканчиваются ли билеты
 */
function isLowStock(ticketType: TicketType): boolean {
  if (ticketType.available === undefined) return false;
  return ticketType.available > 0 && ticketType.available < 10;
}

/**
 * Карточка типа билета с badges состояния
 */
export function TicketTypeCard({
  ticketType,
  isSelected = false,
  onSelect,
  disabled = false,
}: TicketTypeCardProps) {
  const salesStarted = isSalesStarted(ticketType.salesStart);
  const isFree = ticketType.priceCents === 0;
  const lowStock = isLowStock(ticketType);
  const isDisabled = disabled || ticketType.isSoldOut || !salesStarted || !ticketType.isActive;

  return (
    <Card
      className={cn(
        'cursor-pointer transition-all',
        isSelected && 'ring-2 ring-primary',
        isDisabled && 'opacity-50 cursor-not-allowed',
        !isDisabled && 'hover:shadow-md'
      )}
      onClick={() => {
        if (!isDisabled && onSelect) {
          onSelect();
        }
      }}
      data-testid="ticket-type-card"
      data-ticket-id={ticketType.id}
    >
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <CardTitle className="text-lg">{ticketType.name}</CardTitle>
          <div className="flex flex-wrap gap-1 justify-end">
            {isFree && (
              <Badge variant="secondary">Бесплатный</Badge>
            )}
            {ticketType.isSoldOut && (
              <Badge variant="destructive">Распродан</Badge>
            )}
            {!salesStarted && (
              <Badge variant="outline">Скоро</Badge>
            )}
            {lowStock && !ticketType.isSoldOut && (
              <Badge variant="outline" className="text-orange-600 border-orange-600">
                Заканчиваются
              </Badge>
            )}
          </div>
        </div>
        {ticketType.description && (
          <CardDescription>{ticketType.description}</CardDescription>
        )}
      </CardHeader>
      <CardContent>
        <div className="flex items-center justify-between">
          <span className="text-2xl font-bold">
            {formatPrice(ticketType.priceCents)}
          </span>
          {ticketType.available !== undefined && !ticketType.isSoldOut && (
            <span className="text-sm text-muted-foreground">
              Осталось: {ticketType.available}
            </span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
