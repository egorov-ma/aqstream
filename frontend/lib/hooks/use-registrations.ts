import { useQuery } from '@tanstack/react-query';
import { registrationsApi, type RegistrationFilters } from '@/lib/api/registrations';

/**
 * Получить регистрации события (для организаторов)
 */
export function useEventRegistrations(eventId: string, filters?: RegistrationFilters) {
  return useQuery({
    queryKey: ['registrations', 'event', eventId, filters],
    queryFn: () => registrationsApi.listByEvent(eventId, filters),
    enabled: !!eventId,
  });
}

/**
 * Получить регистрацию по ID
 */
export function useRegistration(registrationId: string) {
  return useQuery({
    queryKey: ['registrations', registrationId],
    queryFn: () => registrationsApi.getById(registrationId),
    enabled: !!registrationId,
  });
}

/**
 * Получить мои регистрации
 */
export function useMyRegistrations(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['registrations', 'my', params],
    queryFn: () => registrationsApi.getMy(params),
  });
}
