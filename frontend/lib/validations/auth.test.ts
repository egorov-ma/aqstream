import { describe, it, expect } from 'vitest';
import {
  loginSchema,
  registerSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
} from './auth';

describe('loginSchema', () => {
  it('validates correct data', () => {
    const result = loginSchema.safeParse({
      email: 'test@example.com',
      password: 'password123',
    });
    expect(result.success).toBe(true);
  });

  it('rejects empty email', () => {
    const result = loginSchema.safeParse({
      email: '',
      password: 'password123',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Email обязателен');
    }
  });

  it('rejects invalid email format', () => {
    const result = loginSchema.safeParse({
      email: 'invalid-email',
      password: 'password123',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Некорректный формат email');
    }
  });

  it('rejects empty password', () => {
    const result = loginSchema.safeParse({
      email: 'test@example.com',
      password: '',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Пароль обязателен');
    }
  });
});

describe('registerSchema', () => {
  it('validates correct data with lastName', () => {
    const result = registerSchema.safeParse({
      email: 'test@example.com',
      firstName: 'Иван',
      lastName: 'Иванов',
      password: 'password123',
      confirmPassword: 'password123',
    });
    expect(result.success).toBe(true);
  });

  it('validates correct data without lastName', () => {
    const result = registerSchema.safeParse({
      email: 'test@example.com',
      firstName: 'Иван',
      lastName: '',
      password: 'password123',
      confirmPassword: 'password123',
    });
    expect(result.success).toBe(true);
  });

  it('rejects short password', () => {
    const result = registerSchema.safeParse({
      email: 'test@example.com',
      firstName: 'Иван',
      password: 'pass1',
      confirmPassword: 'pass1',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe(
        'Пароль должен содержать минимум 8 символов'
      );
    }
  });

  it('rejects password without digits', () => {
    const result = registerSchema.safeParse({
      email: 'test@example.com',
      firstName: 'Иван',
      password: 'passwordonly',
      confirmPassword: 'passwordonly',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe(
        'Пароль должен содержать буквы и цифры'
      );
    }
  });

  it('rejects password without letters', () => {
    const result = registerSchema.safeParse({
      email: 'test@example.com',
      firstName: 'Иван',
      password: '12345678',
      confirmPassword: '12345678',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe(
        'Пароль должен содержать буквы и цифры'
      );
    }
  });

  it('rejects mismatched passwords', () => {
    const result = registerSchema.safeParse({
      email: 'test@example.com',
      firstName: 'Иван',
      password: 'password123',
      confirmPassword: 'different123',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Пароли должны совпадать');
      expect(result.error.issues[0].path).toContain('confirmPassword');
    }
  });

  it('rejects empty firstName', () => {
    const result = registerSchema.safeParse({
      email: 'test@example.com',
      firstName: '',
      password: 'password123',
      confirmPassword: 'password123',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Имя обязательно');
    }
  });
});

describe('forgotPasswordSchema', () => {
  it('validates correct email', () => {
    const result = forgotPasswordSchema.safeParse({
      email: 'test@example.com',
    });
    expect(result.success).toBe(true);
  });

  it('rejects empty email', () => {
    const result = forgotPasswordSchema.safeParse({
      email: '',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Email обязателен');
    }
  });

  it('rejects invalid email', () => {
    const result = forgotPasswordSchema.safeParse({
      email: 'invalid',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Некорректный формат email');
    }
  });
});

describe('resetPasswordSchema', () => {
  it('validates correct data', () => {
    const result = resetPasswordSchema.safeParse({
      newPassword: 'newpassword123',
      confirmPassword: 'newpassword123',
    });
    expect(result.success).toBe(true);
  });

  it('rejects short password', () => {
    const result = resetPasswordSchema.safeParse({
      newPassword: 'pass1',
      confirmPassword: 'pass1',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe(
        'Пароль должен содержать минимум 8 символов'
      );
    }
  });

  it('rejects mismatched passwords', () => {
    const result = resetPasswordSchema.safeParse({
      newPassword: 'newpassword123',
      confirmPassword: 'different123',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Пароли должны совпадать');
    }
  });

  it('rejects password without letters and digits', () => {
    const result = resetPasswordSchema.safeParse({
      newPassword: 'onlyletters',
      confirmPassword: 'onlyletters',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe(
        'Пароль должен содержать буквы и цифры'
      );
    }
  });
});
