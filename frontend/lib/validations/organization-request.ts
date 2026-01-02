import { z } from 'zod';

// Slug: 3-50 символов, только строчные буквы, цифры, дефис
const slugSchema = z
  .string()
  .min(3, 'Slug должен содержать минимум 3 символа')
  .max(50, 'Slug не должен превышать 50 символов')
  .regex(
    /^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$/,
    'Slug может содержать только строчные латинские буквы, цифры и дефис. Должен начинаться и заканчиваться буквой или цифрой'
  );

// Схема создания заявки на организацию
export const createOrganizationRequestSchema = z.object({
  name: z
    .string()
    .min(2, 'Название должно содержать минимум 2 символа')
    .max(255, 'Название не должно превышать 255 символов'),
  slug: slugSchema,
  description: z
    .string()
    .max(2000, 'Описание не должно превышать 2000 символов')
    .optional()
    .or(z.literal('')),
});

// Схема отклонения заявки (для админа)
export const rejectOrganizationRequestSchema = z.object({
  comment: z
    .string()
    .min(10, 'Причина отклонения должна содержать минимум 10 символов')
    .max(1000, 'Причина отклонения не должна превышать 1000 символов'),
});

// Типы для форм
export type CreateOrganizationRequestFormData = z.infer<typeof createOrganizationRequestSchema>;
export type RejectOrganizationRequestFormData = z.infer<typeof rejectOrganizationRequestSchema>;
