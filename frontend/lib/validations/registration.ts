import { z } from 'zod';
import type { CustomFieldConfig, RegistrationFormConfig } from '@/lib/api/types';

/**
 * Схема регистрации с поддержкой custom fields
 */
export const registrationSchema = z.object({
  firstName: z.string().min(1, 'Имя обязательно'),
  lastName: z.string().optional(),
  email: z.string().email('Некорректный email'),
  ticketTypeId: z.string().min(1, 'Выберите тип билета'),
  customFields: z.record(z.string(), z.string()).optional(),
});

export type RegistrationFormData = z.infer<typeof registrationSchema>;

/**
 * Валидирует custom fields на основе конфигурации
 * Вызывается вручную при submit
 */
export function validateCustomFields(
  data: Record<string, string>,
  config: CustomFieldConfig[]
): Record<string, string> | null {
  const errors: Record<string, string> = {};

  for (const field of config) {
    const value = data[field.name] || '';

    // Проверка обязательности
    if (field.required && !value.trim()) {
      errors[field.name] = `${field.label} обязательно`;
      continue;
    }

    // Пропускаем пустые необязательные поля
    if (!value.trim()) continue;

    // Валидация по типу
    switch (field.type) {
      case 'email':
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          errors[field.name] = 'Некорректный email';
        }
        break;
      case 'tel':
        if (!/^[+]?[\d\s()-]{7,}$/.test(value)) {
          errors[field.name] = 'Некорректный номер телефона';
        }
        break;
      case 'select':
        if (field.options && !field.options.includes(value)) {
          errors[field.name] = 'Выберите значение из списка';
        }
        break;
    }
  }

  return Object.keys(errors).length > 0 ? errors : null;
}

/**
 * Дефолтные значения для формы регистрации
 */
export function getDefaultRegistrationValues(
  formConfig?: RegistrationFormConfig,
  prefill?: Partial<RegistrationFormData>
): RegistrationFormData {
  const defaults: RegistrationFormData = {
    firstName: prefill?.firstName ?? '',
    lastName: prefill?.lastName ?? '',
    email: prefill?.email ?? '',
    ticketTypeId: prefill?.ticketTypeId ?? '',
    customFields: {},
  };

  // Инициализируем custom fields пустыми значениями
  if (formConfig?.customFields) {
    for (const field of formConfig.customFields) {
      defaults.customFields![field.name] = prefill?.customFields?.[field.name] ?? '';
    }
  }

  return defaults;
}
