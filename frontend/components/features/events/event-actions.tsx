'use client';

import * as React from 'react';
import { useRouter } from 'next/navigation';
import {
  MoreHorizontal,
  Pencil,
  Eye,
  Send,
  XCircle,
  Trash2,
  EyeOff,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import type { Event } from '@/lib/api/types';
import {
  usePublishEvent,
  useCancelEvent,
  useDeleteEvent,
  useUnpublishEvent,
} from '@/lib/hooks/use-events';
import { canPublishEvent, canCancelEvent } from './event-status-badge';

interface EventActionsProps {
  event: Event;
}

type DialogType = 'cancel' | 'delete' | null;

export function EventActions({ event }: EventActionsProps) {
  const router = useRouter();
  const [dialogOpen, setDialogOpen] = React.useState<DialogType>(null);
  const [cancelReason, setCancelReason] = React.useState('');

  const publishEvent = usePublishEvent();
  const unpublishEvent = useUnpublishEvent();
  const cancelEvent = useCancelEvent();
  const deleteEvent = useDeleteEvent();

  const isLoading =
    publishEvent.isPending ||
    unpublishEvent.isPending ||
    cancelEvent.isPending ||
    deleteEvent.isPending;

  const handleView = () => {
    router.push(`/dashboard/events/${event.id}`);
  };

  const handleEdit = () => {
    router.push(`/dashboard/events/${event.id}/edit`);
  };

  const handlePublish = async () => {
    await publishEvent.mutateAsync(event.id);
  };

  const handleUnpublish = async () => {
    await unpublishEvent.mutateAsync(event.id);
  };

  const handleCancel = async () => {
    await cancelEvent.mutateAsync({
      id: event.id,
      reason: cancelReason.trim() || undefined,
    });
    setCancelReason('');
    setDialogOpen(null);
  };

  const handleDelete = async () => {
    await deleteEvent.mutateAsync(event.id);
    setDialogOpen(null);
  };

  const handleDialogClose = () => {
    setDialogOpen(null);
    setCancelReason('');
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            disabled={isLoading}
            data-testid={`event-actions-${event.id}`}
          >
            <MoreHorizontal className="h-4 w-4" />
            <span className="sr-only">Действия</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={handleView}>
            <Eye className="mr-2 h-4 w-4" />
            Просмотр
          </DropdownMenuItem>
          <DropdownMenuItem onClick={handleEdit}>
            <Pencil className="mr-2 h-4 w-4" />
            Редактировать
          </DropdownMenuItem>

          <DropdownMenuSeparator />

          {/* Публикация / Снятие с публикации */}
          {canPublishEvent(event.status) && (
            <DropdownMenuItem onClick={handlePublish}>
              <Send className="mr-2 h-4 w-4" />
              Опубликовать
            </DropdownMenuItem>
          )}
          {event.status === 'PUBLISHED' && (
            <DropdownMenuItem onClick={handleUnpublish}>
              <EyeOff className="mr-2 h-4 w-4" />
              Снять с публикации
            </DropdownMenuItem>
          )}

          {/* Отмена */}
          {canCancelEvent(event.status) && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={() => setDialogOpen('cancel')}
                className="text-orange-600 focus:text-orange-600"
              >
                <XCircle className="mr-2 h-4 w-4" />
                Отменить событие
              </DropdownMenuItem>
            </>
          )}

          {/* Удаление (только черновики) */}
          {event.status === 'DRAFT' && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={() => setDialogOpen('delete')}
                className="text-destructive focus:text-destructive"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Удалить
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Диалог подтверждения отмены */}
      <AlertDialog
        open={dialogOpen === 'cancel'}
        onOpenChange={(open) => !open && handleDialogClose()}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Отменить событие?</AlertDialogTitle>
            <AlertDialogDescription>
              Все регистрации будут отменены. Участники получат уведомление.
              Это действие нельзя отменить.
            </AlertDialogDescription>
          </AlertDialogHeader>

          {/* Поле для причины отмены */}
          <div className="space-y-2 py-2">
            <Label htmlFor="cancel-reason">Причина отмены (опционально)</Label>
            <Textarea
              id="cancel-reason"
              placeholder="Укажите причину отмены события для уведомления участников..."
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              rows={3}
              data-testid="cancel-reason-input"
            />
          </div>

          <AlertDialogFooter>
            <AlertDialogCancel onClick={handleDialogClose}>Назад</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCancel}
              className="bg-orange-600 hover:bg-orange-700"
            >
              Отменить событие
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Диалог подтверждения удаления */}
      <AlertDialog
        open={dialogOpen === 'delete'}
        onOpenChange={(open) => !open && setDialogOpen(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Удалить событие?</AlertDialogTitle>
            <AlertDialogDescription>
              Событие &quot;{event.title}&quot; будет удалено. Это действие
              нельзя отменить.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Назад</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-destructive hover:bg-destructive/90"
            >
              Удалить
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
