'use client';

import { useState } from 'react';
import Link from 'next/link';
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
import { useLogin } from '@/lib/hooks/use-auth';
import { loginSchema, type LoginFormData } from '@/lib/validations/auth';
import { getAuthErrorMessage } from '@/lib/api/error-codes';
import type { ApiError } from '@/lib/api/types';

export function LoginForm() {
  const [apiError, setApiError] = useState<string | null>(null);
  const loginMutation = useLogin();

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  async function onSubmit(data: LoginFormData) {
    setApiError(null);
    try {
      await loginMutation.mutateAsync(data);
    } catch (error) {
      if (error instanceof AxiosError && error.response?.data) {
        const apiErrorData = error.response.data as ApiError;
        const message = getAuthErrorMessage(
          apiErrorData.code,
          apiErrorData.message || 'Неверный email или пароль'
        );
        setApiError(message);
      } else {
        setApiError('Произошла ошибка. Попробуйте позже');
      }
    }
  }

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="space-y-4"
        data-testid="login-form"
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

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Пароль</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  placeholder="••••••••"
                  autoComplete="current-password"
                  data-testid="password-input"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="flex justify-end">
          <Link
            href="/forgot-password"
            className="text-sm text-muted-foreground hover:text-primary"
            data-testid="forgot-password-link"
          >
            Забыли пароль?
          </Link>
        </div>

        <Button
          type="submit"
          className="w-full"
          disabled={loginMutation.isPending}
          data-testid="login-submit"
        >
          {loginMutation.isPending ? 'Вход...' : 'Войти'}
        </Button>
      </form>
    </Form>
  );
}
