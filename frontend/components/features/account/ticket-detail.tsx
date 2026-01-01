'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { QRCodeSVG } from 'qrcode.react';
import { ArrowLeft, Calendar, Copy, ExternalLink, Send, Share2, XCircle } from 'lucide-react';
import { toast } from 'sonner';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
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
import { generateIcsContent, downloadFile } from '@/lib/utils/calendar';
import {
  getStatusBadgeVariant,
  getStatusLabel,
  canCancelRegistration,
} from '@/lib/utils/registration';
import type { Registration } from '@/lib/api/types';

interface TicketDetailProps {
  registration: Registration;
  onCancel: (id: string) => void;
  isCancelling: boolean;
  onResendTicket: (id: string) => void;
  isResending: boolean;
}

/**
 * Детальная страница билета.
 * Показывает крупный QR-код, информацию о регистрации и действия.
 */
export function TicketDetail({
  registration,
  onCancel,
  isCancelling,
  onResendTicket,
  isResending,
}: TicketDetailProps) {
  const router = useRouter();
  const canCancel = canCancelRegistration(registration.status);
  // Можно отправить билет только для активных регистраций
  const canResend = registration.status === 'CONFIRMED' || registration.status === 'CHECKED_IN';

  // Копировать код подтверждения
  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(registration.confirmationCode);
      toast.success('Код скопирован');
    } catch {
      toast.error('Не удалось скопировать код');
    }
  };

  // Добавить в календарь
  const handleAddToCalendar = () => {
    const icsContent = generateIcsContent({
      id: registration.id,
      title: registration.eventTitle,
      startsAt: registration.eventStartsAt,
      description: `Билет: ${registration.ticketTypeName}\nКод: ${registration.confirmationCode}`,
    });

    downloadFile(icsContent, `event-${registration.eventSlug}.ics`, 'text/calendar');
    toast.success('Файл календаря загружен');
  };

  // Поделиться
  const handleShare = async () => {
    // Используем абсолютный URL для consistency
    const baseUrl = typeof window !== 'undefined' ? window.location.origin : '';
    const shareUrl = `${baseUrl}/dashboard/my-registrations/${registration.id}`;
    const shareData = {
      title: `Билет: ${registration.eventTitle}`,
      text: `Мой билет на ${registration.eventTitle}`,
      url: shareUrl,
    };

    try {
      if (navigator.share && navigator.canShare(shareData)) {
        await navigator.share(shareData);
      } else {
        // Fallback: копируем URL
        await navigator.clipboard.writeText(shareUrl);
        toast.success('Ссылка скопирована');
      }
    } catch (error) {
      // Пользователь отменил шаринг или ошибка
      if (error instanceof Error && error.name !== 'AbortError') {
        toast.error('Не удалось поделиться');
      }
    }
  };

  // Обработчик отмены с навигацией
  const handleCancel = () => {
    onCancel(registration.id);
    router.push('/dashboard/my-registrations');
  };

  return (
    <div className="space-y-6">
      {/* Навигация */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild aria-label="Назад к билетам">
          <Link href="/dashboard/my-registrations">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold">{registration.eventTitle}</h1>
          <p className="text-muted-foreground">
            {formatEventDate(registration.eventStartsAt)}
          </p>
        </div>
        <Badge variant={getStatusBadgeVariant(registration.status)} className="ml-auto">
          {getStatusLabel(registration.status)}
        </Badge>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* QR-код */}
        <Card>
          <CardHeader className="text-center">
            <CardTitle>QR-код билета</CardTitle>
            <CardDescription>
              Покажите этот код при входе на мероприятие
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col items-center gap-4">
            <div className="p-4 bg-white rounded-lg">
              <QRCodeSVG
                value={registration.confirmationCode}
                size={256}
                level="H"
                includeMargin
              />
            </div>
            <div className="flex items-center gap-2">
              <code className="text-2xl font-mono font-bold tracking-widest">
                {registration.confirmationCode}
              </code>
              <Button variant="ghost" size="icon" onClick={handleCopyCode} aria-label="Скопировать код">
                <Copy className="h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Информация о билете */}
        <Card>
          <CardHeader>
            <CardTitle>Информация о билете</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <p className="text-sm text-muted-foreground">Событие</p>
              <p className="font-medium">{registration.eventTitle}</p>
            </div>

            <Separator />

            <div>
              <p className="text-sm text-muted-foreground">Дата и время</p>
              <p className="font-medium">{formatEventDate(registration.eventStartsAt)}</p>
            </div>

            <Separator />

            <div>
              <p className="text-sm text-muted-foreground">Тип билета</p>
              <p className="font-medium">{registration.ticketTypeName}</p>
            </div>

            <Separator />

            <div>
              <p className="text-sm text-muted-foreground">Участник</p>
              <p className="font-medium">
                {registration.firstName} {registration.lastName}
              </p>
              <p className="text-sm text-muted-foreground">{registration.email}</p>
            </div>

            <Separator />

            {/* Действия */}
            <div className="space-y-2">
              {canResend && (
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => onResendTicket(registration.id)}
                  disabled={isResending}
                >
                  <Send className="mr-2 h-4 w-4" />
                  {isResending ? 'Отправка...' : 'Отправить в Telegram'}
                </Button>
              )}

              <Button variant="outline" className="w-full" onClick={handleAddToCalendar}>
                <Calendar className="mr-2 h-4 w-4" />
                Добавить в календарь
              </Button>

              <Button variant="outline" className="w-full" onClick={handleShare}>
                <Share2 className="mr-2 h-4 w-4" />
                Поделиться
              </Button>

              <Button variant="outline" className="w-full" asChild>
                <Link href={`/events/${registration.eventSlug}`}>
                  <ExternalLink className="mr-2 h-4 w-4" />
                  Страница события
                </Link>
              </Button>

              {canCancel && (
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <Button
                      variant="destructive"
                      className="w-full"
                      disabled={isCancelling}
                    >
                      <XCircle className="mr-2 h-4 w-4" />
                      Отменить регистрацию
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
                        onClick={handleCancel}
                        className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                      >
                        Да, отменить
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
