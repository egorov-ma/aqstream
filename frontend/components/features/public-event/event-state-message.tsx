'use client';

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AlertCircle, Calendar, CheckCircle2, XCircle } from 'lucide-react';
import type { Event } from '@/lib/api/types';

interface EventStateMessageProps {
  event: Event;
  isRegistrationClosed?: boolean;
  allSoldOut?: boolean;
}

/**
 * Компонент отображения состояния события
 * Показывает предупреждения для CANCELLED, COMPLETED, закрытой регистрации и распроданных билетов
 */
export function EventStateMessage({
  event,
  isRegistrationClosed = false,
  allSoldOut = false,
}: EventStateMessageProps) {
  // Событие отменено
  if (event.status === 'CANCELLED') {
    return (
      <Alert variant="destructive" data-testid="event-cancelled-alert">
        <XCircle className="h-4 w-4" />
        <AlertTitle>Событие отменено</AlertTitle>
        <AlertDescription>
          {event.cancelReason || 'К сожалению, это событие было отменено.'}
        </AlertDescription>
      </Alert>
    );
  }

  // Событие завершено
  if (event.status === 'COMPLETED') {
    return (
      <Alert data-testid="event-completed-alert">
        <CheckCircle2 className="h-4 w-4" />
        <AlertTitle>Событие завершено</AlertTitle>
        <AlertDescription>
          Это событие уже прошло. Спасибо всем участникам!
        </AlertDescription>
      </Alert>
    );
  }

  // Все билеты распроданы
  if (allSoldOut) {
    return (
      <Alert data-testid="tickets-soldout-alert">
        <AlertCircle className="h-4 w-4" />
        <AlertTitle>Билеты распроданы</AlertTitle>
        <AlertDescription>
          К сожалению, все билеты на это событие распроданы.
        </AlertDescription>
      </Alert>
    );
  }

  // Регистрация закрыта
  if (isRegistrationClosed) {
    return (
      <Alert data-testid="registration-closed-alert">
        <Calendar className="h-4 w-4" />
        <AlertTitle>Регистрация закрыта</AlertTitle>
        <AlertDescription>
          Регистрация на это событие завершена.
        </AlertDescription>
      </Alert>
    );
  }

  return null;
}
