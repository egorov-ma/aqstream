import { test, expect } from '@playwright/test';
import { login, logout } from './fixtures/auth';
import { testUsers, generateEventTitle } from './fixtures/test-data';

test.describe('Event Management (J1)', () => {
  test.describe.configure({ mode: 'serial' });

  let eventSlug: string;
  const eventTitle = generateEventTitle();

  test.beforeAll(async ({ browser }) => {
    // Verify Docker stack is running by checking API health
    const page = await browser.newPage();
    try {
      const response = await page.request.get('http://localhost:8080/actuator/health');
      expect(response.ok()).toBeTruthy();
    } finally {
      await page.close();
    }
  });

  test('owner can login', async ({ page }) => {
    await login(page, testUsers.owner);
    await expect(page.getByRole('heading', { name: /обзор/i })).toBeVisible();
  });

  test('owner can navigate to create event page', async ({ page }) => {
    await login(page, testUsers.owner);

    await page.getByRole('link', { name: /создать событие/i }).click();
    await expect(page).toHaveURL(/\/dashboard\/events\/new/);
    await expect(
      page.getByRole('heading', { name: /создание события/i })
    ).toBeVisible();

    // Verify form is present using data-testid
    await expect(page.getByTestId('event-form')).toBeVisible();
  });

  test('owner can create and publish an event', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto('/dashboard/events/new');

    // Fill event title using data-testid
    await page.getByTestId('event-title-input').fill(eventTitle);

    // Open date picker using data-testid for button
    await page.getByTestId('event-starts-at-picker-button').click();

    // Wait for calendar to be visible
    await expect(page.locator('[data-slot="calendar"]')).toBeVisible();

    // Navigate to next month
    await page.getByRole('button', { name: /next month/i }).click();

    // Click on day 15
    await page.getByRole('gridcell', { name: '15' }).click();

    // Fill streaming URL (required for online events)
    await page
      .getByLabel(/ссылка на трансляцию/i)
      .fill('https://meet.example.com/test');

    // Enable public event
    const publicSwitch = page.getByRole('switch', {
      name: /публичное событие/i,
    });
    await publicSwitch.click();
    await expect(publicSwitch).toBeChecked();

    // Add ticket type using data-testid
    await page.getByTestId('ticket-type-add-button').click();

    // Fill ticket name using data-testid (first ticket = index 0)
    await page.getByTestId('ticket-type-name-0').fill('Free Admission');

    // Publish event using data-testid
    await page.getByTestId('event-publish-submit').click();

    // Wait for success - check for status badge with data-testid
    await expect(page.getByTestId('event-status-published')).toBeVisible({
      timeout: 15000,
    });

    // Extract event ID from URL for later tests
    const url = page.url();
    const match = url.match(/\/events\/([^/]+)$/);
    if (match) {
      eventSlug = match[1];
    }
  });

  test('published event is accessible via public API', async ({ page }) => {
    // This test verifies the event was actually created in the backend
    const response = await page.request.get(
      'http://localhost:8080/api/v1/public/events'
    );
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    const events = data.data || [];

    // Find our event by title
    const ourEvent = events.find((e: { title: string }) =>
      e.title.includes('E2E Test Event')
    );
    expect(ourEvent).toBeTruthy();
    expect(ourEvent.slug).toBeTruthy();

    // Store slug for registration test
    eventSlug = ourEvent.slug;
  });

  test('public event page is accessible', async ({ page }) => {
    // Use the slug from previous test or fetch it
    if (!eventSlug) {
      const response = await page.request.get(
        'http://localhost:8080/api/v1/public/events'
      );
      const data = await response.json();
      const events = data.data || [];
      const ourEvent = events.find((e: { title: string }) =>
        e.title.includes('E2E Test Event')
      );
      eventSlug = ourEvent?.slug || 'e2e-test-event-january-2026';
    }

    await page.goto(`/events/${eventSlug}`);

    // Verify event page content
    await expect(page.getByRole('heading', { level: 1 })).toContainText(
      /e2e test event/i
    );
    // Check registration form is present using data-testid
    await expect(page.getByTestId('registration-form-card')).toBeVisible();
  });

  test('owner can logout', async ({ page }) => {
    await login(page, testUsers.owner);
    await logout(page);
    await expect(page).toHaveURL('/login');
  });
});

