'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AxiosError } from 'axios';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
  FormDescription,
} from '@/components/ui/form';
import { useCreateOrganizationRequest } from '@/lib/hooks/use-organization-requests';
import {
  createOrganizationRequestSchema,
  type CreateOrganizationRequestFormData,
} from '@/lib/validations/organization-request';
import type { ApiError } from '@/lib/api/types';

interface CreateRequestFormProps {
  onSuccess?: () => void;
}

export function CreateRequestForm({ onSuccess }: CreateRequestFormProps) {
  const [apiError, setApiError] = useState<string | null>(null);
  const createMutation = useCreateOrganizationRequest();

  const form = useForm<CreateOrganizationRequestFormData>({
    resolver: zodResolver(createOrganizationRequestSchema),
    defaultValues: {
      name: '',
      slug: '',
      description: '',
    },
  });

  // Автогенерация slug из названия
  const handleNameChange = (value: string) => {
    const currentSlug = form.getValues('slug');
    // Генерируем slug только если он пустой или был сгенерирован автоматически
    if (!currentSlug || currentSlug === generateSlug(form.getValues('name'))) {
      form.setValue('slug', generateSlug(value));
    }
  };

  function generateSlug(name: string): string {
    return name
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, '') // Убираем всё кроме букв, цифр, пробелов и дефисов
      .replace(/\s+/g, '-') // Заменяем пробелы на дефисы
      .replace(/-+/g, '-') // Убираем повторяющиеся дефисы
      .replace(/^-|-$/g, '') // Убираем дефисы в начале и конце
      .slice(0, 50); // Ограничиваем длину
  }

  async function onSubmit(data: CreateOrganizationRequestFormData) {
    setApiError(null);
    try {
      await createMutation.mutateAsync({
        name: data.name,
        slug: data.slug,
        description: data.description || undefined,
      });
      form.reset();
      onSuccess?.();
    } catch (error) {
      if (error instanceof AxiosError && error.response?.data) {
        const apiErrorData = error.response.data as ApiError;
        setApiError(apiErrorData.message || 'Ошибка при отправке заявки');
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
        data-testid="org-request-form"
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
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Название организации</FormLabel>
              <FormControl>
                <Input
                  placeholder="Моя организация"
                  data-testid="org-name-input"
                  {...field}
                  onChange={(e) => {
                    field.onChange(e);
                    handleNameChange(e.target.value);
                  }}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="slug"
          render={({ field }) => (
            <FormItem>
              <FormLabel>URL-идентификатор (slug)</FormLabel>
              <FormControl>
                <Input
                  placeholder="my-organization"
                  data-testid="org-slug-input"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                Только строчные латинские буквы, цифры и дефис
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Описание (необязательно)</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="Расскажите о вашей организации..."
                  className="min-h-[100px]"
                  data-testid="org-description-input"
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
          disabled={createMutation.isPending}
          data-testid="org-request-submit"
        >
          {createMutation.isPending ? 'Отправка...' : 'Отправить заявку'}
        </Button>
      </form>
    </Form>
  );
}
