'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import {
  rejectOrganizationRequestSchema,
  type RejectOrganizationRequestFormData,
} from '@/lib/validations/organization-request';
import type { OrganizationRequest } from '@/lib/api/types';

interface RejectDialogProps {
  request: OrganizationRequest | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: (id: string, comment: string) => void;
  isPending?: boolean;
}

export function RejectDialog({
  request,
  open,
  onOpenChange,
  onConfirm,
  isPending,
}: RejectDialogProps) {
  const form = useForm<RejectOrganizationRequestFormData>({
    resolver: zodResolver(rejectOrganizationRequestSchema),
    defaultValues: {
      comment: '',
    },
  });

  const handleSubmit = (data: RejectOrganizationRequestFormData) => {
    if (request) {
      onConfirm(request.id, data.comment);
      form.reset();
    }
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      form.reset();
    }
    onOpenChange(open);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent data-testid="reject-dialog">
        <DialogHeader>
          <DialogTitle>Отклонить заявку</DialogTitle>
          <DialogDescription>
            Вы собираетесь отклонить заявку на создание организации &quot;{request?.name}&quot;.
            Укажите причину отклонения.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="comment"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Причина отклонения</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Укажите причину отклонения заявки (минимум 10 символов)..."
                      className="min-h-[100px]"
                      data-testid="reject-comment-input"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleOpenChange(false)}
              >
                Отмена
              </Button>
              <Button
                type="submit"
                variant="destructive"
                disabled={isPending}
                data-testid="reject-confirm"
              >
                {isPending ? 'Отклонение...' : 'Отклонить заявку'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
