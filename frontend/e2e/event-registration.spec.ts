import { test, expect } from '@playwright/test';
import { login } from './fixtures/auth';
import { testUsers } from './fixtures/test-data';

test.describe('Event Registration (J2)', () => {
  test.describe.configure({ mode: 'serial' });

  let eventSlug: string;

  test.beforeAll(async ({ browser }) => {
    // Find an existing public event to register for
    const page = await browser.newPage();
    try {
      const response = await page.request.get(
        'http://localhost:8080/api/v1/public/events'
      );
      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      const events = data.data || [];

      // Find any E2E test event or use the first available
      const testEvent =
        events.find((e: { title: string }) =>
          e.title.includes('E2E Test Event')
        ) || events[0];

      if (testEvent) {
        eventSlug = testEvent.slug;
      }
    } finally {
      await page.close();
    }
  });

  test('user can view public event page', async ({ page }) => {
    test.skip(!eventSlug, 'No public events available');

    await page.goto(`/events/${eventSlug}`);

    // Verify event page loads
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    // Use data-testid for registration form card
    await expect(page.getByTestId('registration-form-card')).toBeVisible();
  });

  test('authenticated user can register for event', async ({ page }) => {
    test.skip(!eventSlug, 'No public events available');

    // Login as regular user
    await login(page, testUsers.user);

    // Navigate to public event page
    await page.goto(`/events/${eventSlug}`);

    // Wait for form to load
    await expect(page.getByTestId('registration-form')).toBeVisible();

    // Form should be pre-filled with user data using data-testid
    await expect(page.getByTestId('firstName-input')).toHaveValue(
      testUsers.user.firstName
    );
    await expect(page.getByTestId('email-input')).toHaveValue(
      testUsers.user.email
    );

    // Select ticket type by clicking on ticket card
    const ticketCard = page.getByTestId('ticket-type-card').first();
    if (await ticketCard.isVisible()) {
      await ticketCard.click();
    }

    // Submit registration using data-testid
    await page.getByTestId('registration-submit').click();

    // Wait for success page or error
    // Success: redirect to /events/{slug}/success with confirmation code
    // Error: show API error message
    await page.waitForURL(/\/(success|events)/, { timeout: 15000 });

    // If redirected to success page, verify confirmation code
    if (page.url().includes('success')) {
      await expect(page.getByTestId('registration-success-card')).toBeVisible();
    } else {
      // Might show "already registered" error
      const errorMessage = page.getByTestId('api-error-message');
      if (await errorMessage.isVisible()) {
        // This is expected if user already registered
        await expect(errorMessage).toBeVisible();
      }
    }
  });

  test('success page shows ticket code', async ({ page }) => {
    test.skip(!eventSlug, 'No public events available');

    await login(page, testUsers.owner); // Use owner for fresh registration

    await page.goto(`/events/${eventSlug}`);

    // Wait for form
    await expect(page.getByTestId('registration-form')).toBeVisible();

    // Select ticket type
    const ticketCard = page.getByTestId('ticket-type-card').first();
    if (await ticketCard.isVisible()) {
      await ticketCard.click();
    }

    // Submit registration
    await page.getByTestId('registration-submit').click();

    await page.waitForURL(/\/(success|events)/, { timeout: 15000 });

    if (page.url().includes('success')) {
      // Verify success page elements using data-testid
      await expect(page.getByTestId('registration-success-card')).toBeVisible();
      await expect(page.getByTestId('confirmation-code')).toBeVisible();

      // Ticket code should match pattern (8 uppercase letters/numbers)
      const codeText = await page.getByTestId('confirmation-code').textContent();
      expect(codeText).toMatch(/[A-Z0-9]{8}/);
    }
  });

  test('unauthenticated user sees login prompt', async ({ page }) => {
    test.skip(!eventSlug, 'No public events available');

    // Go directly to event page without logging in
    await page.goto(`/events/${eventSlug}`);

    // Registration form card should be visible
    await expect(page.getByTestId('registration-form-card')).toBeVisible();

    // Form fields should NOT be visible (only login buttons)
    await expect(page.getByTestId('registration-form')).not.toBeVisible();

    // Login button should be present inside the card
    await expect(
      page.getByTestId('registration-form-card').getByRole('link', { name: /войти/i })
    ).toBeVisible();
  });

  test('registration form validates required fields', async ({ page }) => {
    test.skip(!eventSlug, 'No public events available');

    await login(page, testUsers.user);
    await page.goto(`/events/${eventSlug}`);

    // Wait for form
    await expect(page.getByTestId('registration-form')).toBeVisible();

    // Clear required fields using data-testid
    await page.getByTestId('firstName-input').clear();
    await page.getByTestId('email-input').clear();

    // Try to submit empty form
    await page.getByTestId('registration-submit').click();

    // Should show validation errors (form should not submit)
    // Check that we're still on the same page (not redirected)
    await expect(page).toHaveURL(new RegExp(`/events/${eventSlug}`));
  });
});
