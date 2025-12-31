import { describe, it, expect } from 'vitest';
import {
  registrationSchema,
  validateCustomFields,
  getDefaultRegistrationValues,
} from './registration';
import type { CustomFieldConfig, RegistrationFormConfig } from '@/lib/api/types';

describe('registrationSchema', () => {
  it('validates correct data with all fields', () => {
    const result = registrationSchema.safeParse({
      firstName: 'Иван',
      lastName: 'Иванов',
      email: 'ivan@example.com',
      ticketTypeId: '123e4567-e89b-12d3-a456-426614174000',
      customFields: { company: 'Acme' },
    });
    expect(result.success).toBe(true);
  });

  it('validates correct data without optional fields', () => {
    const result = registrationSchema.safeParse({
      firstName: 'Иван',
      email: 'ivan@example.com',
      ticketTypeId: '123e4567-e89b-12d3-a456-426614174000',
    });
    expect(result.success).toBe(true);
  });

  it('rejects empty firstName', () => {
    const result = registrationSchema.safeParse({
      firstName: '',
      email: 'ivan@example.com',
      ticketTypeId: '123e4567-e89b-12d3-a456-426614174000',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Имя обязательно');
    }
  });

  it('rejects invalid email', () => {
    const result = registrationSchema.safeParse({
      firstName: 'Иван',
      email: 'invalid',
      ticketTypeId: '123e4567-e89b-12d3-a456-426614174000',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Некорректный email');
    }
  });

  it('rejects empty ticketTypeId', () => {
    const result = registrationSchema.safeParse({
      firstName: 'Иван',
      email: 'ivan@example.com',
      ticketTypeId: '',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Выберите тип билета');
    }
  });
});

describe('validateCustomFields', () => {
  const textField: CustomFieldConfig = {
    name: 'company',
    label: 'Компания',
    type: 'text',
    required: true,
  };

  const emailField: CustomFieldConfig = {
    name: 'workEmail',
    label: 'Рабочий email',
    type: 'email',
    required: false,
  };

  const telField: CustomFieldConfig = {
    name: 'phone',
    label: 'Телефон',
    type: 'tel',
    required: false,
  };

  const selectField: CustomFieldConfig = {
    name: 'diet',
    label: 'Питание',
    type: 'select',
    required: true,
    options: ['Обычное', 'Вегетарианское', 'Веганское'],
  };

  it('returns null for valid required text field', () => {
    const errors = validateCustomFields({ company: 'Acme' }, [textField]);
    expect(errors).toBeNull();
  });

  it('returns error for empty required field', () => {
    const errors = validateCustomFields({ company: '' }, [textField]);
    expect(errors).not.toBeNull();
    expect(errors?.company).toBe('Компания обязательно');
  });

  it('returns error for missing required field', () => {
    const errors = validateCustomFields({}, [textField]);
    expect(errors).not.toBeNull();
    expect(errors?.company).toBe('Компания обязательно');
  });

  it('returns null for valid email field', () => {
    const errors = validateCustomFields({ workEmail: 'work@example.com' }, [emailField]);
    expect(errors).toBeNull();
  });

  it('returns error for invalid email field', () => {
    const errors = validateCustomFields({ workEmail: 'invalid' }, [emailField]);
    expect(errors).not.toBeNull();
    expect(errors?.workEmail).toBe('Некорректный email');
  });

  it('returns null for empty optional email field', () => {
    const errors = validateCustomFields({ workEmail: '' }, [emailField]);
    expect(errors).toBeNull();
  });

  it('returns null for valid phone field', () => {
    const errors = validateCustomFields({ phone: '+7 (999) 123-45-67' }, [telField]);
    expect(errors).toBeNull();
  });

  it('returns error for invalid phone field', () => {
    const errors = validateCustomFields({ phone: '123' }, [telField]);
    expect(errors).not.toBeNull();
    expect(errors?.phone).toBe('Некорректный номер телефона');
  });

  it('returns null for valid select field', () => {
    const errors = validateCustomFields({ diet: 'Вегетарианское' }, [selectField]);
    expect(errors).toBeNull();
  });

  it('returns error for invalid select option', () => {
    const errors = validateCustomFields({ diet: 'Фастфуд' }, [selectField]);
    expect(errors).not.toBeNull();
    expect(errors?.diet).toBe('Выберите значение из списка');
  });

  it('validates multiple fields', () => {
    const config: CustomFieldConfig[] = [textField, selectField];

    // Все валидно
    const validErrors = validateCustomFields(
      { company: 'Acme', diet: 'Обычное' },
      config
    );
    expect(validErrors).toBeNull();

    // Один невалидный
    const invalidErrors = validateCustomFields(
      { company: '', diet: 'Обычное' },
      config
    );
    expect(invalidErrors).not.toBeNull();
    expect(invalidErrors?.company).toBeDefined();
    expect(invalidErrors?.diet).toBeUndefined();
  });
});

describe('getDefaultRegistrationValues', () => {
  it('returns empty defaults without config', () => {
    const defaults = getDefaultRegistrationValues(undefined, undefined);

    expect(defaults.firstName).toBe('');
    expect(defaults.lastName).toBe('');
    expect(defaults.email).toBe('');
    expect(defaults.ticketTypeId).toBe('');
    expect(defaults.customFields).toEqual({});
  });

  it('returns prefilled values', () => {
    const defaults = getDefaultRegistrationValues(undefined, {
      firstName: 'Иван',
      lastName: 'Иванов',
      email: 'ivan@example.com',
    });

    expect(defaults.firstName).toBe('Иван');
    expect(defaults.lastName).toBe('Иванов');
    expect(defaults.email).toBe('ivan@example.com');
  });

  it('initializes custom fields from config', () => {
    const config: RegistrationFormConfig = {
      customFields: [
        { name: 'company', label: 'Компания', type: 'text', required: true },
        { name: 'diet', label: 'Питание', type: 'select', required: false, options: [] },
      ],
    };

    const defaults = getDefaultRegistrationValues(config, undefined);

    expect(defaults.customFields).toEqual({
      company: '',
      diet: '',
    });
  });

  it('uses prefilled custom field values', () => {
    const config: RegistrationFormConfig = {
      customFields: [
        { name: 'company', label: 'Компания', type: 'text', required: true },
      ],
    };

    const defaults = getDefaultRegistrationValues(config, {
      customFields: { company: 'Acme' },
    });

    expect(defaults.customFields).toEqual({
      company: 'Acme',
    });
  });
});
