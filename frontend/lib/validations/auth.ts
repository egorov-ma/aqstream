import { z } from 'zod';

// Email: обязателен, валидный формат
const emailSchema = z
  .string()
  .min(1, 'Email обязателен')
  .email('Некорректный формат email');

// Пароль: минимум 8 символов, должен содержать буквы И цифры
const passwordSchema = z
  .string()
  .min(8, 'Пароль должен содержать минимум 8 символов')
  .max(100, 'Пароль слишком длинный')
  .refine(
    (password) => /[a-zA-Z]/.test(password) && /\d/.test(password),
    'Пароль должен содержать буквы и цифры'
  );

// Схема входа
export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Пароль обязателен'),
});

// Схема регистрации
export const registerSchema = z
  .object({
    email: emailSchema.max(255, 'Email слишком длинный'),
    firstName: z
      .string()
      .min(1, 'Имя обязательно')
      .max(100, 'Имя слишком длинное'),
    lastName: z.string().max(100, 'Фамилия слишком длинная').optional().or(z.literal('')),
    password: passwordSchema,
    confirmPassword: z.string().min(1, 'Подтверждение пароля обязательно'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Пароли должны совпадать',
    path: ['confirmPassword'],
  });

// Схема восстановления пароля
export const forgotPasswordSchema = z.object({
  email: emailSchema,
});

// Схема сброса пароля
export const resetPasswordSchema = z
  .object({
    newPassword: passwordSchema,
    confirmPassword: z.string().min(1, 'Подтверждение пароля обязательно'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Пароли должны совпадать',
    path: ['confirmPassword'],
  });

// Типы для форм
export type LoginFormData = z.infer<typeof loginSchema>;
export type RegisterFormData = z.infer<typeof registerSchema>;
export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;
