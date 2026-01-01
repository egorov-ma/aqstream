import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  TIMEZONE_OPTIONS,
  formatEventDate,
  formatDateOnly,
  formatTimeOnly,
  getTimezoneLabel,
  combineDateAndTime,
  extractDatePart,
  extractTimePart,
  isValidDateString,
  getCurrentISODate,
  isFutureDate,
  isPastDate,
} from './date-time';

describe('TIMEZONE_OPTIONS', () => {
  it('contains all Russian timezones', () => {
    expect(TIMEZONE_OPTIONS).toHaveLength(11);
    expect(TIMEZONE_OPTIONS[0].value).toBe('Europe/Kaliningrad');
    expect(TIMEZONE_OPTIONS[1].value).toBe('Europe/Moscow');
  });

  it('has labels in Russian', () => {
    const moscowOption = TIMEZONE_OPTIONS.find((tz) => tz.value === 'Europe/Moscow');
    expect(moscowOption?.label).toBe('Москва (UTC+3)');
  });
});

describe('formatEventDate', () => {
  it('formats valid ISO date', () => {
    const result = formatEventDate('2025-06-15T10:30:00.000Z');
    expect(result).toMatch(/15 июн/i);
    expect(result).toMatch(/2025/);
  });

  it('returns empty string for null', () => {
    expect(formatEventDate(null)).toBe('');
  });

  it('returns empty string for undefined', () => {
    expect(formatEventDate(undefined)).toBe('');
  });

  it('returns empty string for invalid date', () => {
    expect(formatEventDate('invalid-date')).toBe('');
  });
});

describe('formatDateOnly', () => {
  it('formats valid ISO date without time', () => {
    const result = formatDateOnly('2025-06-15T10:30:00.000Z');
    expect(result).toMatch(/15 июн/i);
    expect(result).toMatch(/2025/);
    expect(result).not.toMatch(/10:30/);
  });

  it('returns empty string for null', () => {
    expect(formatDateOnly(null)).toBe('');
  });

  it('returns empty string for invalid date', () => {
    expect(formatDateOnly('not-a-date')).toBe('');
  });
});

describe('formatTimeOnly', () => {
  it('formats time from ISO date', () => {
    const result = formatTimeOnly('2025-06-15T10:30:00.000Z');
    // Время зависит от локального часового пояса, но формат должен быть HH:mm
    expect(result).toMatch(/^\d{2}:\d{2}$/);
  });

  it('returns empty string for null', () => {
    expect(formatTimeOnly(null)).toBe('');
  });

  it('returns empty string for invalid date', () => {
    expect(formatTimeOnly('not-a-date')).toBe('');
  });
});

describe('getTimezoneLabel', () => {
  it('returns label for known timezone', () => {
    expect(getTimezoneLabel('Europe/Moscow')).toBe('Москва (UTC+3)');
    expect(getTimezoneLabel('Asia/Vladivostok')).toBe('Владивосток (UTC+10)');
  });

  it('returns timezone value for unknown timezone', () => {
    expect(getTimezoneLabel('America/New_York')).toBe('America/New_York');
  });
});

describe('combineDateAndTime', () => {
  it('combines date and time into ISO string', () => {
    const result = combineDateAndTime('2025-06-15', '10:30');
    expect(result).toMatch(/2025-06-15/);
    expect(result).toMatch(/T\d{2}:\d{2}:\d{2}/);
  });

  it('uses 00:00 when time is empty', () => {
    const result = combineDateAndTime('2025-06-15', '');
    expect(result).toBeTruthy();
  });

  it('returns empty string when date is empty', () => {
    expect(combineDateAndTime('', '10:30')).toBe('');
  });
});

describe('extractDatePart', () => {
  it('extracts date in YYYY-MM-DD format', () => {
    // Учитываем локальную таймзону
    const result = extractDatePart('2025-06-15T10:30:00.000Z');
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('returns empty string for null', () => {
    expect(extractDatePart(null)).toBe('');
  });

  it('returns empty string for invalid date', () => {
    expect(extractDatePart('invalid')).toBe('');
  });
});

describe('extractTimePart', () => {
  it('extracts time in HH:mm format', () => {
    const result = extractTimePart('2025-06-15T10:30:00.000Z');
    expect(result).toMatch(/^\d{2}:\d{2}$/);
  });

  it('returns empty string for null', () => {
    expect(extractTimePart(null)).toBe('');
  });

  it('returns empty string for invalid date', () => {
    expect(extractTimePart('invalid')).toBe('');
  });
});

describe('isValidDateString', () => {
  it('returns true for valid ISO date', () => {
    expect(isValidDateString('2025-06-15T10:30:00.000Z')).toBe(true);
  });

  it('returns true for date only', () => {
    expect(isValidDateString('2025-06-15')).toBe(true);
  });

  it('returns false for null', () => {
    expect(isValidDateString(null)).toBe(false);
  });

  it('returns false for undefined', () => {
    expect(isValidDateString(undefined)).toBe(false);
  });

  it('returns false for invalid date', () => {
    expect(isValidDateString('not-a-date')).toBe(false);
  });
});

describe('getCurrentISODate', () => {
  it('returns current date in ISO format', () => {
    const result = getCurrentISODate();
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/);
  });

  it('returns date close to now', () => {
    const result = getCurrentISODate();
    const resultDate = new Date(result);
    const now = new Date();
    // Разница не более 1 секунды
    expect(Math.abs(resultDate.getTime() - now.getTime())).toBeLessThan(1000);
  });
});

describe('isFutureDate', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2025-06-15T12:00:00.000Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns true for future date', () => {
    expect(isFutureDate('2025-12-31T00:00:00.000Z')).toBe(true);
  });

  it('returns false for past date', () => {
    expect(isFutureDate('2025-01-01T00:00:00.000Z')).toBe(false);
  });

  it('returns false for null', () => {
    expect(isFutureDate(null)).toBe(false);
  });

  it('returns false for invalid date', () => {
    expect(isFutureDate('invalid')).toBe(false);
  });
});

describe('isPastDate', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2025-06-15T12:00:00.000Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns true for past date', () => {
    expect(isPastDate('2025-01-01T00:00:00.000Z')).toBe(true);
  });

  it('returns false for future date', () => {
    expect(isPastDate('2025-12-31T00:00:00.000Z')).toBe(false);
  });

  it('returns false for null', () => {
    expect(isPastDate(null)).toBe(false);
  });

  it('returns false for invalid date', () => {
    expect(isPastDate('invalid')).toBe(false);
  });
});
