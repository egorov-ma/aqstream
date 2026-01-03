'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Separator } from '@/components/ui/separator';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import {
  baseEventFormSchema,
  defaultEventFormValues,
  validateEventCrossFields,
  type EventFormData,
} from '@/lib/validations/events';
import type { Event, Organization } from '@/lib/api/types';
import { DateTimePicker } from './date-time-picker';
import { TimezoneSelect } from './timezone-select';
import { MarkdownEditor } from './markdown-editor';
import { ImageUpload } from './image-upload';
import { TicketTypeList } from './ticket-type-list';
import { EventPreview } from './event-preview';
import { RecurrenceConfig } from './recurrence-config';

interface EventFormProps {
  event?: Event;
  onSubmit: (data: EventFormData, publish: boolean) => Promise<void>;
  isLoading?: boolean;
  /**
   * Список организаций для выбора (только для админов).
   * Если передан и isAdmin=true, показывается dropdown выбора организации.
   */
  organizations?: Organization[];
  /**
   * Является ли пользователь админом платформы.
   * Если true и передан organizations, показывается dropdown выбора организации.
   */
  isAdmin?: boolean;
}

// Маппинг Event в форму
function mapEventToForm(event: Event): Partial<EventFormData> {
  return {
    title: event.title,
    description: event.description ?? '',
    startsAt: event.startsAt,
    endsAt: event.endsAt ?? '',
    timezone: event.timezone,
    locationType: event.locationType,
    locationAddress: event.locationAddress ?? '',
    onlineUrl: event.onlineUrl ?? '',
    maxCapacity: event.maxCapacity ?? null,
    registrationOpensAt: event.registrationOpensAt ?? '',
    registrationClosesAt: event.registrationClosesAt ?? '',
    isPublic: event.isPublic,
    participantsVisibility: event.participantsVisibility,
    groupId: event.groupId ?? '',
    coverImageUrl: event.coverImageUrl ?? '',
    ticketTypes: [], // Билеты загружаются отдельно
  };
}

