'use client';

import { useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AxiosError } from 'axios';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { useResetPassword } from '@/lib/hooks/use-auth';
import { resetPasswordSchema, type ResetPasswordFormData } from '@/lib/validations/auth';

export function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const [apiError, setApiError] = useState<string | null>(null);
  const mutation = useResetPassword();

  const form = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      newPassword: '',
      confirmPassword: '',
    },
  });

  // Функция отправки определена до проверки token,
  // чтобы избежать non-null assertion
  async function onSubmit(data: ResetPasswordFormData) {
    if (!token) return; // Дополнительная защита (не должна срабатывать)
    setApiError(null);
    try {
      await mutation.mutateAsync({
        token,
        newPassword: data.newPassword,
      });
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 400) {
        setApiError('Ссылка недействительна или срок её действия истёк');
      } else {
        setApiError('Произошла ошибка. Попробуйте позже');
      }
    }
  }

  if (!token) {
    return (
      <div className="text-center text-destructive" data-testid="api-error-message">
        Недействительная ссылка. Запросите сброс пароля заново.
      </div>
    );
  }

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="space-y-4"
        data-testid="reset-password-form"
      >
        {apiError && (
          <div
            className="rounded-md bg-destructive/15 p-3 text-sm text-destructive"
            data-testid="api-error-message"
          >
            {apiError}
          </div>
        )}

        <FormField
          control={form.control}
          name="newPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Новый пароль</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  placeholder="••••••••"
                  autoComplete="new-password"
                  data-testid="password-input"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="confirmPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Подтвердите пароль</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  placeholder="••••••••"
                  autoComplete="new-password"
                  data-testid="confirm-password-input"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button
          type="submit"
          className="w-full"
          disabled={mutation.isPending}
          data-testid="reset-password-submit"
        >
          {mutation.isPending ? 'Сохранение...' : 'Сохранить пароль'}
        </Button>
      </form>
    </Form>
  );
}
