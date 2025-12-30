'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
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
import { useForgotPassword } from '@/lib/hooks/use-auth';
import { forgotPasswordSchema, type ForgotPasswordFormData } from '@/lib/validations/auth';

export function ForgotPasswordForm() {
  const [isSuccess, setIsSuccess] = useState(false);
  const mutation = useForgotPassword();

  const form = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: '',
    },
  });

  async function onSubmit(data: ForgotPasswordFormData) {
    try {
      await mutation.mutateAsync(data);
      setIsSuccess(true);
    } catch {
      // Для безопасности всегда показываем успех
      // (не раскрываем существование email)
      setIsSuccess(true);
    }
  }

  if (isSuccess) {
    return (
      <div className="text-center space-y-4" data-testid="success-message">
        <p className="text-muted-foreground">
          Если аккаунт с таким email существует, мы отправили инструкции по восстановлению
          пароля.
        </p>
        <p className="text-sm text-muted-foreground">Проверьте также папку «Спам».</p>
      </div>
    );
  }

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="space-y-4"
        data-testid="forgot-password-form"
      >
        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl>
                <Input
                  type="email"
                  placeholder="email@example.com"
                  autoComplete="email"
                  data-testid="email-input"
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
          data-testid="forgot-password-submit"
        >
          {mutation.isPending ? 'Отправка...' : 'Отправить инструкции'}
        </Button>
      </form>
    </Form>
  );
}
