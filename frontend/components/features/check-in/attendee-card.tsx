'use client';

import { Check, User, Ticket, Clock, AlertCircle } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

interface AttendeeCardProps {
  registration: {
    id: string;
    confirmationCode: string;
    firstName: string;
    lastName: string | null;
    email: string;
    ticketTypeName: string;
    status: string;
    checkedInAt: string | null;
  };
  onCheckIn: (id: string) => void;
  isCheckingIn?: boolean;
  isOffline?: boolean;
}

/**
 * Карточка участника для check-in.
 */
export function AttendeeCard({
  registration,
  onCheckIn,
  isCheckingIn = false,
  isOffline = false,
}: AttendeeCardProps) {
  const isCheckedIn = registration.status === 'CHECKED_IN' || !!registration.checkedInAt;
  const fullName = [registration.firstName, registration.lastName].filter(Boolean).join(' ');

  return (
    <Card className={isCheckedIn ? 'border-green-500 bg-green-50 dark:bg-green-950/20' : ''}>
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <CardTitle className="flex items-center gap-2 text-lg">
            <User className="h-5 w-5" />
            {fullName}
          </CardTitle>
          {isCheckedIn ? (
            <Badge variant="default" className="bg-green-500">
              <Check className="mr-1 h-3 w-3" />
              Отмечен
            </Badge>
          ) : (
            <Badge variant="outline">Ожидает</Badge>
          )}
        </div>
      </CardHeader>

      <CardContent className="space-y-2">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Ticket className="h-4 w-4" />
          <span>{registration.ticketTypeName}</span>
        </div>

        <div className="text-sm text-muted-foreground">
          Код: <span className="font-mono font-bold">{registration.confirmationCode}</span>
        </div>

        <div className="text-sm text-muted-foreground">{registration.email}</div>

        {isCheckedIn && registration.checkedInAt && (
          <div className="flex items-center gap-2 text-sm text-green-600">
            <Clock className="h-4 w-4" />
            <span>
              Отмечен:{' '}
              {new Date(registration.checkedInAt).toLocaleTimeString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </span>
          </div>
        )}
      </CardContent>

      <CardFooter>
        {isCheckedIn ? (
          <div className="flex w-full items-center justify-center gap-2 text-green-600">
            <Check className="h-5 w-5" />
            <span>Участник уже отмечен</span>
          </div>
        ) : (
          <Button
            onClick={() => onCheckIn(registration.id)}
            disabled={isCheckingIn}
            className="w-full"
            size="lg"
          >
            {isCheckingIn ? (
              'Отмечаем...'
            ) : (
              <>
                <Check className="mr-2 h-5 w-5" />
                Отметить присутствие
                {isOffline && (
                  <Badge variant="secondary" className="ml-2">
                    <AlertCircle className="mr-1 h-3 w-3" />
                    Офлайн
                  </Badge>
                )}
              </>
            )}
          </Button>
        )}
      </CardFooter>
    </Card>
  );
}
