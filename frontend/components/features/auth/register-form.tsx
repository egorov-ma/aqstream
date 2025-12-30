'use client';

import { useState } from 'react';
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
import { useRegister } from '@/lib/hooks/use-auth';
import { registerSchema, type RegisterFormData } from '@/lib/validations/auth';
import { getAuthErrorMessage } from '@/lib/api/error-codes';
import type { ApiError } from '@/lib/api/types';

export function RegisterForm() {
  const [apiError, setApiError] = useState<string | null>(null);
  const registerMutation = useRegister();

  const form = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: '',
      firstName: '',
      lastName: '',
      password: '',
      confirmPassword: '',
    },
  });

  async function onSubmit(data: RegisterFormData) {
    setApiError(null);
    try {
      // Удаляем confirmPassword — не нужен для API
      const { confirmPassword: _, ...registerData } = data;
      await registerMutation.mutateAsync(registerData);
    } catch (error) {
      if (error instanceof AxiosError && error.response?.data) {
        const apiErrorData = error.response.data as ApiError;
        const message = getAuthErrorMessage(
          apiErrorData.code,
          apiErrorData.message || 'Ошибка регистрации'
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
        data-testid="register-form"
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

        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="firstName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Имя</FormLabel>
                <FormControl>
                  <Input
                    placeholder="Иван"
                    autoComplete="given-name"
                    data-testid="first-name-input"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="lastName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Фамилия (необязательно)</FormLabel>
                <FormControl>
                  <Input
                    placeholder="Иванов"
                    autoComplete="family-name"
                    data-testid="last-name-input"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

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
          disabled={registerMutation.isPending}
          data-testid="register-submit"
        >
          {registerMutation.isPending ? 'Регистрация...' : 'Зарегистрироваться'}
        </Button>
      </form>
    </Form>
  );
}
