'use client';

import Link from 'next/link';
import { QRCodeSVG } from 'qrcode.react';
import { XCircle } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';

import { formatEventDate } from '@/lib/utils/date-time';
import {
  getStatusBadgeVariant,
  getStatusLabel,
  canCancelRegistration,
} from '@/lib/utils/registration';
import type { Registration } from '@/lib/api/types';

interface TicketCardProps {
  registration: Registration;
  onCancel?: (id: string) => void;
  isCancelling?: boolean;
}

/**
 * Карточка билета для мобильного отображения.
 * Показывает QR-код, информацию о событии и статус регистрации.
 */
export function TicketCard({ registration, onCancel, isCancelling }: TicketCardProps) {
  const canCancel = canCancelRegistration(registration.status);

  return (
    <Card className="flex flex-col">
      <CardHeader className="flex flex-row items-start justify-between gap-4 pb-2">
        <div className="flex-1 min-w-0">
          <CardTitle className="text-base truncate">
            <Link
              href={`/events/${registration.eventSlug}`}
              className="hover:underline"
            >
              {registration.eventTitle}
            </Link>
          </CardTitle>
          <p className="text-sm text-muted-foreground mt-1">
            {formatEventDate(registration.eventStartsAt)}
          </p>
        </div>
        <Badge variant={getStatusBadgeVariant(registration.status)}>
          {getStatusLabel(registration.status)}
        </Badge>
      </CardHeader>

      <CardContent className="flex gap-4 pb-2">
        <div className="shrink-0">
          <QRCodeSVG
            value={registration.confirmationCode}
            size={64}
            level="M"
            className="rounded"
          />
        </div>
        <div className="flex-1 min-w-0 space-y-1">
          <p className="text-sm">
            <span className="text-muted-foreground">Тип билета:</span>{' '}
            <span className="font-medium">{registration.ticketTypeName}</span>
          </p>
          <p className="text-sm">
            <span className="text-muted-foreground">Код:</span>{' '}
            <code className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded">
              {registration.confirmationCode}
            </code>
          </p>
        </div>
      </CardContent>

      <CardFooter className="flex gap-2 pt-2">
        <Button variant="outline" size="sm" className="flex-1" asChild>
          <Link href={`/dashboard/my-registrations/${registration.id}`}>
            Подробнее
          </Link>
        </Button>
        {canCancel && onCancel && (
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="text-destructive hover:text-destructive"
                disabled={isCancelling}
                aria-label="Отменить регистрацию"
              >
                <XCircle className="h-4 w-4" />
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Отменить регистрацию?</AlertDialogTitle>
                <AlertDialogDescription>
                  Вы уверены, что хотите отменить регистрацию на событие &quot;
                  {registration.eventTitle}&quot;? Это действие нельзя отменить.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Нет, оставить</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => onCancel(registration.id)}
                  className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                >
                  Да, отменить
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        )}
      </CardFooter>
    </Card>
  );
}
