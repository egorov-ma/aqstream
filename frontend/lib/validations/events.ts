import { z } from 'zod';

// Схема правила повторения
export const recurrenceRuleSchema = z.object({
  frequency: z.enum(['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']),
  interval: z.number().min(1, 'Интервал должен быть больше 0').max(99),
  endsAt: z.string().optional(),
  occurrenceCount: z.number().min(1).max(365).optional(),
  byDay: z.string().optional(), // "MO,WE,FR"
  byMonthDay: z.number().min(1).max(31).optional(),
  excludedDates: z.array(z.string()).optional(),
}).refine(
  (data) => {
    // Для WEEKLY должен быть указан хотя бы один день недели
    if (data.frequency === 'WEEKLY') {
      return data.byDay && data.byDay.length > 0;
    }
    return true;
  },
  {
    message: 'Выберите хотя бы один день недели',
    path: ['byDay'],
  }
);

export type RecurrenceRuleFormData = z.infer<typeof recurrenceRuleSchema>;

// Схема типа билета (для inline формы на странице события)
export const ticketTypeSchema = z.object({
  id: z.string().uuid().optional(),
  name: z
    .string()
    .min(1, 'Название типа билета обязательно')
    .max(100, 'Название не должно превышать 100 символов'),
  description: z
    .string()
    .max(1000, 'Описание не должно превышать 1000 символов')
    .optional()
    .or(z.literal('')),
  quantity: z.coerce
    .number()
    .min(1, 'Количество должно быть больше 0')
    .optional()
    .nullable(),
  salesStart: z.string().optional().nullable().or(z.literal('')),
  salesEnd: z.string().optional().nullable().or(z.literal('')),
  sortOrder: z.coerce.number().min(0).default(0),
});

// Базовая схема события (для useForm/zodResolver, без cross-field валидации)
export const baseEventFormSchema = z.object({
  // Основная информация
  title: z
    .string()
    .min(1, 'Название обязательно')
    .max(255, 'Название не должно превышать 255 символов'),
  description: z
    .string()
    .max(10000, 'Описание не должно превышать 10000 символов')
    .optional()
    .or(z.literal('')),
  coverImageUrl: z.string().url('Некорректный URL изображения').optional().or(z.literal('')),

  // Дата и время
  startsAt: z.string().min(1, 'Дата начала обязательна'),
  endsAt: z.string().optional().or(z.literal('')),
  timezone: z.string().default('Europe/Moscow'),

  // Локация
  locationType: z.enum(['ONLINE', 'OFFLINE', 'HYBRID']).default('ONLINE'),
  locationAddress: z
    .string()
    .max(500, 'Адрес не должен превышать 500 символов')
    .optional()
    .or(z.literal('')),
  onlineUrl: z
    .string()
    .max(500, 'URL не должен превышать 500 символов')
    .optional()
    .or(z.literal('')),

  // Настройки
  maxCapacity: z.coerce
    .number()
    .min(1, 'Вместимость должна быть больше 0')
    .optional()
    .nullable(),
  registrationOpensAt: z.string().optional().or(z.literal('')),
  registrationClosesAt: z.string().optional().or(z.literal('')),
  isPublic: z.boolean().default(false),
  participantsVisibility: z.enum(['CLOSED', 'OPEN']).default('CLOSED'),
  groupId: z.string().uuid().optional().nullable().or(z.literal('')),

  // Типы билетов
  ticketTypes: z.array(ticketTypeSchema).default([]),

  // Повторение
  recurrenceRule: recurrenceRuleSchema.nullable().optional(),
});

// Тип для формы (используется в useForm)
export type EventFormData = z.infer<typeof baseEventFormSchema>;

