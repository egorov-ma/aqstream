import { z } from 'zod';

/**
 * Схема валидации профиля
 */
export const profileSchema = z.object({
  firstName: z
    .string()
    .min(1, 'Имя обязательно')
    .max(100, 'Имя не должно превышать 100 символов'),
  lastName: z
    .string()
    .max(100, 'Фамилия не должна превышать 100 символов')
    .optional()
    .or(z.literal('')),
});

export type ProfileFormData = z.infer<typeof profileSchema>;

/**
 * Схема валидации смены пароля
 */
export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Текущий пароль обязателен'),
    newPassword: z
      .string()
      .min(8, 'Минимум 8 символов')
      .max(100, 'Максимум 100 символов')
      .refine(
        (password) => /[A-Za-zА-Яа-яЁё]/.test(password) && /\d/.test(password),
        'Пароль должен содержать буквы и цифры'
      ),
    confirmPassword: z.string().min(1, 'Подтверждение пароля обязательно'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Пароли не совпадают',
    path: ['confirmPassword'],
  });

export type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;
