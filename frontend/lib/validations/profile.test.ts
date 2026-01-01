import { describe, it, expect } from 'vitest';
import { profileSchema, changePasswordSchema } from './profile';

describe('profileSchema', () => {
  describe('firstName', () => {
    it('accepts valid first name', () => {
      const result = profileSchema.safeParse({ firstName: 'Иван' });
      expect(result.success).toBe(true);
    });

    it('rejects empty first name', () => {
      const result = profileSchema.safeParse({ firstName: '' });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Имя обязательно');
      }
    });

    it('rejects too long first name', () => {
      const result = profileSchema.safeParse({ firstName: 'a'.repeat(101) });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Имя не должно превышать 100 символов');
      }
    });

    it('accepts 100 character first name', () => {
      const result = profileSchema.safeParse({ firstName: 'a'.repeat(100) });
      expect(result.success).toBe(true);
    });
  });

  describe('lastName', () => {
    it('accepts valid last name', () => {
      const result = profileSchema.safeParse({ firstName: 'Иван', lastName: 'Иванов' });
      expect(result.success).toBe(true);
    });

    it('accepts empty last name', () => {
      const result = profileSchema.safeParse({ firstName: 'Иван', lastName: '' });
      expect(result.success).toBe(true);
    });

    it('accepts undefined last name', () => {
      const result = profileSchema.safeParse({ firstName: 'Иван' });
      expect(result.success).toBe(true);
    });

    it('rejects too long last name', () => {
      const result = profileSchema.safeParse({ firstName: 'Иван', lastName: 'a'.repeat(101) });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Фамилия не должна превышать 100 символов');
      }
    });

    it('accepts 100 character last name', () => {
      const result = profileSchema.safeParse({ firstName: 'Иван', lastName: 'a'.repeat(100) });
      expect(result.success).toBe(true);
    });
  });
});

describe('changePasswordSchema', () => {
  const validData = {
    currentPassword: 'oldPassword123',
    newPassword: 'newPassword123',
    confirmPassword: 'newPassword123',
  };

  describe('currentPassword', () => {
    it('accepts non-empty current password', () => {
      const result = changePasswordSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('rejects empty current password', () => {
      const result = changePasswordSchema.safeParse({
        ...validData,
        currentPassword: '',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Текущий пароль обязателен');
      }
    });
  });

  describe('newPassword', () => {
    it('accepts valid password with letters and digits', () => {
      const result = changePasswordSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('accepts password with Cyrillic letters and digits', () => {
      const result = changePasswordSchema.safeParse({
        ...validData,
        newPassword: 'Пароль123',
        confirmPassword: 'Пароль123',
      });
      expect(result.success).toBe(true);
    });

    it('rejects password shorter than 8 characters', () => {
      const result = changePasswordSchema.safeParse({
        ...validData,
        newPassword: 'Pass1',
        confirmPassword: 'Pass1',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Минимум 8 символов');
      }
    });

    it('rejects password longer than 100 characters', () => {
      const longPassword = 'a'.repeat(99) + '12';
      const result = changePasswordSchema.safeParse({
        ...validData,
        newPassword: longPassword,
        confirmPassword: longPassword,
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Максимум 100 символов');
      }
    });

    it('rejects password without letters', () => {
      const result = changePasswordSchema.safeParse({
        ...validData,
        newPassword: '12345678',
        confirmPassword: '12345678',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Пароль должен содержать буквы и цифры');
      }
    });

    it('rejects password without digits', () => {
      const result = changePasswordSchema.safeParse({
        ...validData,
        newPassword: 'PasswordOnly',
        confirmPassword: 'PasswordOnly',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe('Пароль должен содержать буквы и цифры');
      }
    });
  });

  describe('confirmPassword', () => {
    it('accepts matching passwords', () => {
      const result = changePasswordSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('rejects non-matching passwords', () => {
      const result = changePasswordSchema.safeParse({
        ...validData,
        confirmPassword: 'differentPassword1',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        const passwordMismatchError = result.error.issues.find(
          (issue) => issue.path.includes('confirmPassword') && issue.message === 'Пароли не совпадают'
        );
        expect(passwordMismatchError).toBeDefined();
      }
    });

    it('rejects empty confirm password', () => {
      const result = changePasswordSchema.safeParse({
        ...validData,
        confirmPassword: '',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        const emptyConfirmError = result.error.issues.find(
          (issue) => issue.message === 'Подтверждение пароля обязательно'
        );
        expect(emptyConfirmError).toBeDefined();
      }
    });
  });

  describe('full form validation', () => {
    it('validates complete valid form', () => {
      const result = changePasswordSchema.safeParse({
        currentPassword: 'myOldPassword',
        newPassword: 'myNewPass123',
        confirmPassword: 'myNewPass123',
      });
      expect(result.success).toBe(true);
    });

    it('returns all errors for invalid form', () => {
      const result = changePasswordSchema.safeParse({
        currentPassword: '',
        newPassword: 'abc',
        confirmPassword: 'xyz',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        // Должно быть несколько ошибок
        expect(result.error.issues.length).toBeGreaterThan(1);
      }
    });
  });
});
