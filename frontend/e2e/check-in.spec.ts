import { test, expect } from '@playwright/test';
import { login } from './fixtures/auth';
import { testUsers } from './fixtures/test-data';

test.describe('Check-in (J4)', () => {
  test.describe.configure({ mode: 'serial' });

  // Будем использовать существующую регистрацию из БД
  // Confirmation code: KTP56DG5 (Test Owner, E2E Test Event)
  const confirmationCode = 'KTP56DG5';
  const eventId = 'af3c2f83-b948-4321-9011-a079a5990e6f';

  test.beforeAll(async ({ browser }) => {
    // Проверяем, что Docker stack работает
    const page = await browser.newPage();
    try {
      const response = await page.request.get('http://localhost:8080/actuator/health');
      expect(response.ok()).toBeTruthy();
    } finally {
      await page.close();
    }
  });

  test('owner can access check-in page', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto(`/dashboard/events/${eventId}/check-in`);

    // Проверяем заголовок страницы
    await expect(page.getByRole('heading', { name: /check-in/i })).toBeVisible();

    // Проверяем наличие табов
    await expect(page.getByRole('tab', { name: /сканер/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /вручную/i })).toBeVisible();
  });

  test('owner can switch to manual search tab', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto(`/dashboard/events/${eventId}/check-in`);

    // Переключаемся на вкладку "Вручную"
    await page.getByRole('tab', { name: /вручную/i }).click();

    // Проверяем, что форма поиска появилась
    await expect(page.getByTestId('check-in-search-form')).toBeVisible();
    await expect(page.getByTestId('check-in-code-input')).toBeVisible();
    await expect(page.getByTestId('check-in-search-button')).toBeVisible();
  });

  test('owner can search for registration by code', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto(`/dashboard/events/${eventId}/check-in`);

    // Переключаемся на вкладку "Вручную"
    await page.getByRole('tab', { name: /вручную/i }).click();

    // Вводим код подтверждения
    await page.getByTestId('check-in-code-input').fill(confirmationCode);

    // Нажимаем кнопку поиска
    await page.getByTestId('check-in-search-button').click();

    // Ждём появления карточки участника
    await expect(page.getByTestId('attendee-card')).toBeVisible({ timeout: 10000 });

    // Проверяем, что отображается имя участника
    await expect(page.getByTestId('attendee-name')).toContainText(/Test/);
  });

  test('invalid code shows error message', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto(`/dashboard/events/${eventId}/check-in`);

    // Переключаемся на вкладку "Вручную"
    await page.getByRole('tab', { name: /вручную/i }).click();

    // Вводим неверный код
    await page.getByTestId('check-in-code-input').fill('INVALID1');
    await page.getByTestId('check-in-search-button').click();

    // Ожидаем сообщение об ошибке
    await expect(page.getByText(/не найдена|недействителен/i)).toBeVisible({
      timeout: 10000,
    });
  });

  test('owner can perform check-in', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto(`/dashboard/events/${eventId}/check-in`);

    // Переключаемся на вкладку "Вручную"
    await page.getByRole('tab', { name: /вручную/i }).click();

    // Вводим код подтверждения
    await page.getByTestId('check-in-code-input').fill(confirmationCode);
    await page.getByTestId('check-in-search-button').click();

    // Ждём появления карточки участника
    await expect(page.getByTestId('attendee-card')).toBeVisible({ timeout: 10000 });

    // Проверяем статус (может быть уже отмечен или ожидает)
    const checkedInBadge = page.getByTestId('attendee-status-checked-in');
    const pendingBadge = page.getByTestId('attendee-status-pending');

    // Если участник ещё не отмечен, выполняем check-in
    if (await pendingBadge.isVisible()) {
      await page.getByTestId('check-in-confirm-button').click();

      // Ждём изменения статуса на "Отмечен"
      await expect(checkedInBadge).toBeVisible({ timeout: 10000 });
    }

    // В любом случае проверяем, что статус "Отмечен"
    await expect(checkedInBadge).toBeVisible();
    await expect(page.getByText(/участник уже отмечен/i)).toBeVisible();
  });

  test('can scan another code after check-in', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto(`/dashboard/events/${eventId}/check-in`);

    // Переключаемся на вкладку "Вручную"
    await page.getByRole('tab', { name: /вручную/i }).click();

    // Вводим код подтверждения
    await page.getByTestId('check-in-code-input').fill(confirmationCode);
    await page.getByTestId('check-in-search-button').click();

    // Ждём появления карточки участника
    await expect(page.getByTestId('attendee-card')).toBeVisible({ timeout: 10000 });

    // Нажимаем "Сканировать другой код"
    await page.getByRole('button', { name: /сканировать другой код/i }).click();

    // Проверяем, что вернулись к форме поиска
    await expect(page.getByTestId('check-in-search-form')).toBeVisible();
  });
});
