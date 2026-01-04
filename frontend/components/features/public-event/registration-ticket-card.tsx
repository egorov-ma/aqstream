'use client';

import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { CheckCircle, Ticket, MessageSquare, X, ArrowRight } from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';

import type { Registration, Event } from '@/lib/api/types';
import { useCancelRegistration, useResendTicket } from '@/lib/hooks/use-registrations';
// TODO: Restore when add-to-calendar-button is implemented
// import { AddToCalendarButton } from './add-to-calendar-button';
import { ROUTES } from '@/lib/routes';

interface RegistrationTicketCardProps {
  registration: Registration;
  event: Event;
}

/**
 * Компонент карточки билета для зарегистрированного пользователя.
 * Отображается вместо формы регистрации, если пользователь уже зарегистрирован на событие.
 */
export function RegistrationTicketCard({ registration, event }: RegistrationTicketCardProps) {
  const queryClient = useQueryClient();
  const [showCancel, setShowCancel] = useState(false);
  const cancelMutation = useCancelRegistration();
  const resendMutation = useResendTicket();

  // Обработчик отмены регистрации
  const handleCancel = async () => {
    try {
      await cancelMutation.mutateAsync(registration.id);
      toast.success('Регистрация отменена');
      // Invalidate queries для обновления данных
      await queryClient.invalidateQueries({ queryKey: ['event', event.slug] });
      await queryClient.invalidateQueries({ queryKey: ['registrations'] });
    } catch {
      toast.error('Ошибка при отмене регистрации');
    }
  };

  // Обработчик повторной отправки билета
  const handleResend = async () => {
    try {
      await resendMutation.mutateAsync(registration.id);
    } catch {
      // Toast уже показан в hook
    }
  };

  return (
    <Card data-testid="registration-ticket-card">
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <CheckCircle className="h-5 w-5 text-green-500" />
              Вы зарегистрированы
            </CardTitle>
          </div>
          <Badge data-testid={`registration-status-${registration.status.toLowerCase()}`}>
            {registration.status === 'CONFIRMED' ? 'Подтверждено' : registration.status}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Confirmation Code */}
        <div className="bg-muted p-4 rounded-lg text-center">
          <p className="text-xs text-muted-foreground mb-1">Код билета</p>
          <p
            className="text-2xl font-mono font-bold tracking-wider"
            data-testid="ticket-confirmation-code"
          >
            {registration.confirmationCode}
          </p>
        </div>

        {/* Ticket Type */}
        <div className="flex items-center gap-2 text-sm">
          <Ticket className="h-4 w-4 text-muted-foreground" />
          <span className="font-medium" data-testid="ticket-type-name">
            {registration.ticketTypeName}
          </span>
        </div>

        <Separator />

        {/* Actions */}
        <div className="space-y-2">
          {/* TODO: Restore when add-to-calendar-button is implemented */}
          {/* <AddToCalendarButton event={event} /> */}

          <Button
            variant="outline"
            className="w-full"
            onClick={handleResend}
            disabled={resendMutation.isPending}
            data-testid="resend-ticket-button"
          >
            <MessageSquare className="mr-2 h-4 w-4" />
            Отправить в Telegram
          </Button>

          <Button
            variant="ghost"
            className="w-full"
            asChild
            data-testid="my-registrations-link"
          >
            <Link href={ROUTES.MY_REGISTRATIONS}>
              Все мои билеты
              <ArrowRight className="ml-2 h-4 w-4" />
            </Link>
          </Button>

          {/* Cancel Button */}
          {!showCancel ? (
            <Button
              variant="ghost"
              className="w-full text-destructive hover:text-destructive"
              onClick={() => setShowCancel(true)}
              data-testid="cancel-registration-show-button"
            >
              Отменить регистрацию
            </Button>
          ) : (
            <div className="space-y-2 pt-2 border-t">
              <p className="text-sm text-muted-foreground">
                Вы уверены? Место станет доступно для других участников.
              </p>
              <div className="flex gap-2">
                <Button
                  variant="destructive"
                  className="flex-1"
                  onClick={handleCancel}
                  disabled={cancelMutation.isPending}
                  data-testid="cancel-registration-confirm-button"
                >
                  <X className="mr-2 h-4 w-4" />
                  Да, отменить
                </Button>
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => setShowCancel(false)}
                  data-testid="cancel-registration-abort-button"
                >
                  Нет
                </Button>
              </div>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