// Схема с cross-field валидацией (для runtime проверки)
export const eventFormSchema = baseEventFormSchema
  // Валидация: для OFFLINE/HYBRID требуется адрес
  .refine(
    (data) => {
      if (data.locationType === 'OFFLINE' || data.locationType === 'HYBRID') {
        return !!data.locationAddress && data.locationAddress.trim().length > 0;
      }
      return true;
    },
    {
      message: 'Укажите адрес для офлайн или гибридного события',
      path: ['locationAddress'],
    }
  )
  // Валидация: для ONLINE/HYBRID требуется URL
  .refine(
    (data) => {
      if (data.locationType === 'ONLINE' || data.locationType === 'HYBRID') {
        return !!data.onlineUrl && data.onlineUrl.trim().length > 0;
      }
      return true;
    },
    {
      message: 'Укажите URL для онлайн или гибридного события',
      path: ['onlineUrl'],
    }
  )
  // Валидация: endsAt >= startsAt
  .refine(
    (data) => {
      if (data.endsAt && data.startsAt) {
        return new Date(data.endsAt) >= new Date(data.startsAt);
      }
      return true;
    },
    {
      message: 'Дата окончания должна быть после даты начала',
      path: ['endsAt'],
    }
  )
  // Валидация: registrationClosesAt >= registrationOpensAt
  .refine(
    (data) => {
      if (data.registrationClosesAt && data.registrationOpensAt) {
        return new Date(data.registrationClosesAt) >= new Date(data.registrationOpensAt);
      }
      return true;
    },
    {
      message: 'Дата закрытия регистрации должна быть после даты открытия',
      path: ['registrationClosesAt'],
    }
  );

// Схема для быстрого добавления типа билета
export const quickTicketTypeSchema = z.object({
  name: z
    .string()
    .min(1, 'Название обязательно')
    .max(100, 'Название не должно превышать 100 символов'),
  quantity: z.coerce
    .number()
    .min(1, 'Количество должно быть больше 0')
    .optional()
    .nullable(),
});

// Типы для форм
export type TicketTypeFormData = z.infer<typeof ticketTypeSchema>;
export type QuickTicketTypeFormData = z.infer<typeof quickTicketTypeSchema>;

// Значения по умолчанию для новой формы
export const defaultEventFormValues: EventFormData = {
  title: '',
  description: '',
  startsAt: '',
  endsAt: '',
  timezone: 'Europe/Moscow',
  locationType: 'ONLINE',
  locationAddress: '',
  onlineUrl: '',
  maxCapacity: null,
  registrationOpensAt: '',
  registrationClosesAt: '',
  isPublic: false,
  participantsVisibility: 'CLOSED',
  groupId: '',
  coverImageUrl: '',
  ticketTypes: [],
  recurrenceRule: null,
};

// Ошибка cross-field валидации
export interface CrossFieldError {
  field: keyof EventFormData;
  message: string;
}

// Cross-field валидация (для ручной проверки при submit)
export function validateEventCrossFields(data: EventFormData): CrossFieldError[] {
  const errors: CrossFieldError[] = [];

  // Для OFFLINE/HYBRID требуется адрес
  if (data.locationType === 'OFFLINE' || data.locationType === 'HYBRID') {
    if (!data.locationAddress || data.locationAddress.trim().length === 0) {
      errors.push({
        field: 'locationAddress',
        message: 'Укажите адрес для офлайн или гибридного события',
      });
    }
  }

  // Для ONLINE/HYBRID требуется URL
  if (data.locationType === 'ONLINE' || data.locationType === 'HYBRID') {
    if (!data.onlineUrl || data.onlineUrl.trim().length === 0) {
      errors.push({
        field: 'onlineUrl',
        message: 'Укажите URL для онлайн или гибридного события',
      });
    }
  }

  // endsAt >= startsAt
  if (data.endsAt && data.startsAt) {
    if (new Date(data.endsAt) < new Date(data.startsAt)) {
      errors.push({
        field: 'endsAt',
        message: 'Дата окончания должна быть после даты начала',
      });
    }
  }

  // registrationClosesAt >= registrationOpensAt
  if (data.registrationClosesAt && data.registrationOpensAt) {
    if (new Date(data.registrationClosesAt) < new Date(data.registrationOpensAt)) {
      errors.push({
        field: 'registrationClosesAt',
        message: 'Дата закрытия регистрации должна быть после даты открытия',
      });
    }
  }

  return errors;
}
