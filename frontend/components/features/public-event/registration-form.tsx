'use client';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
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
import {
  registrationSchema,
  getDefaultRegistrationValues,
  validateCustomFields,
  type RegistrationFormData,
} from '@/lib/validations/registration';
import { useAuthStore } from '@/lib/store/auth-store';
import type { Event, TicketType, CustomFieldConfig } from '@/lib/api/types';

interface RegistrationFormProps {
  event: Event;
  ticketTypes: TicketType[];
  disabled?: boolean;
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
}: RegistrationFormProps) {
  const { user } = useAuthStore();

  // Получаем дефолтные значения с автозаполнением для авторизованных
  const defaultValues = getDefaultRegistrationValues(event.registrationFormConfig, {
    firstName: user?.firstName,
    lastName: user?.lastName || undefined,
    email: user?.email,
  });

  const form = useForm<RegistrationFormData>({
    resolver: zodResolver(registrationSchema),
    defaultValues,
  });

  // Обновляем форму при изменении user
  useEffect(() => {
    if (user) {
      form.setValue('firstName', user.firstName);
      form.setValue('lastName', user.lastName || '');
      form.setValue('email', user.email);
    }
  }, [user, form]);

  const onSubmit = async (data: RegistrationFormData) => {
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

    // TODO: P2-019 — реализовать отправку регистрации
    toast.info('Регистрация будет доступна скоро', {
      description: 'Функционал регистрации находится в разработке.',
    });
  };

  const customFields = event.registrationFormConfig?.customFields || [];
  const isFormDisabled = disabled || event.status === 'CANCELLED' || event.status === 'COMPLETED';

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

            {/* Базовые поля */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="firstName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Имя *</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Иван"
                        disabled={isFormDisabled}
                        data-testid="firstName-input"
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
                    <FormLabel>Фамилия</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Иванов"
                        disabled={isFormDisabled}
                        data-testid="lastName-input"
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
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email *</FormLabel>
                  <FormControl>
                    <Input
                      type="email"
                      placeholder="ivan@example.com"
                      disabled={isFormDisabled}
                      data-testid="email-input"
                      {...field}
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
              disabled={isFormDisabled || form.formState.isSubmitting}
              data-testid="registration-submit"
            >
              {form.formState.isSubmitting ? 'Регистрация...' : 'Зарегистрироваться'}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}
