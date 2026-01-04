'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AxiosError } from 'axios';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

import { TicketSelector } from './ticket-selector';
import { TicketTypeList } from './ticket-type-list';
import {
  registrationSchema,
  getDefaultRegistrationValues,
  validateCustomFields,
  type RegistrationFormData,
} from '@/lib/validations/registration';
import { useAuthStore } from '@/lib/store/auth-store';
import { useCreatePublicRegistration } from '@/lib/hooks/use-registrations';
import { getRegistrationErrorMessage } from '@/lib/api/error-codes';
import { ROUTES, getLoginUrl, getRegisterUrl } from '@/lib/routes';
import type { Event, TicketType, CustomFieldConfig, Registration, ApiError } from '@/lib/api/types';

interface RegistrationFormProps {
  event: Event;
  ticketTypes: TicketType[];
  disabled?: boolean;
  onSuccess?: (registration: Registration) => void;
}

/**
 * Рендерит кастомное поле формы
 */
function CustomFieldInput({
  field,
  formField,
}: {
  field: CustomFieldConfig;
  formField: {
    value: string;
    onChange: (value: string) => void;
  };
}) {
  if (field.type === 'select' && field.options && field.options.length > 0) {
    return (
      <Select onValueChange={formField.onChange} defaultValue={formField.value}>
        <SelectTrigger>
          <SelectValue placeholder={`Выберите ${field.label.toLowerCase()}`} />
        </SelectTrigger>
        <SelectContent>
          {field.options.map((option) => (
            <SelectItem key={option} value={option}>
              {option}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    );
  }

  return (
    <Input
      type={field.type === 'tel' ? 'tel' : field.type === 'email' ? 'email' : 'text'}
      placeholder={field.label}
      value={formField.value}
      onChange={(e) => formField.onChange(e.target.value)}
    />
  );
}

/**
 * Форма регистрации на событие
 * Поддерживает базовые поля и динамические custom fields
 */
export function RegistrationForm({
  event,
  ticketTypes,
  disabled = false,
  onSuccess,
}: RegistrationFormProps) {
  const { user } = useAuthStore();
  const router = useRouter();
  const [apiError, setApiError] = useState<string | null>(null);

  // Используем публичный endpoint для регистрации по slug
  const createRegistration = useCreatePublicRegistration(event.slug);

  // Получаем дефолтные значения формы (без личных данных - автозаполнение на backend)
  const defaultValues = getDefaultRegistrationValues(event.registrationFormConfig);

  const form = useForm<RegistrationFormData>({
    resolver: zodResolver(registrationSchema),
    defaultValues,
  });

  const onSubmit = async (data: RegistrationFormData) => {
    // Сбрасываем ошибку API
    setApiError(null);

    // Валидируем custom fields
    const customFieldsConfig = event.registrationFormConfig?.customFields || [];
    if (customFieldsConfig.length > 0 && data.customFields) {
      const customErrors = validateCustomFields(
        data.customFields as Record<string, string>,
        customFieldsConfig
      );
      if (customErrors) {
        // Устанавливаем ошибки для custom fields
        Object.entries(customErrors).forEach(([fieldName, error]) => {
          form.setError(`customFields.${fieldName}` as `customFields.${string}`, {
            type: 'manual',
            message: error,
          });
        });
        return;
      }
    }

    try {
      // Отправляем только ticketTypeId и customFields (личные данные автозаполняются на backend)
      const registration = await createRegistration.mutateAsync({
        ticketTypeId: data.ticketTypeId,
        customFields: data.customFields as Record<string, string> | undefined,
      });

      // Revalidate серверные данные (обновляет счетчики билетов и userRegistration)
      router.refresh();

      // Вызываем callback или редиректим на success page
      if (onSuccess) {
        onSuccess(registration);
      } else {
        router.push(ROUTES.EVENT_SUCCESS(event.slug, registration.confirmationCode));
      }
    } catch (error) {
      if (error instanceof AxiosError && error.response?.data) {
        const apiErrorData = error.response.data as ApiError;
        const message = getRegistrationErrorMessage(apiErrorData.code, apiErrorData.message);
        setApiError(message);
      } else {
        setApiError('Произошла ошибка при регистрации. Попробуйте ещё раз.');
      }
    }
  };

  const customFields = event.registrationFormConfig?.customFields || [];
  const isFormDisabled = disabled || event.status === 'CANCELLED' || event.status === 'COMPLETED';

  // Auth guard: показываем список билетов и форму входа если пользователь не авторизован
  if (!user) {
    return (
      <Card data-testid="registration-form-card">
        <CardHeader>
          <CardTitle>Регистрация</CardTitle>
          <CardDescription>
            Для регистрации на событие необходимо войти в аккаунт
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Список доступных билетов */}
          {ticketTypes.length > 0 && (
            <div>
              <h3 className="text-sm font-medium mb-3">Доступные билеты</h3>
              <TicketTypeList ticketTypes={ticketTypes} />
            </div>
          )}

          <p className="text-sm text-muted-foreground">
            Войдите в свой аккаунт или зарегистрируйтесь, чтобы записаться на это событие.
          </p>
          <div className="flex flex-col gap-2">
            <Button asChild data-testid="login-button">
              <Link href={getLoginUrl(ROUTES.EVENT(event.slug))}>
                Войти
              </Link>
            </Button>
            <Button variant="outline" asChild data-testid="register-button">
              <Link href={getRegisterUrl(ROUTES.EVENT(event.slug))}>
                Зарегистрироваться
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card data-testid="registration-form-card">
      <CardHeader>
        <CardTitle>Регистрация</CardTitle>
        <CardDescription>
          Заполните форму для регистрации на событие
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6" data-testid="registration-form">
            {/* Ошибка API */}
            {apiError && (
              <Alert variant="destructive" data-testid="api-error-message">
                <AlertDescription>{apiError}</AlertDescription>
              </Alert>
            )}

            {/* Выбор типа билета */}
            <FormField
              control={form.control}
              name="ticketTypeId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Тип билета</FormLabel>
                  <FormControl>
                    <TicketSelector
                      ticketTypes={ticketTypes}
                      selectedId={field.value}
                      onSelect={field.onChange}
                      disabled={isFormDisabled}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Кастомные поля */}
            {customFields.length > 0 && (
              <div className="space-y-4 pt-4 border-t">
                <h3 className="text-sm font-medium text-muted-foreground">
                  Дополнительная информация
                </h3>
                {customFields.map((customField) => (
                  <FormField
                    key={customField.name}
                    control={form.control}
                    name={`customFields.${customField.name}`}
                    render={({ field: formField }) => (
                      <FormItem>
                        <FormLabel>
                          {customField.label}
                          {customField.required && ' *'}
                        </FormLabel>
                        <FormControl>
                          <CustomFieldInput
                            field={customField}
                            formField={{
                              value: (formField.value as string) || '',
                              onChange: formField.onChange,
                            }}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                ))}
              </div>
            )}

            {/* Кнопка отправки */}
            <Button
              type="submit"
              className="w-full"
              disabled={isFormDisabled || createRegistration.isPending}
              data-testid="registration-submit"
            >
              {createRegistration.isPending ? 'Регистрация...' : 'Зарегистрироваться'}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}
