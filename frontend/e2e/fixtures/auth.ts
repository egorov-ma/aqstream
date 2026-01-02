import { Page, expect } from '@playwright/test';

export interface TestUser {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

/**
 * Login with provided credentials
 * Navigates to /login, fills the form, and waits for redirect to dashboard
 */
export async function login(page: Page, user: TestUser): Promise<void> {
  await page.goto('/login');

  // Wait for form to be ready
  await expect(page.getByTestId('email-input')).toBeVisible({ timeout: 10000 });

  await page.getByTestId('email-input').fill(user.email);
  await page.getByTestId('password-input').fill(user.password);
  await page.getByTestId('login-submit').click();

  // Wait for successful redirect to dashboard or check for error
  try {
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
  } catch {
    // Check for error message
    const errorMessage = page.getByTestId('api-error-message');
    if (await errorMessage.isVisible()) {
      const text = await errorMessage.textContent();
      throw new Error(`Login failed: ${text}`);
    }
    throw new Error(`Login failed: stayed on ${page.url()}`);
  }
}

/**
 * Logout from the application
 * Clicks user menu and selects logout
 */
export async function logout(page: Page): Promise<void> {
  // Open user menu (button with initials like "TO", "TU", etc.)
  const userMenuButton = page.locator('button[aria-haspopup="menu"]').last();
  await userMenuButton.click();

  // Click logout menu item
  await page.getByRole('menuitem', { name: /выйти/i }).click();

  // Wait for redirect to login
  await expect(page).toHaveURL('/login', { timeout: 10000 });
}

/**
 * Check if user is authenticated by looking for dashboard navigation
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  const dashboardNav = page.getByRole('navigation');
  return dashboardNav.isVisible();
}
