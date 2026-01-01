import { describe, it, expect } from 'vitest';
import type { RegistrationStatus } from '@/lib/api/types';
import { getStatusBadgeVariant, getStatusLabel, canCancelRegistration } from './registration';

describe('getStatusBadgeVariant', () => {
  it('returns default for CONFIRMED', () => {
    expect(getStatusBadgeVariant('CONFIRMED')).toBe('default');
  });

  it('returns default for CHECKED_IN', () => {
    expect(getStatusBadgeVariant('CHECKED_IN')).toBe('default');
  });

  it('returns destructive for CANCELLED', () => {
    expect(getStatusBadgeVariant('CANCELLED')).toBe('destructive');
  });

  it('returns secondary for RESERVED', () => {
    expect(getStatusBadgeVariant('RESERVED')).toBe('secondary');
  });

  it('returns secondary for PENDING', () => {
    expect(getStatusBadgeVariant('PENDING')).toBe('secondary');
  });

  it('returns outline for EXPIRED', () => {
    expect(getStatusBadgeVariant('EXPIRED')).toBe('outline');
  });

  it('returns outline for unknown status', () => {
    expect(getStatusBadgeVariant('UNKNOWN' as RegistrationStatus)).toBe('outline');
  });
});

describe('getStatusLabel', () => {
  it('returns Russian label for CONFIRMED', () => {
    expect(getStatusLabel('CONFIRMED')).toBe('Подтверждена');
  });

  it('returns Russian label for CHECKED_IN', () => {
    expect(getStatusLabel('CHECKED_IN')).toBe('Отмечена');
  });

  it('returns Russian label for CANCELLED', () => {
    expect(getStatusLabel('CANCELLED')).toBe('Отменена');
  });

  it('returns Russian label for RESERVED', () => {
    expect(getStatusLabel('RESERVED')).toBe('Забронирована');
  });

  it('returns Russian label for PENDING', () => {
    expect(getStatusLabel('PENDING')).toBe('Ожидает оплаты');
  });

  it('returns Russian label for EXPIRED', () => {
    expect(getStatusLabel('EXPIRED')).toBe('Истекла');
  });

  it('returns status as-is for unknown status', () => {
    expect(getStatusLabel('UNKNOWN' as RegistrationStatus)).toBe('UNKNOWN');
  });
});

describe('canCancelRegistration', () => {
  it('returns true for CONFIRMED', () => {
    expect(canCancelRegistration('CONFIRMED')).toBe(true);
  });

  it('returns true for RESERVED', () => {
    expect(canCancelRegistration('RESERVED')).toBe(true);
  });

  it('returns false for CANCELLED', () => {
    expect(canCancelRegistration('CANCELLED')).toBe(false);
  });

  it('returns false for CHECKED_IN', () => {
    expect(canCancelRegistration('CHECKED_IN')).toBe(false);
  });

  it('returns false for PENDING', () => {
    expect(canCancelRegistration('PENDING')).toBe(false);
  });

  it('returns false for EXPIRED', () => {
    expect(canCancelRegistration('EXPIRED')).toBe(false);
  });
});