export function EventForm({
  event,
  onSubmit,
  isLoading,
  organizations,
  isAdmin = false,
}: EventFormProps) {
  const [isPublishing, setIsPublishing] = React.useState(false);
  const showOrganizationSelect = isAdmin && organizations && organizations.length > 0;

  // Type assertion: Zod .default() creates input/output type mismatch
  const form = useForm<EventFormData>({
    // eslint-disable-next-line
    resolver: zodResolver(baseEventFormSchema) as any,
    defaultValues: event ? mapEventToForm(event) : defaultEventFormValues,
  });

  const locationType = form.watch('locationType');

  // Валидация cross-field правил и установка ошибок в форму
  const validateCrossFields = (data: EventFormData): boolean => {
    const crossFieldErrors = validateEventCrossFields(data);
    crossFieldErrors.forEach((err) => {
      form.setError(err.field, { type: 'manual', message: err.message });
    });
    return crossFieldErrors.length === 0;
  };

  // Обработчик сохранения черновика
  const handleSaveDraft = async (data: EventFormData) => {
    // Cross-field валидация
    if (!validateCrossFields(data)) {
      return;
    }
    setIsPublishing(false);
    await onSubmit(data, false);
  };

  // Обработчик публикации
  const handlePublish = async () => {
    setIsPublishing(true);
    const isValid = await form.trigger();
    if (isValid) {
      const data = form.getValues();
      // Cross-field валидация
      if (!validateCrossFields(data)) {
        setIsPublishing(false);
        return;
      }
      await onSubmit(data, true);
    }
    setIsPublishing(false);
  };

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(handleSaveDraft)}
        className="space-y-6"
        data-testid="event-form"
      >
        {/* Выбор организации (только для админов) */}
        {showOrganizationSelect && (
          <Card>
            <CardHeader>
              <CardTitle>Организация</CardTitle>
            </CardHeader>
            <CardContent>
              <FormField
                control={form.control}
                name="organizationId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Выберите организацию *</FormLabel>
                    <Select
                      value={field.value ?? ''}
                      onValueChange={field.onChange}
                      disabled={isLoading}
                    >
                      <FormControl>
                        <SelectTrigger data-testid="event-organization-select">
                          <SelectValue placeholder="Выберите организацию" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {organizations.map((org) => (
                          <SelectItem key={org.id} value={org.id}>
                            {org.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormDescription>
                      Событие будет создано для выбранной организации
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>
        )}

        {/* Основная информация */}
        <Card>
          <CardHeader>
            <CardTitle>Основная информация</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Название */}
            <FormField
              control={form.control}
              name="title"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Название *</FormLabel>
                  <FormControl>
                    <Input
                      {...field}
                      placeholder="Введите название события"
                      disabled={isLoading}
                      data-testid="event-title-input"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Описание (Markdown) */}
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Описание</FormLabel>
                  <FormControl>
                    <MarkdownEditor
                      value={field.value ?? ''}
                      onChange={field.onChange}
                      disabled={isLoading}
                      data-testid="event-description-editor"
                    />
                  </FormControl>
                  <FormDescription>
                    Поддерживается форматирование Markdown
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Обложка */}
            <FormField
              control={form.control}
              name="coverImageUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Обложка</FormLabel>
                  <FormControl>
                    <ImageUpload
                      value={field.value ?? ''}
                      onChange={field.onChange}
                      disabled={isLoading}
                      data-testid="event-cover-upload"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        {/* Дата и время */}
        <Card>
          <CardHeader>
            <CardTitle>Дата и время</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Дата начала */}
              <FormField
                control={form.control}
                name="startsAt"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Начало *</FormLabel>
                    <FormControl>
                      <DateTimePicker
                        value={field.value}
                        onChange={field.onChange}
                        disabled={isLoading}
                        data-testid="event-starts-at-picker"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Дата окончания */}
              <FormField
                control={form.control}
                name="endsAt"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Окончание</FormLabel>
                    <FormControl>
                      <DateTimePicker
                        value={field.value ?? ''}
                        onChange={field.onChange}
                        disabled={isLoading}
                        placeholder="Опционально"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Часовой пояс */}
            <FormField
              control={form.control}
              name="timezone"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Часовой пояс</FormLabel>
                  <FormControl>
                    <TimezoneSelect
                      value={field.value}
                      onValueChange={field.onChange}
                      disabled={isLoading}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <Separator />

            {/* Повторение */}
            <FormField
              control={form.control}
              name="recurrenceRule"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <RecurrenceConfig
                      value={field.value ?? null}
                      onChange={field.onChange}
                      disabled={isLoading || !!event}
                    />
                  </FormControl>
                  <FormMessage />
                  {event && (
                    <FormDescription>
                      Настройка повторения недоступна при редактировании
                    </FormDescription>
                  )}
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        {/* Локация */}
        <Card>
          <CardHeader>
            <CardTitle>Локация</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Тип локации */}
            <FormField
              control={form.control}
              name="locationType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Формат проведения</FormLabel>
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                    disabled={isLoading}
                  >
                    <FormControl>
                      <SelectTrigger data-testid="event-location-type-select">
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="ONLINE">Онлайн</SelectItem>
                      <SelectItem value="OFFLINE">Офлайн</SelectItem>
                      <SelectItem value="HYBRID">Гибрид</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* URL для онлайн событий */}
            {(locationType === 'ONLINE' || locationType === 'HYBRID') && (
              <FormField
                control={form.control}
                name="onlineUrl"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>
                      Ссылка на трансляцию{' '}
                      {locationType === 'ONLINE' && '*'}
                    </FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        value={field.value ?? ''}
                        type="url"
                        placeholder="https://..."
                        disabled={isLoading}
                      />
                    </FormControl>
                    <FormDescription>
                      Zoom, YouTube, Telegram и др.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}

            {/* Адрес для офлайн событий */}
            {(locationType === 'OFFLINE' || locationType === 'HYBRID') && (
              <FormField
                control={form.control}
                name="locationAddress"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>
                      Адрес{' '}
                      {locationType === 'OFFLINE' && '*'}
                    </FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        value={field.value ?? ''}
                        placeholder="Город, улица, здание"
                        disabled={isLoading}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}
          </CardContent>
        </Card>

        {/* Настройки регистрации */}
        <Card>
          <CardHeader>
            <CardTitle>Настройки</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Максимальная вместимость */}
            <FormField
              control={form.control}
              name="maxCapacity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Максимальное количество участников</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      value={field.value ?? ''}
                      onChange={(e) =>
                        field.onChange(
                          e.target.value ? parseInt(e.target.value, 10) : null
                        )
                      }
                      placeholder="Без ограничений"
                      disabled={isLoading}
                      min={1}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <Separator />

            {/* Период регистрации */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="registrationOpensAt"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Открытие регистрации</FormLabel>
                    <FormControl>
                      <DateTimePicker
                        value={field.value ?? ''}
                        onChange={field.onChange}
                        disabled={isLoading}
                        placeholder="Сразу после публикации"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="registrationClosesAt"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Закрытие регистрации</FormLabel>
                    <FormControl>
                      <DateTimePicker
                        value={field.value ?? ''}
                        onChange={field.onChange}
                        disabled={isLoading}
                        placeholder="До начала события"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <Separator />

            {/* Видимость */}
            <div className="space-y-4">
              <FormField
                control={form.control}
                name="isPublic"
                render={({ field }) => (
                  <FormItem className="flex items-center justify-between rounded-lg border p-3">
                    <div className="space-y-0.5">
                      <FormLabel>Публичное событие</FormLabel>
                      <FormDescription>
                        Событие будет видно в публичном каталоге
                      </FormDescription>
                    </div>
                    <FormControl>
                      <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                        disabled={isLoading}
                      />
                    </FormControl>
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="participantsVisibility"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Видимость списка участников</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                      disabled={isLoading}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="CLOSED">
                          Только организаторы
                        </SelectItem>
                        <SelectItem value="OPEN">
                          Все зарегистрированные
                        </SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </CardContent>
        </Card>

        {/* Типы билетов */}
        <FormField
          control={form.control}
          name="ticketTypes"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <TicketTypeList
                  ticketTypes={field.value}
                  onChange={field.onChange}
                  disabled={isLoading}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Кнопки действий */}
        <div className="flex justify-end gap-4">
          {/* Предпросмотр */}
          <EventPreview data={form.getValues()} />

          <Button
            type="submit"
            variant="outline"
            disabled={isLoading}
            data-testid="event-submit"
          >
            {isLoading && !isPublishing ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Сохранение...
              </>
            ) : (
              'Сохранить черновик'
            )}
          </Button>
          <Button
            type="button"
            onClick={handlePublish}
            disabled={isLoading}
            data-testid="event-publish-submit"
          >
            {isLoading && isPublishing ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Публикация...
              </>
            ) : (
              'Опубликовать'
            )}
          </Button>
        </div>
      </form>
    </Form>
  );
}
