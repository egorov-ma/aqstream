/**
 * Test data for E2E tests
 * These users must exist in the test database (created by Docker seed data)
 *
 * Роли (согласно docs/business/role-model.md):
 * - owner: Админ платформы (is_admin=true) + Владелец организации
 * - user: Обычный пользователь платформы
 */
export const testUsers = {
  /**
   * Админ платформы + Владелец организации "Test Organization"
   * Используй для: одобрения заявок на организации, управления платформой
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
