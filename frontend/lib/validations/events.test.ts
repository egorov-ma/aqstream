import { describe, it, expect } from 'vitest';
import {
  ticketTypeSchema,
  baseEventFormSchema,
  eventFormSchema,
  validateEventCrossFields,
  defaultEventFormValues,
  type EventFormData,
} from './events';

describe('ticketTypeSchema', () => {
  it('валидирует корректный тип билета', () => {
    const validTicketType = {
      name: 'Стандартный билет',
      description: 'Базовый доступ',
      quantity: 100,
      sortOrder: 0,
    };

    const result = ticketTypeSchema.safeParse(validTicketType);
    expect(result.success).toBe(true);
  });

  it('требует имя', () => {
    const invalidTicketType = {
      name: '',
      sortOrder: 0,
    };

    const result = ticketTypeSchema.safeParse(invalidTicketType);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain('name');
    }
  });

  it('ограничивает длину имени 100 символами', () => {
    const invalidTicketType = {
      name: 'a'.repeat(101),
      sortOrder: 0,
    };

    const result = ticketTypeSchema.safeParse(invalidTicketType);
    expect(result.success).toBe(false);
  });

  it('количество должно быть больше 0', () => {
    const invalidTicketType = {
      name: 'Билет',
      quantity: 0,
      sortOrder: 0,
    };

    const result = ticketTypeSchema.safeParse(invalidTicketType);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain('quantity');
    }
  });

  it('допускает null для quantity', () => {
    const validTicketType = {
      name: 'Безлимитный билет',
      quantity: null,
      sortOrder: 0,
    };

    const result = ticketTypeSchema.safeParse(validTicketType);
    expect(result.success).toBe(true);
  });
});

describe('baseEventFormSchema', () => {
  it('валидирует минимально корректное событие', () => {
    const validEvent = {
      title: 'Тестовое событие',
      startsAt: '2025-01-15T10:00:00Z',
      timezone: 'Europe/Moscow',
      locationType: 'ONLINE',
      isPublic: false,
      participantsVisibility: 'CLOSED',
      ticketTypes: [],
    };

    const result = baseEventFormSchema.safeParse(validEvent);
    expect(result.success).toBe(true);
  });

  it('требует название', () => {
    const invalidEvent = {
      title: '',
      startsAt: '2025-01-15T10:00:00Z',
    };

    const result = baseEventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('title'))).toBe(true);
    }
  });

  it('ограничивает длину названия 255 символами', () => {
    const invalidEvent = {
      title: 'a'.repeat(256),
      startsAt: '2025-01-15T10:00:00Z',
    };

    const result = baseEventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
  });

  it('требует дату начала', () => {
    const invalidEvent = {
      title: 'Тест',
      startsAt: '',
    };

    const result = baseEventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('startsAt'))).toBe(true);
    }
  });

  it('ограничивает длину описания 10000 символами', () => {
    const invalidEvent = {
      title: 'Тест',
      startsAt: '2025-01-15T10:00:00Z',
      description: 'a'.repeat(10001),
    };

    const result = baseEventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
  });

  it('вместимость должна быть больше 0', () => {
    const invalidEvent = {
      title: 'Тест',
      startsAt: '2025-01-15T10:00:00Z',
      maxCapacity: 0,
    };

    const result = baseEventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('maxCapacity'))).toBe(true);
    }
  });
});

describe('eventFormSchema (с cross-field валидацией)', () => {
  const baseValidEvent = {
    title: 'Тестовое событие',
    startsAt: '2025-01-15T10:00:00Z',
    timezone: 'Europe/Moscow',
    locationType: 'ONLINE' as const,
    onlineUrl: 'https://zoom.us/j/123',
    isPublic: false,
    participantsVisibility: 'CLOSED' as const,
    ticketTypes: [],
  };

  it('требует URL для ONLINE события', () => {
    const invalidEvent = {
      ...baseValidEvent,
      locationType: 'ONLINE' as const,
      onlineUrl: '',
    };

    const result = eventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('onlineUrl'))).toBe(true);
    }
  });

  it('требует адрес для OFFLINE события', () => {
    const invalidEvent = {
      ...baseValidEvent,
      locationType: 'OFFLINE' as const,
      onlineUrl: undefined,
      locationAddress: '',
    };

    const result = eventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('locationAddress'))).toBe(true);
    }
  });

  it('требует и URL и адрес для HYBRID события', () => {
    const invalidEvent = {
      ...baseValidEvent,
      locationType: 'HYBRID' as const,
      onlineUrl: '',
      locationAddress: '',
    };

    const result = eventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join('.'));
      expect(paths.some((p) => p.includes('onlineUrl'))).toBe(true);
      expect(paths.some((p) => p.includes('locationAddress'))).toBe(true);
    }
  });

  it('endsAt должна быть после startsAt', () => {
    const invalidEvent = {
      ...baseValidEvent,
      startsAt: '2025-01-15T12:00:00Z',
      endsAt: '2025-01-15T10:00:00Z', // До начала
    };

    const result = eventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('endsAt'))).toBe(true);
    }
  });

  it('registrationClosesAt должна быть после registrationOpensAt', () => {
    const invalidEvent = {
      ...baseValidEvent,
      registrationOpensAt: '2025-01-10T12:00:00Z',
      registrationClosesAt: '2025-01-10T10:00:00Z', // До открытия
    };

    const result = eventFormSchema.safeParse(invalidEvent);
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('registrationClosesAt'))).toBe(true);
    }
  });
});

describe('validateEventCrossFields', () => {
  const baseValidData: EventFormData = {
    ...defaultEventFormValues,
    title: 'Тест',
    startsAt: '2025-01-15T10:00:00Z',
    locationType: 'ONLINE',
    onlineUrl: 'https://zoom.us/j/123',
  };

  it('возвращает пустой массив для валидных данных', () => {
    const errors = validateEventCrossFields(baseValidData);
    expect(errors).toHaveLength(0);
  });

  it('возвращает ошибку если ONLINE без URL', () => {
    const data: EventFormData = {
      ...baseValidData,
      locationType: 'ONLINE',
      onlineUrl: '',
    };

    const errors = validateEventCrossFields(data);
    expect(errors.some((e) => e.field === 'onlineUrl')).toBe(true);
  });

  it('возвращает ошибку если OFFLINE без адреса', () => {
    const data: EventFormData = {
      ...baseValidData,
      locationType: 'OFFLINE',
      onlineUrl: '',
      locationAddress: '',
    };

    const errors = validateEventCrossFields(data);
    expect(errors.some((e) => e.field === 'locationAddress')).toBe(true);
  });

  it('возвращает ошибку если endsAt < startsAt', () => {
    const data: EventFormData = {
      ...baseValidData,
      startsAt: '2025-01-15T12:00:00Z',
      endsAt: '2025-01-15T10:00:00Z',
    };

    const errors = validateEventCrossFields(data);
    expect(errors.some((e) => e.field === 'endsAt')).toBe(true);
  });

  it('возвращает ошибку если registrationClosesAt < registrationOpensAt', () => {
    const data: EventFormData = {
      ...baseValidData,
      registrationOpensAt: '2025-01-10T12:00:00Z',
      registrationClosesAt: '2025-01-10T10:00:00Z',
    };

    const errors = validateEventCrossFields(data);
    expect(errors.some((e) => e.field === 'registrationClosesAt')).toBe(true);
  });
});