test.describe('Event Cancellation (J5)', () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async ({ browser }) => {
    // Проверяем, что Docker стек работает
    const page = await browser.newPage();
    try {
      const response = await page.request.get('http://localhost:8080/actuator/health');
      expect(response.ok()).toBeTruthy();
    } finally {
      await page.close();
    }
  });

  test('owner can see cancel option for published event', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto('/dashboard/events');

    // Ожидаем загрузки таблицы событий
    await expect(page.getByTestId('events-table')).toBeVisible({ timeout: 10000 });

    // Находим кнопку действий для опубликованного события
    // Ищем строку с текстом "Опубликовано" и кликаем на кнопку действий в этой строке
    const publishedRow = page.locator('tr', { hasText: 'Опубликовано' }).first();
    await publishedRow.getByRole('button', { name: /действия/i }).click();

    // Проверяем, что опция "Отменить событие" видна
    await expect(page.getByTestId('event-cancel-menu-item')).toBeVisible();
  });

  test('cancel dialog shows correctly', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto('/dashboard/events');

    // Ожидаем загрузки таблицы
    await expect(page.getByTestId('events-table')).toBeVisible({ timeout: 10000 });

    // Открываем меню действий для опубликованного события
    const publishedRow = page.locator('tr', { hasText: 'Опубликовано' }).first();
    await publishedRow.getByRole('button', { name: /действия/i }).click();

    // Кликаем на "Отменить событие"
    await page.getByTestId('event-cancel-menu-item').click();

    // Проверяем, что диалог появился
    await expect(page.getByRole('alertdialog')).toBeVisible();
    await expect(page.getByText(/отменить событие\?/i)).toBeVisible();
    await expect(page.getByText(/все регистрации будут отменены/i)).toBeVisible();

    // Проверяем наличие поля для причины отмены
    await expect(page.getByTestId('cancel-reason-input')).toBeVisible();

    // Проверяем наличие кнопок
    await expect(page.getByTestId('cancel-dialog-back')).toBeVisible();
    await expect(page.getByTestId('cancel-dialog-confirm')).toBeVisible();
  });

  test('owner can cancel an event with reason', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto('/dashboard/events');

    // Ожидаем загрузки таблицы
    await expect(page.getByTestId('events-table')).toBeVisible({ timeout: 10000 });

    // Открываем меню действий для опубликованного события
    const publishedRow = page.locator('tr', { hasText: 'Опубликовано' }).first();

    // Проверяем, что есть опубликованное событие
    const publishedCount = await page.locator('tr', { hasText: 'Опубликовано' }).count();
    if (publishedCount === 0) {
      test.skip();
      return;
    }

    await publishedRow.getByRole('button', { name: /действия/i }).click();

    // Кликаем на "Отменить событие"
    await page.getByTestId('event-cancel-menu-item').click();

    // Вводим причину отмены
    await page.getByTestId('cancel-reason-input').fill('E2E тест: событие отменено');

    // Подтверждаем отмену
    await page.getByTestId('cancel-dialog-confirm').click();

    // Ожидаем изменения статуса на "Отменено" (используем testid для badge)
    await expect(page.getByTestId('event-status-cancelled').first()).toBeVisible({ timeout: 10000 });
  });

  test('cancelled event cannot be cancelled again', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto('/dashboard/events');

    // Ожидаем загрузки таблицы
    await expect(page.getByTestId('events-table')).toBeVisible({ timeout: 10000 });

    // Находим отменённое событие
    const cancelledRow = page.locator('tr', { hasText: 'Отменено' }).first();
    const cancelledCount = await page.locator('tr', { hasText: 'Отменено' }).count();

    if (cancelledCount === 0) {
      test.skip();
      return;
    }

    // Открываем меню действий
    await cancelledRow.getByRole('button', { name: /действия/i }).click();

    // Проверяем, что опция "Отменить событие" НЕ видна
    await expect(page.getByTestId('event-cancel-menu-item')).not.toBeVisible();
  });
});
