'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AxiosError } from 'axios';

import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { PasswordInput } from '@/components/ui/password-input';

import { useChangePassword } from '@/lib/hooks/use-profile';
import {
  changePasswordSchema,
  type ChangePasswordFormData,
} from '@/lib/validations/profile';
import type { ApiError } from '@/lib/api/types';

const PROFILE_ERROR_MESSAGES: Record<string, string> = {
  wrong_password: 'Неверный текущий пароль',
};

function getErrorMessage(code: string, fallback: string): string {
  return PROFILE_ERROR_MESSAGES[code] || fallback;
}

/**
 * Форма смены пароля пользователя.
 */
export function ChangePasswordForm() {
  const changePassword = useChangePassword();

  const form = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const onSubmit = (data: ChangePasswordFormData) => {
    changePassword.mutate(
      {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      },
      {
        onSuccess: () => {
          form.reset();
        },
        onError: (error) => {
          if (error instanceof AxiosError && error.response?.data) {
            const apiError = error.response.data as ApiError;
            const message = getErrorMessage(apiError.code, apiError.message);
            form.setError('currentPassword', { message });
          }
        },
      }
    );
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="currentPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Текущий пароль</FormLabel>
              <FormControl>
                <PasswordInput
                  {...field}
                  placeholder="Введите текущий пароль"
                  data-testid="currentPassword-input"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="newPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Новый пароль</FormLabel>
              <FormControl>
                <PasswordInput
                  {...field}
                  placeholder="Минимум 8 символов, буквы и цифры"
                  data-testid="newPassword-input"
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
              <FormLabel>Подтверждение пароля</FormLabel>
              <FormControl>
                <PasswordInput
                  {...field}
                  placeholder="Повторите новый пароль"
                  data-testid="confirmPassword-input"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button
          type="submit"
          disabled={changePassword.isPending}
          data-testid="change-password-submit"
        >
          {changePassword.isPending ? 'Изменение...' : 'Изменить пароль'}
        </Button>
      </form>
    </Form>
  );
}
