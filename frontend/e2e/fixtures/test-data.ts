/**
 * Test data for E2E tests
 * These users must exist in the test database (created by Docker seed data)
 *
 * Роли (согласно docs/business/role-model.md):
 * - admin: Платформенный администратор (is_admin=true) — одобряет заявки на организации
 * - owner: Владелец организации — управляет своей организацией, событиями, группами
 * - user: Обычный пользователь платформы — регистрируется на события, подаёт заявки
 */
export const testUsers = {
  /**
   * Платформенный администратор (is_admin=true)
   * Используй для: одобрения/отклонения заявок на создание организаций
   */
  admin: {
    email: 'admin@test.local',
    password: '123123Qw!',
    firstName: 'Test',
    lastName: 'Admin',
  },
  /**
   * Владелец организации "Test Organization"
   * Используй для: создания событий, управления билетами, check-in
   * НЕ является платформенным админом (is_admin=false)
   */
  owner: {
    email: 'owner@test.local',
    password: '123123Qw!',
    firstName: 'Test',
    lastName: 'Owner',
  },
  /**
   * Обычный пользователь платформы
   * Используй для: регистрации на события, подачи заявок на организации
   */
  user: {
    email: 'user@test.local',
    password: '123123Qw!',
    firstName: 'Test',
    lastName: 'User',
  },
} as const;

export const testOrganization = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Test Organization',
  slug: 'test-organization',
} as const;

/**
 * Generate unique event title to avoid conflicts
 */
export function generateEventTitle(): string {
  const timestamp = Date.now();
  return `E2E Test Event ${timestamp}`;
}

/**
 * Generate future date for event (15 days from now)
 */
export function getFutureDate(): Date {
  const date = new Date();
  date.setDate(date.getDate() + 15);
  date.setHours(12, 0, 0, 0);
  return date;
}
