import type { RegistrationStatus } from '@/lib/api/types';

/**
 * Варианты Badge для статусов регистрации.
 */
export function getStatusBadgeVariant(
  status: RegistrationStatus
): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status) {
    case 'CONFIRMED':
    case 'CHECKED_IN':
      return 'default';
    case 'CANCELLED':
      return 'destructive';
    case 'RESERVED':
    case 'PENDING':
      return 'secondary';
    case 'EXPIRED':
      return 'outline';
    default:
      return 'outline';
  }
}

/**
 * Названия статусов регистрации на русском.
 */
export function getStatusLabel(status: RegistrationStatus): string {
  switch (status) {
    case 'CONFIRMED':
      return 'Подтверждена';
    case 'CHECKED_IN':
      return 'Отмечена';
    case 'CANCELLED':
      return 'Отменена';
    case 'RESERVED':
      return 'Забронирована';
    case 'PENDING':
      return 'Ожидает оплаты';
    case 'EXPIRED':
      return 'Истекла';
    default:
      return status;
  }
}

/**
 * Проверяет, можно ли отменить регистрацию.
 */
export function canCancelRegistration(status: RegistrationStatus): boolean {
  return status === 'CONFIRMED' || status === 'RESERVED';
}
