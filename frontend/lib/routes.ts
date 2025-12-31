/**
 * Константы путей приложения
 * Централизованное управление маршрутами для consistency
 */

export const ROUTES = {
  // Public
  HOME: '/',
  EVENTS: '/events',
  EVENT: (slug: string) => `/events/${slug}`,
  EVENT_SUCCESS: (slug: string, code: string) => `/events/${slug}/success?code=${code}`,

  // Auth
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',
  RESET_PASSWORD: '/reset-password',
  VERIFY_EMAIL: '/verify-email',

  // Dashboard
  DASHBOARD: '/dashboard',
  MY_REGISTRATIONS: '/dashboard/my-registrations',
  MY_EVENTS: '/dashboard/events',
  CREATE_EVENT: '/dashboard/events/new',
  EDIT_EVENT: (id: string) => `/dashboard/events/${id}/edit`,
  EVENT_DETAILS: (id: string) => `/dashboard/events/${id}`,
  SETTINGS: '/dashboard/settings',
} as const;

/**
 * Генерирует URL с redirect параметром для возврата после логина
 */
export function getLoginUrl(redirect?: string): string {
  if (redirect) {
    return `${ROUTES.LOGIN}?redirect=${encodeURIComponent(redirect)}`;
  }
  return ROUTES.LOGIN;
}

/**
 * Генерирует URL регистрации с redirect параметром
 */
export function getRegisterUrl(redirect?: string): string {
  if (redirect) {
    return `${ROUTES.REGISTER}?redirect=${encodeURIComponent(redirect)}`;
  }
  return ROUTES.REGISTER;
}
